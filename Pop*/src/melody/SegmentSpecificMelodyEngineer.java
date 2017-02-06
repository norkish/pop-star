package melody;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import composition.Measure;
import composition.Score;
import constraint.Constraint;
import data.MusicXMLModel;
import data.MusicXMLModelLearner;
import data.MusicXMLParser;
import data.MusicXMLSummaryGenerator;
import data.MusicXMLParser.Harmony;
import data.MusicXMLParser.Key;
import data.MusicXMLParser.Note;
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

		SparseSingleOrderMarkovModel<Integer> pitchMarkovModel;
		Map<Integer, Integer> pitchStatesByIndex = new HashMap<Integer, Integer>();// states are pitches for now
		Map<Integer, Integer> pitchPriorCounts = new HashMap<Integer, Integer>();
		Map<Integer, Map<Integer, Integer>> pitchTransitionCounts = new HashMap<Integer, Map<Integer, Integer>>();

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
			pitchMarkovModel = null;

			Integer prevNotePitchIdx = -1;
			Harmony currHarmony = null;
			double prevNoteDurationInBeats = -1.0;

			SortedMap<Integer, SegmentType> globalStructure = musicXML.globalStructure;
			int notesToAdvanceForTies; 
			int maxNotesToAdvanceForTies = 5;
			SegmentType prevType = null;
			for (int i = 0; i < notesByMeasure.size(); i += notesToAdvanceForTies) {
				notesToAdvanceForTies = 1;
				Triple<Integer, Integer, Note> measureOffsetNote = notesByMeasure.get(i);
				Note note = measureOffsetNote.getThird();
				int measure = measureOffsetNote.getFirst();
				int divsOffset = measureOffsetNote.getSecond();
				currHarmony = Utils.valueForKeyBeforeOrEqualTo(measure, divsOffset, harmonyByMeasure);
				if (note == null || note.isChordWithPrevious)
					continue;

				Time currTime = musicXML.getTimeForMeasure(measure);
				SegmentType currType = Utils.valueForKeyBeforeOrEqualTo(measure, globalStructure);
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
					for (int j = 1; j <= maxNotesToAdvanceForTies; j++) {
						Triple<Integer, Integer, Note> currNoteMeasureOffsetNote = notesByMeasure.get(i+j);
						currNote = currNoteMeasureOffsetNote.getThird();
						int currNoteMeasure = currNoteMeasureOffsetNote.getFirst();

						if (currNote.isChordWithPrevious || currNote.pitch != note.pitch || (currNote.lyric != null && !currNote.lyric.text.isEmpty())) {
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

				if (prevNotePitchIdx == -1) {
					Utils.incrementValueForKey(pitchPriorCounts, notePitchIdx);
				} else {
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
			if (pitchMarkovModel == null) {
				pitchMarkovModel = buildPitchModel();
			}
			
			SparseNHMM<Integer> constrainedPitchModel = new SparseNHMM<Integer>(pitchMarkovModel, length,
					new ArrayList<Constraint<Integer>>());
			
			List<Integer> pitchList = constrainedPitchModel.generate(length);
			
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
					new ArrayList<Constraint<Double>>());
			List<Double> durationsAsBeats = constrainedDurationModel.generate(length);
			List<Pair<Integer,Double>> pitchDurationPairs = new ArrayList<Pair<Integer,Double>>();
			
			// combine pitch and durations
			for (int i = 0; i < pitchList.size(); i++) {
				pitchDurationPairs.add(new Pair<Integer,Double>(pitchList.get(i), durationsAsBeats.get(i)));
			}
			
			return pitchDurationPairs;
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
			
			Map<Integer, Integer> durationPriorCounts = durationPriorCountsBySegmentByTime.get(sequenceTime).get(type);
			Map<Integer, Map<Integer, Integer>> durationTransitionCounts = durationTransitionCountsBySegmentByTime.get(sequenceTime).get(type);
			
			Map<Integer, Double> priors = new HashMap<Integer, Double>();
			Map<Integer, Map<Integer, Double>> transitions = new HashMap<Integer, Map<Integer, Double>>();

			double totalCount = 0;
			if (durationPriorCounts != null) {
				for (Integer count : durationPriorCounts.values()) {
					totalCount += count;
				}
				for (Entry<Integer, Integer> entry : durationPriorCounts.entrySet()) {
					priors.put(entry.getKey(), entry.getValue() / totalCount);
				}
			}

			for (Entry<Integer, Map<Integer, Integer>> outerEntry : durationTransitionCounts.entrySet()) {
				Integer fromIdx = outerEntry.getKey();
				Map<Integer, Integer> innerMap = outerEntry.getValue();

				Map<Integer, Double> newInnerMap = new HashMap<Integer, Double>();
				transitions.put(fromIdx, newInnerMap);

				totalCount = 0;
				for (Integer count : innerMap.values()) {
					totalCount += count;
				}
				for (Entry<Integer, Integer> entry : innerMap.entrySet()) {
					newInnerMap.put(entry.getKey(), entry.getValue() / totalCount);
				}
			}
			
			SparseSingleOrderMarkovModel<Double> durationMarkovModel = new SparseSingleOrderMarkovModel<Double>(durationStatesByIndex, priors, transitions);
			
			return durationMarkovModel;
		}

		private SparseSingleOrderMarkovModel<Integer> buildPitchModel() {
			Map<Integer, Double> priors = new HashMap<Integer, Double>();
			Map<Integer, Map<Integer, Double>> transitions = new HashMap<Integer, Map<Integer, Double>>();

			double totalCount = 0;
			for (Integer count : pitchPriorCounts.values()) {
				totalCount += count;
			}
			for (Entry<Integer, Integer> entry : pitchPriorCounts.entrySet()) {
				priors.put(entry.getKey(), entry.getValue() / totalCount);
			}

			for (Entry<Integer, Map<Integer, Integer>> outerEntry : pitchTransitionCounts.entrySet()) {
				Integer fromIdx = outerEntry.getKey();
				Map<Integer, Integer> innerMap = outerEntry.getValue();

				Map<Integer, Double> newInnerMap = new HashMap<Integer, Double>();
				transitions.put(fromIdx, newInnerMap);

				totalCount = 0;
				for (Integer count : innerMap.values()) {
					totalCount += count;
				}
				for (Entry<Integer, Integer> entry : innerMap.entrySet()) {
					newInnerMap.put(entry.getKey(), entry.getValue() / totalCount);
				}
			}

			return new SparseSingleOrderMarkovModel<Integer>(pitchStatesByIndex, priors, transitions);
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
