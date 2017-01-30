package melody;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;

import composition.Measure;
import composition.Score;
import constraint.Constraint;
import data.MusicXMLModel;
import data.MusicXMLModelLearner;
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

	public static class SegmentSpecificMelodyEngineerMusicXMLModel extends MusicXMLModel {

		SparseSingleOrderMarkovModel<Integer> pitchMarkovModel;
		Map<Integer, Integer> pitchStatesByIndex = new HashMap<Integer, Integer>();// states are pitches for now
		Map<Integer, Integer> pitchPriorCounts = new HashMap<Integer, Integer>();
		Map<Integer, Map<Integer, Integer>> pitchTransitionCounts = new HashMap<Integer, Map<Integer, Integer>>();

		// have a markov model for each offset position with the markov property applying to the previous duration
		Map<Time, Map<Double, SparseSingleOrderMarkovModel<Double>>> durationMarkovModelsByOffsetByTime = new HashMap<Time, Map<Double, SparseSingleOrderMarkovModel<Double>>>();
		Map<Time, TreeMap<Double, Map<Double, Integer>>> durationStatesByIndexByOffsetByTime = new HashMap<Time, TreeMap<Double,Map<Double, Integer>>>();// states are pitches for now
		Map<Time, TreeMap<Double, Map<Integer, Integer>>> durationPriorCountsByOffsetByTime = new HashMap<Time, TreeMap<Double, Map<Integer, Integer>>>();
		Map<Time, TreeMap<Double, Map<Integer, Map<Integer, Integer>>>> durationTransitionCountsByOffsetByTime = new HashMap<Time, TreeMap<Double, Map<Integer, Map<Integer, Integer>>>>();

		@Override
		/**
		 * Only trains on examples with harmony
		 */
		public void trainOnExample(ParsedMusicXMLObject musicXML) {
			// if doesn't have harmony, return
			if (musicXML.noteCount == 0) {
				return;
			}

			// the current markovModel is now obselete and will be regenerated
			// when next sampled
			pitchMarkovModel = null;

			Integer prevNotePitchIdx = -1;
			double prevNoteDurationInBeats = -1.0;

			// TODO: condition on duration, condition on segment type, condition on chord
			SortedMap<Integer, Time> timeMap = musicXML.timeByAbsoluteMeasure;
			
			SortedMap<Integer, Integer> divsPerQuarterByMeasure = musicXML.divsPerQuarterByAbsoluteMeasure;
			List<Triple<Integer, Integer, Note>> notesByMeasure = musicXML.notesByPlayedMeasure;
			int notesToAdvanceForTies; 
			int maxNotesToAdvanceForTies = 5;
			for (int i = 0; i < notesByMeasure.size(); i += notesToAdvanceForTies) {
				notesToAdvanceForTies = 1;
				Triple<Integer, Integer, Note> measureOffsetNote = notesByMeasure.get(i);
				Note note = measureOffsetNote.getThird();
				if (note == null || note.isChordWithPrevious)
					continue;

				int measure = measureOffsetNote.getFirst();
				int divsOffset = measureOffsetNote.getSecond();
				Time currTime = Utils.valueForKeyBeforeOrEqualTo(measure, timeMap);
				
				// We'll count anything in 2/2 as 4/4
				if (currTime.equals(Time.TWO_TWO)) {
					currTime = Time.FOUR_FOUR;
				}
				
				durationMarkovModelsByOffsetByTime.remove(currTime);
				double divsPerQuarter = (double) Utils.valueForKeyBeforeOrEqualTo(measure, divsPerQuarterByMeasure);
				double beatsOffset = (divsOffset/divsPerQuarter) * (currTime.beatType/4.0);

				double noteDurationInBeats = (note.duration/divsPerQuarter) * (currTime.beatType/4.0);
				
				// if the note is tied (or "slurred to same pitch")  
				if (note.tie == NoteTie.START || note.slur == NoteTie.START) {
					Note currNote = note;
					Time currNoteTime = null;
					double currNoteDivsPerQuarter = -1;
					List<Double> noteDurationInBeatsToTie = new ArrayList<Double>();
					for (int j = 1; j <= maxNotesToAdvanceForTies; j++) {
						Triple<Integer, Integer, Note> currNoteMeasureOffsetNote = notesByMeasure.get(i+j);
						currNote = currNoteMeasureOffsetNote.getThird();
						int currNoteMeasure = currNoteMeasureOffsetNote.getFirst();
						currNoteTime = Utils.valueForKeyBeforeOrEqualTo(currNoteMeasure, timeMap);
						if (currNoteTime.equals(Time.TWO_TWO)) {
							currNoteTime = Time.FOUR_FOUR;
						}
						currNoteDivsPerQuarter = (double) Utils.valueForKeyBeforeOrEqualTo(currNoteMeasure, divsPerQuarterByMeasure);

						if (currNote.isChordWithPrevious || currNote.pitch != note.pitch || (currNote.lyric != null && !currNote.lyric.text.isEmpty())) {
							break;
						}
						
						noteDurationInBeatsToTie.add((currNote.duration/currNoteDivsPerQuarter) * (currNoteTime.beatType/4.0));
						
						if (note.tie == NoteTie.START && currNote.tie == NoteTie.STOP || note.slur == NoteTie.START && currNote.slur == NoteTie.STOP) {
							break;
						}
					}
					
					// if we found the end of the tie, add all the notes
//					if (note.tie == NoteTie.START && currNote.tie == NoteTie.STOP || note.slur == NoteTie.START && currNote.slur == NoteTie.STOP) {
						for (Double duration : noteDurationInBeatsToTie) {
							noteDurationInBeats += duration;
							notesToAdvanceForTies++;
						}
//					} else { // otherwise, just add one note
//						if (!noteDurationInBeatsToTie.isEmpty()) {
//							noteDurationInBeats += noteDurationInBeatsToTie.get(0);
//							notesToAdvanceForTies++;
//						}
//					}
				}

				Integer notePitchIdx = pitchStatesByIndex.get(note.pitch);
				if (notePitchIdx == null) {
					notePitchIdx = pitchStatesByIndex.size();
					pitchStatesByIndex.put(note.pitch, notePitchIdx);
				}

				TreeMap<Double, Map<Double, Integer>> durationStatesByIndexByOffset = durationStatesByIndexByOffsetByTime.get(currTime);
				if (durationStatesByIndexByOffset == null) {
					durationStatesByIndexByOffset = new TreeMap<Double, Map<Double, Integer>>();
					durationStatesByIndexByOffsetByTime.put(currTime, durationStatesByIndexByOffset);
				}
				
				Map<Double, Integer> durationStatesByIndex = durationStatesByIndexByOffset.get(beatsOffset);
				if (durationStatesByIndex == null) {
					durationStatesByIndex = new HashMap<Double, Integer>();
					durationStatesByIndexByOffset.put(beatsOffset, durationStatesByIndex);
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
					TreeMap<Double, Map<Integer, Integer>> durationPriorCountsByOffset = durationPriorCountsByOffsetByTime.get(currTime);
					if (durationPriorCountsByOffset == null) {
						durationPriorCountsByOffset = new TreeMap<Double, Map<Integer, Integer>>();
						durationPriorCountsByOffsetByTime.put(currTime, durationPriorCountsByOffset);
					}
					Map<Integer, Integer> durationPriorCounts = durationPriorCountsByOffset.get(beatsOffset);
					if (durationPriorCounts == null) {
						durationPriorCounts = new HashMap<Integer, Integer>();
						durationPriorCountsByOffset.put(beatsOffset, durationPriorCounts);
					}
					
					Utils.incrementValueForKey(durationPriorCounts, noteDurationIdx);
				} else {
					TreeMap<Double, Map<Integer, Map<Integer, Integer>>> durationTransitionCountsByOffset = durationTransitionCountsByOffsetByTime.get(currTime);
					if (durationTransitionCountsByOffset == null) {
						durationTransitionCountsByOffset = new TreeMap<Double, Map<Integer, Map<Integer, Integer>>>();
						durationTransitionCountsByOffsetByTime.put(currTime, durationTransitionCountsByOffset);
					}
					Map<Integer, Map<Integer, Integer>> durationTransitionCounts = durationTransitionCountsByOffset.get(beatsOffset);
					if (durationTransitionCounts == null) {
						durationTransitionCounts = new HashMap<Integer, Map<Integer, Integer>>();
						durationTransitionCountsByOffset.put(beatsOffset, durationTransitionCounts);
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
			Map<Double, SparseSingleOrderMarkovModel<Double>> durationMarkovModelsByOffset = durationMarkovModelsByOffsetByTime.get(sequenceTime);
			if (durationMarkovModelsByOffset == null) {
				durationMarkovModelsByOffset = buildDurationModels(sequenceTime);
			}
			List<Double> durationsAsBeats = generateSequenceOfDurationsAsBeats(durationMarkovModelsByOffset,length, sequenceTime.beats);
			List<Pair<Integer,Double>> pitchDurationPairs = new ArrayList<Pair<Integer,Double>>();
			
			// combine pitch and durations
			for (int i = 0; i < pitchList.size(); i++) {
				pitchDurationPairs.add(new Pair<Integer,Double>(pitchList.get(i), durationsAsBeats.get(i)));
			}
			
			return pitchDurationPairs;
		}

		private Map<Double, SparseSingleOrderMarkovModel<Double>> buildDurationModels(Time sequenceTime) {
			Map<Double, SparseSingleOrderMarkovModel<Double>> durationMarkovModelsByOffset = new HashMap<Double, SparseSingleOrderMarkovModel<Double>>();
			
			TreeMap<Double, Map<Double, Integer>> durationStatesByIndexByOffset = durationStatesByIndexByOffsetByTime.get(sequenceTime);
			if (durationStatesByIndexByOffset == null) {
				throw new RuntimeException("Model was not trained on any XMLs with time signature " + sequenceTime);
			}
			TreeMap<Double, Map<Integer, Integer>> durationPriorCountsByOffset = durationPriorCountsByOffsetByTime.get(sequenceTime);
			TreeMap<Double, Map<Integer, Map<Integer, Integer>>> durationTransitionCountsByOffset = durationTransitionCountsByOffsetByTime.get(sequenceTime);
			
			for (Double beatsOffset : durationStatesByIndexByOffset.keySet()) {
				Map<Double, Integer> durationStatesByIndex = durationStatesByIndexByOffset.get(beatsOffset);
				Map<Integer, Integer> durationPriorCounts = durationPriorCountsByOffset.get(beatsOffset);
				Map<Integer, Map<Integer, Integer>> durationTransitionCounts = durationTransitionCountsByOffset.get(beatsOffset);
				
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
				durationMarkovModelsByOffset.put(beatsOffset, new SparseSingleOrderMarkovModel<Double>(durationStatesByIndex, priors, transitions));
			}
			
			return durationMarkovModelsByOffset;
		}

		private List<Double> generateSequenceOfDurationsAsBeats(
				Map<Double, SparseSingleOrderMarkovModel<Double>> durationMarkovModelsByOffset, int numberOfElements, int beats) {
			List<Double> durationsSequence = new ArrayList<Double>();
			if (numberOfElements == 0) return durationsSequence;
			
			double measurePos = 0.0;
			SparseSingleOrderMarkovModel<Double> durationMarkovModel = durationMarkovModelsByOffset.get(measurePos);
			double nextDuration = durationMarkovModel.sampleStartState();
			durationsSequence.add(nextDuration);
			measurePos += nextDuration % beats;
			
			for (int i = 1; i < numberOfElements; i++) {
				durationMarkovModel = durationMarkovModelsByOffset.get(measurePos); 
				nextDuration = durationMarkovModel.sampleNextState(nextDuration);
				durationsSequence.add(nextDuration);
				measurePos += nextDuration;
				measurePos %= beats;
			}
			
			return durationsSequence;
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
					int pitch = pitchDuration.getFirst();
					
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
