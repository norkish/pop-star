package melody;

import java.awt.Color;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import org.tc33.jheatchart.HeatChart;

import composition.Measure;
import composition.Score;
import constraint.Constraint;
import data.MusicXMLModel;
import data.MusicXMLModelLearner;
import data.MusicXMLParser.Harmony;
import data.MusicXMLParser.Key;
import data.MusicXMLParser.Note;
import data.MusicXMLParser.NoteLyric;
import data.MusicXMLParser.NoteTie;
import data.MusicXMLParser.Time;
import data.ParsedMusicXMLObject;
import globalstructure.SegmentType;
import inspiration.Inspiration;
import markov.SparseNHMM;
import markov.SparseSingleOrderMarkovModel;
import utils.Pair;
import utils.Triple;
import utils.Utils;

public class SegmentSpecificMelodyEngineer extends MelodyEngineer {

	private static boolean normalizeByHarmony = true;
	public static class SegmentSpecificMelodyEngineerMusicXMLModel extends MusicXMLModel {

		private static final int DEISRED_AVERAGE_OCTAVE = 4; // in MIDI and MusicXML 4 octave starts with middle C
		Map<SegmentType,SparseSingleOrderMarkovModel<Integer>> pitchMarkovModelsBySegment;
		Map<Integer, Integer> pitchStatesByIndex = new HashMap<Integer, Integer>();// states are pitches for now
		Map<SegmentType,Map<Integer, Integer>> pitchPriorCountsBySegment = new EnumMap<SegmentType,Map<Integer, Integer>>(SegmentType.class);
		Map<SegmentType,Map<Integer, Map<Integer, Integer>>> pitchTransitionCountsBySegment = new EnumMap<SegmentType, Map<Integer, Map<Integer, Integer>>>(SegmentType.class);

