package melody;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import composition.Measure;
import composition.Score;
import constraint.Constraint;
import data.MusicXMLModel;
import data.MusicXMLModelLearner;
import data.MusicXMLParser.Key;
import data.MusicXMLParser.Note;
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

		SparseSingleOrderMarkovModel<Integer> markovModel;
		Map<Integer, Integer> statesByIndex = new HashMap<Integer, Integer>();// states are pitches for now
		Map<Integer, Integer> priorCounts = new HashMap<Integer, Integer>();
		Map<Integer, Map<Integer, Integer>> transitionCounts = new HashMap<Integer, Map<Integer, Integer>>();

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
			markovModel = null;

			Integer prevNoteIdx = -1;

			// TODO: condition on duration, condition on segment type, condition on chord
			for (Triple<Integer, Integer, Note> measureOffsetNote : musicXML.notesByMeasure) {
				// int measure = measureOffsetNote.getFirst();
				// int offset = measureOffsetNote.getSecond();
				Note note = measureOffsetNote.getThird();
				if (note == null)
					continue;

				Integer noteIdx = statesByIndex.get(note.pitch);
				if (noteIdx == null) {
					noteIdx = statesByIndex.size();
					statesByIndex.put(note.pitch, noteIdx);
				}

				if (prevNoteIdx == -1) {
					Utils.incrementValueForKey(priorCounts, noteIdx);
				} else {
					Utils.incrementValueForKeys(transitionCounts, prevNoteIdx, noteIdx);
				}

				prevNoteIdx = noteIdx;
			}

		}

		public String toString() {
			StringBuilder str = new StringBuilder();

			// string representation of model

			return str.toString();
		}

		public List<Integer> sampleMelodySequenceOfLength(int length, SegmentType type,
				List<Constraint<Note>> contextualConstraints) {
			// TODO Add constraints according to type, including constraints
			// which depend on the harm sequence
			if (markovModel == null) {
				markovModel = buildModel();
			}
			SparseNHMM<Integer> constrainedModel = new SparseNHMM<Integer>(markovModel, length,
					new ArrayList<Constraint<Integer>>());
			return constrainedModel.generate(length);
		}

		private SparseSingleOrderMarkovModel<Integer> buildModel() {
			Map<Integer, Double> priors = new HashMap<Integer, Double>();
			Map<Integer, Map<Integer, Double>> transitions = new HashMap<Integer, Map<Integer, Double>>();

			double totalCount = 0;
			for (Integer count : priorCounts.values()) {
				totalCount += count;
			}
			for (Entry<Integer, Integer> entry : priorCounts.entrySet()) {
				priors.put(entry.getKey(), entry.getValue() / totalCount);
			}

			for (Entry<Integer, Map<Integer, Integer>> outerEntry : transitionCounts.entrySet()) {
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

			return new SparseSingleOrderMarkovModel<Integer>(statesByIndex, priors, transitions);
		}
	}

	private SegmentSpecificMelodyEngineerMusicXMLModel model;
	private Random rand = new Random();

	public SegmentSpecificMelodyEngineer() {
		this.model = (SegmentSpecificMelodyEngineerMusicXMLModel) MusicXMLModelLearner
				.getTrainedModel(this.getClass());
	}

	@Override
	public void addMelody(Inspiration inspiration, Score score) {
		List<Pair<SegmentType, Integer>> segmentsLengths = score.getSegmentsLengths();

		for (SegmentType type : new SegmentType[] { SegmentType.CHORUS, SegmentType.VERSE, SegmentType.BRIDGE,
				SegmentType.INTRO, SegmentType.OUTRO, SegmentType.INTERLUDE }) {
			int length = getMinLengthForSegmentType(segmentsLengths, type);
			// TODO: may want to generate new sequences for every repetition if
			// length varies, probably easiest that way rather than adapting
			// existing seq
			if (length != Integer.MAX_VALUE) {
				length = 16;
				List<Integer> melodySeq = model.sampleMelodySequenceOfLength(length, type, null);
				addMelodyBySegmentType(score, melodySeq, type);
			}
		}

	}

	private void addMelodyBySegmentType(Score score, List<Integer> melodySeq, SegmentType type) {
		SegmentType prevType = null;
		int counter = -1;
		for (Measure measure : score.getMeasures()) {
			if (measure.segmentType != prevType) {
				if (measure.segmentType == type) {
					counter = 0;
				}
				prevType = measure.segmentType;
			}

			if (measure.segmentType == type) {
				Time currTime = measure.time;
				Key currKey = measure.key;
				int divisionsPerQuarterNote = measure.divisionsPerQuarterNote;
				
				int accumulativeDivisions = 0;
				final int divsPerQuarter = divisionsPerQuarterNote;
				final int totalMeasureDivisions = (int) (currTime.beats * divsPerQuarter * (4.0/currTime.beatType));
				
				while (accumulativeDivisions < totalMeasureDivisions) {
					int pitch = melodySeq.get(counter);
					if (pitch == 56) pitch = -1; // rest
					int divisionsToAdd =  rand .nextInt(totalMeasureDivisions-accumulativeDivisions) + 1;
					List<Note> notesToAdd = createTiedNoteWithDuration(divisionsToAdd, pitch, divsPerQuarter);
					for (Note note : notesToAdd) {
						measure.addNote(((double)accumulativeDivisions)/divsPerQuarter, note);
						accumulativeDivisions += note.duration;
					}
				}
				counter++;
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
