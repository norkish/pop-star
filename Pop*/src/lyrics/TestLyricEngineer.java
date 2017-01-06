package lyrics;

import java.util.List;
import java.util.TreeMap;

import composition.Measure;
import composition.Score;
import data.MusicXML.Note;
import data.MusicXML.NoteLyric;
import data.MusicXML.NoteTie;
import data.MusicXML.Syllabic;
import inspiration.Inspiration;

public class TestLyricEngineer extends LyricalEngineer {

	@Override
	public void addLyrics(Inspiration inspiration, Score score) {
		List<Measure> measures = score.getMeasures();
		
		NoteLyric[] lyrics = loadTestStates();
		int lyrIdx = 0;
		
		for (Measure measure : measures) {
			TreeMap<Double, Note> notes = measure.getNotes();
			for (Note note : notes.values()) {
				if (note.pitch != -1 && note.tie != NoteTie.STOP) {
					note.lyric = lyrics[lyrIdx++];
				}
			}
		}
	}
	
	// Legacy Code
//	private SingleOrderMarkovModel<Lyric> mModel;
//
//	@Override
//	protected void applyVariationToChorus(LyricSegment lyricSegment, Inspiration inspiration,
//			SegmentStructure segmentSubstructures, SegmentType segmentKey, boolean isLast) {
//
//		if (segmentKey.hasLyrics() && segmentKey != SegmentType.CHORUS) {
//			
//		}
//	}
//	
//	@Override
//	protected LyricSegment generateSegmentLyrics(Inspiration inspiration, SegmentStructure segmentSubstructures,
//			SegmentType segmentKey) {
//		
//		if (mModel == null)
//		{
//			mModel = loadTestModel();
//		}
//		
//		int[] testLengths = new int[]{6,5,8,7};
//		List<List<Lyric>> lyricLines = new ArrayList<List<Lyric>>();
//		for (int i = 0; i < segmentSubstructures.linesPerSegment; i++) {
//			List<Constraint<Lyric>> constraints = segmentSubstructures.lyricConstraints.getConstraintsForLine(i);
//			Constraint.reifyConstraints(constraints,lyricLines);
//			NHMM<Lyric> constrainedLyricModel = new NHMM<Lyric>(mModel, testLengths[i], constraints);
//			lyricLines.add(constrainedLyricModel.generate(testLengths[i]));
//		}
//		
//		return new LyricSegment(lyricLines);
//	}
//
//	
//
//	private static SingleOrderMarkovModel<Lyric> loadTestModel() {
//		Lyric[] states = loadTestStates();
//		double[] priors = loadTestPriors(states.length);
//		double[][] transitions = loadTestTransitions(states.length);
//		SingleOrderMarkovModel<Lyric> newModel = new SingleOrderMarkovModel<Lyric>(states, priors, transitions );
//		return newModel;
//	}
//
//	private static double[][] loadTestTransitions(int length) {
//		double[][] transitions = new double[length][length];
//
//		for (int i = 0; i < length; i++) {
//			for (int j = 0; j < length; j++) {
//				// we set transitions from word to next word to .5
//				if(j == (i + 1))
//				{
//					transitions[i][j] = 0.5;
//				}
//				else
//				{
//					transitions[i][j] = 0.0; 
//				}
//			}
//		}
//		
//		return transitions;
//	}
//
//	private static double[] loadTestPriors(int length) {
//		double[] priors = new double[length];
//		
//		priors[0] = .25; // all states equally likely to start
//		priors[6] = .25; // all states equally likely to start
//		priors[11] = .25; // all states equally likely to start
//		priors[19] = .25; // all states equally likely to start
//		
//		return priors;
//	}
//
	private static NoteLyric[] loadTestStates() {
		return new NoteLyric[]{
				new NoteLyric(Syllabic.SINGLE, "It's", false, false),
				new NoteLyric(Syllabic.SINGLE, "nine", false, false),
				new NoteLyric(Syllabic.BEGIN, "o'", false, false),
				new NoteLyric(Syllabic.END, "clock", false, false),
				new NoteLyric(Syllabic.SINGLE, "on", false, false),
				new NoteLyric(Syllabic.SINGLE, "a", false, false),
				new NoteLyric(Syllabic.BEGIN, "Sat", false, false),
				new NoteLyric(Syllabic.MIDDLE, "ur", false, false),
				new NoteLyric(Syllabic.END, "day", false, false),
				new NoteLyric(Syllabic.SINGLE, "The", false, false),
				new NoteLyric(Syllabic.BEGIN, "reg", false, false),
				new NoteLyric(Syllabic.MIDDLE, "u", false, false),
				new NoteLyric(Syllabic.END, "lar", false, false),
				new NoteLyric(Syllabic.SINGLE, "crowd", false, false),
				new NoteLyric(Syllabic.BEGIN, "shuf", false, false),
				new NoteLyric(Syllabic.END, "fles", false, false),
				new NoteLyric(Syllabic.SINGLE, "in", false, false),
				new NoteLyric(Syllabic.SINGLE, "There's", false, false),
				new NoteLyric(Syllabic.SINGLE, "an", false, false),
				new NoteLyric(Syllabic.SINGLE, "old", false, false),
				new NoteLyric(Syllabic.SINGLE, "man", false, false),
				new NoteLyric(Syllabic.BEGIN, "sit", false, false),
				new NoteLyric(Syllabic.END, "ting", false, false),
				new NoteLyric(Syllabic.SINGLE, "next", false, false),
				new NoteLyric(Syllabic.SINGLE, "to", false, false),
				new NoteLyric(Syllabic.SINGLE, "me", false, false),
				new NoteLyric(Syllabic.BEGIN, "Mak", false, false),
				new NoteLyric(Syllabic.END, "ing", false, false),
				new NoteLyric(Syllabic.SINGLE, "love", false, false),
				new NoteLyric(Syllabic.SINGLE, "to", false, false),
				new NoteLyric(Syllabic.SINGLE, "his", false, false),
				new NoteLyric(Syllabic.BEGIN, "ton", false, false),
				new NoteLyric(Syllabic.END, "ic", false, false),
				new NoteLyric(Syllabic.SINGLE, "and", false, false),
				new NoteLyric(Syllabic.SINGLE, "gin", false, false)
		};
	}
}