		// have a Markov model for each offset position with the markov property applying to the previous duration
		Map<Time, Map<SegmentType, SparseSingleOrderMarkovModel<Double>>> durationMarkovModelsByOffsetBySegmentByTime = new HashMap<Time, Map<SegmentType,SparseSingleOrderMarkovModel<Double>>>();
		Map<Time, Map<SegmentType, Map<Double, Integer>>> durationStatesByIndexBySegmentByTime = new HashMap<Time, Map<SegmentType, Map<Double, Integer>>>();// states are pitches for now
		Map<Time, Map<SegmentType, Map<Integer, Integer>>> durationPriorCountsBySegmentByTime = new HashMap<Time, Map<SegmentType, Map<Integer, Integer>>>();
		Map<Time, Map<SegmentType, Map<Integer, Map<Integer, Integer>>>> durationTransitionCountsBySegmentByTime = new HashMap<Time, Map<SegmentType, Map<Integer, Map<Integer, Integer>>>>();

		
		@Override
		/**
		 * Only trains on examples with harmony
		 */
		public void trainOnExample(ParsedMusicXMLObject musicXML) {
			// if doesn't have harmony, return
			List<Triple<Integer, Integer, Note>> notesByMeasure = musicXML.getNotesByPlayedMeasure();
			SortedMap<Integer, SortedMap<Integer, Harmony>> harmonyByMeasure = musicXML.getUnoverlappingHarmonyByPlayedMeasureAsMap();
			if (notesByMeasure.isEmpty() || harmonyByMeasure.isEmpty()) {
				return;
			}

			// the current markovModel is now obsolete and will be regenerated
			// when next sampled
			pitchMarkovModelsBySegment = null;

			Integer prevNotePitchIdx = -1;
			Harmony currHarmony = null;
			double prevNoteDurationInBeats = -1.0;

			SortedMap<Integer, SortedMap<Double, SegmentType>> globalStructure = musicXML.getGlobalStructureBySegmentTokenStart();
			int notesToAdvanceForTies; 
			int maxNotesToAdvanceForTies = 5;
			SegmentType prevType = null;
			for (int i = 0; i < notesByMeasure.size(); i += notesToAdvanceForTies) {
				notesToAdvanceForTies = 1;
				Triple<Integer, Integer, Note> measureOffsetNote = notesByMeasure.get(i);
				Note note = measureOffsetNote.getThird();
				int measure = measureOffsetNote.getFirst();
				int divsOffset = measureOffsetNote.getSecond();
				double beatsOffset = musicXML.divsToBeats(divsOffset, measure);
				currHarmony = Utils.valueForKeyBeforeOrEqualTo(measure, divsOffset, harmonyByMeasure);
				if (note == null || note.isChordWithPrevious)
					continue;

				Time currTime = musicXML.getTimeForMeasure(measure);
				SegmentType currType = Utils.valueForKeyBeforeOrEqualTo(measure, beatsOffset, globalStructure);
				if (currType != prevType) {
					prevNoteDurationInBeats = -1.0;
				}
				
				durationMarkovModelsByOffsetBySegmentByTime.remove(currTime);
//				double beatsOffset = musicXML.divsToBeats(divsOffset, measure);

				double noteDurationInBeats = musicXML.divsToBeats(note.duration, measure);
				
				// if the note is tied (or "slurred to same pitch")  
				if (note.tie == NoteTie.START || note.slur == NoteTie.START) {
					Note currNote = note;
					List<Double> noteDurationInBeatsToTie = new ArrayList<Double>();
					for (int j = 1; j <= maxNotesToAdvanceForTies && i+j < notesByMeasure.size(); j++) {
						Triple<Integer, Integer, Note> currNoteMeasureOffsetNote = notesByMeasure.get(i+j);
						currNote = currNoteMeasureOffsetNote.getThird();
						int currNoteMeasure = currNoteMeasureOffsetNote.getFirst();
						int currNoteDivsOffset = currNoteMeasureOffsetNote.getSecond();
						double currNoteBeatsOffset = musicXML.divsToBeats(currNoteDivsOffset, currNoteMeasure);
						SegmentType currTiedNoteType = Utils.valueForKeyBeforeOrEqualTo(currNoteMeasure, currNoteBeatsOffset, globalStructure);

						NoteLyric lyric = currNote.getLyric(currTiedNoteType.mustHaveDifferentLyricsOnRepeats());
						if (currNote.isChordWithPrevious || currNote.pitch != note.pitch || (lyric != null && !lyric.text.isEmpty())) {
							break;
						}
						
						noteDurationInBeatsToTie.add(musicXML.divsToBeats(currNote.duration, currNoteMeasure));
						
						if (note.tie == NoteTie.START && currNote.tie == NoteTie.STOP || note.slur == NoteTie.START && currNote.slur == NoteTie.STOP) {
							break;
						}
					}
					
					for (Double duration : noteDurationInBeatsToTie) {
						noteDurationInBeats += duration;
						notesToAdvanceForTies++;
					}
				}

				int notePitchToken;
				if (normalizeByHarmony) {
					if (note.pitch == Note.REST) {
						notePitchToken = note.pitch;
					} else {
						int octave = note.pitch / 12; // loss of precision to get octave
						octave += DEISRED_AVERAGE_OCTAVE-musicXML.getAverageOctave();
						notePitchToken = currHarmony.getScaleStep(note.pitch) + octave * 12;
					}
				} else { 
					notePitchToken = note.pitch;
				}
				
				Integer notePitchIdx = pitchStatesByIndex.get(notePitchToken);
				if (notePitchIdx == null) {
					notePitchIdx = pitchStatesByIndex.size();
					pitchStatesByIndex.put(notePitchToken, notePitchIdx);
				}

				Map<SegmentType, Map<Double, Integer>> durationStatesByIndexBySegment = durationStatesByIndexBySegmentByTime.get(currTime);
				if (durationStatesByIndexBySegment == null) {
					durationStatesByIndexBySegment = new EnumMap<SegmentType, Map<Double, Integer>>(SegmentType.class);
					durationStatesByIndexBySegmentByTime.put(currTime, durationStatesByIndexBySegment);
				}
				
				Map<Double, Integer> durationStatesByIndex = durationStatesByIndexBySegment.get(currType);
				if (durationStatesByIndex == null) {
					durationStatesByIndex = new HashMap<Double, Integer>();
					durationStatesByIndexBySegment.put(currType, durationStatesByIndex);
				}
				
				Integer noteDurationIdx = durationStatesByIndex.get(noteDurationInBeats);
				if (noteDurationIdx == null) {
					noteDurationIdx = durationStatesByIndex.size();
					durationStatesByIndex.put(noteDurationInBeats, noteDurationIdx);
				}

				
				
				if (prevNotePitchIdx == -1 || currType != prevType) {
					Map<Integer, Integer> pitchPriorCounts = pitchPriorCountsBySegment.get(currType);
					if (pitchPriorCounts == null) {
						pitchPriorCounts = new HashMap<Integer,Integer>();
						pitchPriorCountsBySegment.put(currType, pitchPriorCounts);
					}
					
					Utils.incrementValueForKey(pitchPriorCounts, notePitchIdx);
				} 
				
				if (prevNotePitchIdx != -1){
					Map<Integer, Map<Integer, Integer>> pitchTransitionCounts = pitchTransitionCountsBySegment.get(currType);
					if (pitchTransitionCounts == null) {
						pitchTransitionCounts = new HashMap<Integer,Map<Integer,Integer>>();
						pitchTransitionCountsBySegment.put(currType, pitchTransitionCounts);
					}
					Utils.incrementValueForKeys(pitchTransitionCounts, prevNotePitchIdx, notePitchIdx);
				}


				if (prevNoteDurationInBeats == -1.0) {
					Map<SegmentType, Map<Integer, Integer>> durationPriorCountsBySegment = durationPriorCountsBySegmentByTime.get(currTime);
					if (durationPriorCountsBySegment == null) {
						durationPriorCountsBySegment = new EnumMap<SegmentType, Map<Integer, Integer>>(SegmentType.class);
						durationPriorCountsBySegmentByTime.put(currTime, durationPriorCountsBySegment);
					}

					Map<Integer, Integer> durationPriorCounts = durationPriorCountsBySegment.get(currType);
					if (durationPriorCounts == null) {
						durationPriorCounts = new HashMap<Integer, Integer>();
						durationPriorCountsBySegment.put(currType, durationPriorCounts);
					}
					
					Utils.incrementValueForKey(durationPriorCounts, noteDurationIdx);
				} else {
					Map<SegmentType, Map<Integer, Map<Integer, Integer>>> durationTransitionCountsBySegment = durationTransitionCountsBySegmentByTime.get(currTime);
					if (durationTransitionCountsBySegment == null) {
						durationTransitionCountsBySegment = new EnumMap<SegmentType, Map<Integer, Map<Integer, Integer>>>(SegmentType.class);
						durationTransitionCountsBySegmentByTime.put(currTime, durationTransitionCountsBySegment);
					}

					Map<Integer, Map<Integer, Integer>> durationTransitionCounts = durationTransitionCountsBySegment.get(currType);
					if (durationTransitionCounts == null) {
						durationTransitionCounts = new HashMap<Integer, Map<Integer, Integer>>();
						durationTransitionCountsBySegment.put(currType, durationTransitionCounts);
					}
					
					Integer prevNoteDurationIdx = durationStatesByIndex.get(prevNoteDurationInBeats);
					if (prevNoteDurationIdx == null) {
						prevNoteDurationIdx = durationStatesByIndex.size();
						durationStatesByIndex.put(prevNoteDurationInBeats, prevNoteDurationIdx);
					}
					Utils.incrementValueForKeys(durationTransitionCounts, prevNoteDurationIdx, noteDurationIdx);
				}
				
				prevNotePitchIdx = notePitchIdx;
				prevNoteDurationInBeats = noteDurationInBeats;
				prevType = currType;
			}

		}

		public String toString() {
			StringBuilder str = new StringBuilder();

			// string representation of model

			return str.toString();
		}

		/**
		 * 
		 * @param length
		 * @param type
		 * @param contextualConstraints
		 * @return list of pitch,duration (in beats) pairs
		 */
		public List<Pair<Integer,Double>> sampleMelodySequenceOfLength(int length, SegmentType type,
				List<Constraint<Note>> contextualConstraints, Time sequenceTime) {
			// TODO Add constraints according to type, including constraints
			// which depend on the harm sequence

			// generate pitch
			if (pitchMarkovModelsBySegment == null) {
				pitchMarkovModelsBySegment = buildPitchModels();
			}
			
			final SparseSingleOrderMarkovModel<Integer> pitchMarkovModel = pitchMarkovModelsBySegment.get(type);
			List<Integer> pitchList = new ArrayList<Integer>();
			
			Integer lastPitch = pitchMarkovModel.sampleStartState();
			Integer nextPitch;
			pitchList.add(lastPitch);
			
			//sample while not done
			while(pitchList.size() < length) {
				try {
					nextPitch = pitchMarkovModel.sampleNextState(lastPitch);
				} catch (Exception ex) {
					nextPitch = sampleNextPitchForAnySegment(lastPitch);
				}
				
				pitchList.add(lastPitch);
				
				lastPitch = nextPitch;
			}
			
			// generate durations
			Map<SegmentType, SparseSingleOrderMarkovModel<Double>> durationMarkovModelsByOffsetBySegment = durationMarkovModelsByOffsetBySegmentByTime.get(sequenceTime);
			SparseSingleOrderMarkovModel<Double> durationMarkovModel;
			if (durationMarkovModelsByOffsetBySegment == null) {
				durationMarkovModelsByOffsetBySegment = new EnumMap<SegmentType, SparseSingleOrderMarkovModel<Double>>(SegmentType.class);
				durationMarkovModelsByOffsetBySegmentByTime.put(sequenceTime, durationMarkovModelsByOffsetBySegment);
			} 
			
			durationMarkovModel = durationMarkovModelsByOffsetBySegment.get(type);
			if (durationMarkovModel == null) {
				durationMarkovModel = buildDurationModels(sequenceTime, type);
				durationMarkovModelsByOffsetBySegmentByTime.get(sequenceTime).put(type, durationMarkovModel);
			}
			
			SparseNHMM<Double> constrainedDurationModel = new SparseNHMM<Double>(durationMarkovModel, length,
					new ArrayList<Pair<Integer,Constraint<Double>>>());
			List<Double> durationsAsBeats = constrainedDurationModel.generate(length);
			List<Pair<Integer,Double>> pitchDurationPairs = new ArrayList<Pair<Integer,Double>>();
			
			// combine pitch and durations
			Double lastDuration = null,nextDuration;
			for (int i = 0; i < pitchList.size(); i++) {
				nextPitch = pitchList.get(i);
				nextDuration = durationsAsBeats.get(i);
				
				if (Note.REST == nextPitch && Note.REST == lastPitch) {
					//combine multiple consecutive rests
					lastDuration += nextDuration;
					pitchDurationPairs.get(pitchDurationPairs.size()-1).setSecond(lastDuration);
				} else {
					pitchDurationPairs.add(new Pair<Integer,Double>(nextPitch, nextDuration));
					lastPitch = nextPitch;
					lastDuration = nextDuration;
				}
			}
			
			return pitchDurationPairs;
		}

		private Integer sampleNextPitchForAnySegment(Integer prevPitch) {
			
			for (SegmentType type : pitchMarkovModelsBySegment.keySet()) {
				SparseSingleOrderMarkovModel<Integer> markovModel = pitchMarkovModelsBySegment.get(type);
				Integer nextPitch;
				try {
					nextPitch = markovModel.sampleNextState(prevPitch);
					return nextPitch;
				} catch (ArrayIndexOutOfBoundsException ex) {
				}
			}
			throw new RuntimeException("No next state for any offset from " + prevPitch);
		}

		private SparseSingleOrderMarkovModel<Double> buildDurationModels(Time sequenceTime, SegmentType type) {
			Map<Double, SparseSingleOrderMarkovModel<Double>> durationMarkovModelsByOffset = new HashMap<Double, SparseSingleOrderMarkovModel<Double>>();
			
			Map<SegmentType, Map<Double, Integer>> durationStatesByIndexBySegment = durationStatesByIndexBySegmentByTime.get(sequenceTime);
			if (durationStatesByIndexBySegment == null) {
				throw new RuntimeException("Model was not trained on any XMLs with time signature " + sequenceTime);
			}
			Map<Double, Integer> durationStatesByIndex = durationStatesByIndexBySegment.get(type);
			if (durationStatesByIndex == null) {
				throw new RuntimeException("Model was not trained on any XMLs with time signature " + sequenceTime + " and Segment Type " + type);
			}
			
			Map<Integer, Double> priors = computeDurationPriors(sequenceTime, type);

			Map<Integer, Map<Integer, Double>> transitions = computeDurationTransitionProbabilities(sequenceTime, type);
			
			SparseSingleOrderMarkovModel<Double> durationMarkovModel = new SparseSingleOrderMarkovModel<Double>(durationStatesByIndex, priors, transitions);
			
			return durationMarkovModel;
		}

		private Map<Integer, Map<Integer, Double>> computeDurationTransitionProbabilities(Time sequenceTime,
				SegmentType type) {
			Map<Integer, Map<Integer, Integer>> durationTransitionCounts = durationTransitionCountsBySegmentByTime.get(sequenceTime).get(type);
			return computeTransitionProbabilities(durationTransitionCounts);
		}

		private Map<Integer, Double> computeDurationPriors(Time sequenceTime, SegmentType type) {
			Map<Integer, Integer> durationPriorCounts = durationPriorCountsBySegmentByTime.get(sequenceTime).get(type);
			return computePriors(durationPriorCounts);
		}

		private Map<SegmentType, SparseSingleOrderMarkovModel<Integer>> buildPitchModels() {
			Map<SegmentType, SparseSingleOrderMarkovModel<Integer>> models = new EnumMap<SegmentType, SparseSingleOrderMarkovModel<Integer>>(SegmentType.class);
			
			for (SegmentType type : pitchTransitionCountsBySegment.keySet()) {
				Map<Integer, Double> priors = computePriors(pitchPriorCountsBySegment.get(type));
				Map<Integer, Map<Integer, Double>> transitions = computeTransitionProbabilities(pitchTransitionCountsBySegment.get(type));
				models.put(type,new SparseSingleOrderMarkovModel<Integer>(pitchStatesByIndex, priors, transitions));
			}

			return models;
		}


		@Override
		public void toGraph() {
			durationModelToGraph();
			pitchModelToGraph();
		}

		private void durationModelToGraph() {
			Map<Integer, Double> priors = computeDurationPriors(Time.FOUR_FOUR, SegmentType.VERSE);
			Map<Integer, Map<Integer, Double>> transitions = computeDurationTransitionProbabilities(Time.FOUR_FOUR, SegmentType.VERSE);
			int maxXValues = 50;
			int maxYValues = 10;
			
			Map<Double, Integer> statesByIndex = durationStatesByIndexBySegmentByTime.get(Time.FOUR_FOUR).get(SegmentType.VERSE);
			Map<Integer, Map<Integer, Integer>> transitionCounts = durationTransitionCountsBySegmentByTime.get(Time.FOUR_FOUR).get(SegmentType.VERSE);
			
			boolean sortByFrequency = true;
			//y-axis is from
			Map<Double, Integer> statesByIndexSorted = null;
			if (sortByFrequency) {
				statesByIndexSorted = new LinkedHashMap<Double,Integer>();
				Map<Integer, Integer> frequencyByID = new HashMap<Integer,Integer>();
				//initialize frequencies
				for (Integer harmonyID : statesByIndex.values()) {
					frequencyByID.put(harmonyID, 0);
				}
				
				// count frequencies
				for (Map<Integer, Integer> toMap : transitionCounts.values()) {
					for (Integer durationID : toMap.keySet()) {
						Integer count = toMap.get(durationID);
						frequencyByID.put(durationID, frequencyByID.get(durationID)+count);
					}
				}
				
				// sort by counts (and then by ID)
				List<Map.Entry<Integer, Integer>> harmonyIDSortedByFrequency = new ArrayList<Map.Entry<Integer, Integer>>(frequencyByID.entrySet());
				Collections.sort(harmonyIDSortedByFrequency, new ValueThenKeyComparator<Integer, Integer>());
				
				// get reverse map to create new Harmony-ID map that is sorted by frequency
				Double[] durationByID = new Double[statesByIndex.size()];
				for (Double duration : statesByIndex.keySet()) {
					Integer harmonyID = statesByIndex.get(duration);
					durationByID[harmonyID] = duration;
				}
				
				for (Map.Entry<Integer, Integer> entry : harmonyIDSortedByFrequency) {
					statesByIndexSorted.put(durationByID[entry.getKey()], entry.getKey());
				}
				
			} else {
				statesByIndexSorted = statesByIndex;
			}
			
			
			// +1 for priors
			int chartXDimension = Math.min(maxXValues, statesByIndexSorted.size()); 
			int chartYDimension = Math.min(maxYValues, statesByIndexSorted.size()); 

			String[] xValues = new String[chartXDimension];
			String[] yValues = new String[chartYDimension + 1];
			double[][] chartValues = new double[chartYDimension + 1][chartXDimension];
			
			// set axis labels
			yValues[0] = "START";
			
			int i = 0;
			for (Double duration : statesByIndexSorted.keySet()) {
				Integer harmonyId = statesByIndexSorted.get(duration);
				if (i >= chartXDimension && i >= chartYDimension)
					break;
				
				if (i < chartXDimension) {
					xValues[i] = duration.toString();
					Double prior = priors.get(harmonyId);
					chartValues[0][i] = prior == null ? 0.0 : prior;
				}
				if (i < chartYDimension) {
					yValues[i+1] = duration.toString();
				}
				i++;
			}
			
			// populate heatchart
			
			int y = 0;
			for (Double yDuration : statesByIndexSorted.keySet()) {
				if (y >= chartYDimension) {
					break;
				}
				Integer yDurationId = statesByIndexSorted.get(yDuration);
				int x = 0;
				for (Double xDuration : statesByIndexSorted.keySet()) {
					if (x >= chartXDimension) {
						break;
					}
					Integer xHarmonyId = statesByIndexSorted.get(xDuration);
					Double prob = null; 
					Map<Integer, Double> probsFromHarmony = transitions.get(yDurationId);
					if (probsFromHarmony != null) {
						prob = probsFromHarmony.get(xHarmonyId);
					}
					chartValues[y+1][x] = prob == null ? 0.0 : prob;
					x++;
				}
				y++;
			}
			
			Utils.normalizeByFirstDimension(chartValues);

			HeatChart chart = new HeatChart(chartValues);
			chart.setHighValueColour(Color.RED);
			chart.setLowValueColour(Color.BLUE);
			
			chart.setYAxisLabel("Previous Note Duration (beats)");
			chart.setXAxisLabel("Next Note Duration (beats)");
			chart.setXValues(xValues);
			chart.setYValues(yValues);
			chart.setAxisLabelsFont(MusicXMLModel.CHART_LABEL_FONT);
			chart.setAxisValuesFont(MusicXMLModel.CHART_AXIS_FONT);
			chart.setCellSize(new Dimension(30,30));
			
			try {
				chart.saveToFile(new File(GRAPH_DIR + "/melody_rhythm.jpeg"));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		private void pitchModelToGraph() {
			// TODO Auto-generated method stub
			
		}
	}

	private SegmentSpecificMelodyEngineerMusicXMLModel model;

	public SegmentSpecificMelodyEngineer() {
		this.model = (SegmentSpecificMelodyEngineerMusicXMLModel) MusicXMLModelLearner
				.getTrainedModel(this.getClass());
	}

	@Override
	public void addMelody(Inspiration inspiration, Score score) {
		List<Pair<SegmentType, Integer>> segmentsLengths = score.getSegmentsLengths();
		Map<SegmentType,Time> timeBySegment = score.getSegmentTimeSignatures();

		for (SegmentType type : new SegmentType[] { SegmentType.CHORUS, SegmentType.VERSE, SegmentType.BRIDGE,
				SegmentType.INTRO, SegmentType.OUTRO, SegmentType.INTERLUDE }) {
			if (!timeBySegment.containsKey(type)) continue;
			
			int length = getMinLengthForSegmentType(segmentsLengths, type);
			// TODO: may want to generate new sequences for every repetition if
			// length varies, probably easiest that way rather than adapting
			// existing seq
			if (length != Integer.MAX_VALUE) {
				length = 200;
				List<Pair<Integer, Double>> melodySeq = model.sampleMelodySequenceOfLength(length, type, null, timeBySegment.get(type));
				addMelodyBySegmentType(score, melodySeq, type);
			}
		}

	}

	private void addMelodyBySegmentType(Score score, List<Pair<Integer, Double>> melodySeq, SegmentType type) {
		List<Integer> melodySeqDurations = new ArrayList<Integer>();
		SegmentType prevType = null;
		int counter = -1;
		List<Measure> measures = score.getMeasures();
		
		double durationInBeatsToCarryOverToNextMeasure = 0.;
		int prevPitch = -1;
		
		for (int i = 0; i < measures.size(); i++) {
			Measure measure = measures.get(i);
			
			if (measure.segmentType != prevType) {
				if (measure.segmentType == type) {
					counter = 0;
				}
				prevType = measure.segmentType;
			}

			if (measure.segmentType == type) {
				int divisionsPerQuarterNote = measure.divisionsPerQuarterNote;
				Time currTime = measure.time;
				Key currKey = measure.key;
				int accumulativeDivisions = 0;
				int divisionsToAdd;
				final int totalMeasureDivisions = (int) (currTime.beats * divisionsPerQuarterNote * (4.0/currTime.beatType));
	
				// notes that carry over
				if (durationInBeatsToCarryOverToNextMeasure > 0.) {
					divisionsToAdd = (int) (durationInBeatsToCarryOverToNextMeasure * divisionsPerQuarterNote * (4.0/currTime.beatType));
					durationInBeatsToCarryOverToNextMeasure -= totalMeasureDivisions;
					List<Note> notesToAdd = createTiedNoteWithDuration(Math.min(totalMeasureDivisions,divisionsToAdd), prevPitch, divisionsPerQuarterNote);
					if (durationInBeatsToCarryOverToNextMeasure <= 0.) {
						notesToAdd.get(notesToAdd.size()-1).tie = NoteTie.STOP;
						if (notesToAdd.size() > 1) notesToAdd.get(0).tie = NoteTie.NONE;
					}
					for (Note note : notesToAdd) {
						measure.addNote(((double)accumulativeDivisions)/divisionsPerQuarterNote, note);
						accumulativeDivisions += note.duration;
					}
				}

				// notes that need generating
				while (accumulativeDivisions < totalMeasureDivisions) {
					Pair<Integer,Double> pitchDuration = melodySeq.get(counter);
					Harmony currHarmony = score.getHarmonyPlayingAt(i, ((double)accumulativeDivisions)/divisionsPerQuarterNote);
					int pitchToken = pitchDuration.getFirst();
					int pitch;
					if (normalizeByHarmony) {
						if (pitchToken == Note.REST) {
							pitch = pitchToken;
						} else {
							int octave = pitchToken / 12;
							int harmonySpecificInterval = currHarmony.getIntervalForScaleStep(pitchToken%12);
							pitch = octave*12 + harmonySpecificInterval;
						}
					} else {
						pitch = pitchToken;
					}
					
					if (counter < melodySeqDurations.size()) {
						// durations should be the same for repeats of the same segment type
						divisionsToAdd = melodySeqDurations.get(counter);
					} else {
						double durationInBeats = pitchDuration.getSecond();
						divisionsToAdd = (int) (durationInBeats * divisionsPerQuarterNote * (4.0/currTime.beatType));
					 	melodySeqDurations.add(divisionsToAdd);
					}
					int divisionsToCarryOverToNextMeasure = (accumulativeDivisions + divisionsToAdd) - totalMeasureDivisions;
					
					if (divisionsToCarryOverToNextMeasure > 0) {
						if (i == measures.size()-1 || measures.get(i+1).segmentType != measure.segmentType) {// can't carry over if last measure or if carrying over into different segment
							durationInBeatsToCarryOverToNextMeasure = 0.0;
						} else {
							durationInBeatsToCarryOverToNextMeasure = (divisionsToCarryOverToNextMeasure / (double) divisionsPerQuarterNote) * (currTime.beatType/4.0);
						}
						divisionsToAdd = totalMeasureDivisions - accumulativeDivisions;
					}
					
					List<Note> notesToAdd = createTiedNoteWithDuration(divisionsToAdd, pitch, divisionsPerQuarterNote);
					if (durationInBeatsToCarryOverToNextMeasure > 0.) {
						notesToAdd.get(0).tie = NoteTie.START;
						if (notesToAdd.size() > 1) notesToAdd.get(notesToAdd.size()-1).tie = NoteTie.NONE;
					}
					
					for (Note note : notesToAdd) {
						measure.addNote(((double)accumulativeDivisions)/divisionsPerQuarterNote, note);
						accumulativeDivisions += note.duration;
					}
					counter++;
					prevPitch = pitch;
				}
			}
		}
	}

	private int getMinLengthForSegmentType(List<Pair<SegmentType, Integer>> segmentsLengths, SegmentType type) {
		int length = Integer.MAX_VALUE;
		for (Pair<SegmentType, Integer> pair : segmentsLengths) {
			Integer segmentLength = pair.getSecond();
			if (pair.getFirst() == type && segmentLength < length) {
				length = segmentLength;
			}
		}
		return length;
	}

}
