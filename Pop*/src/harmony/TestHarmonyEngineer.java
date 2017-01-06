package harmony;

import composition.Score;
import data.MusicXML.Bass;
import data.MusicXML.Harmony;
import data.MusicXML.Quality;
import data.MusicXML.Root;
import inspiration.Inspiration;
import markov.SingleOrderMarkovModel;
import pitch.Pitch;

public class TestHarmonyEngineer extends HarmonyEngineer {

	private SingleOrderMarkovModel<Chord> mModel;

	@Override
	public void addHarmony(Inspiration inspiration, Score score) {
		// Just add the BJ harmony for the test harmony engineer
		assert score.length() == 17 : "TestHarmonyEngineer failed";
		Quality majorQuality = new Quality();
		Quality minorQuality = new Quality();
		minorQuality.parseKindContentText("min");
		Quality dominantSevenQuality = new Quality();
		dominantSevenQuality.parseKindContentText("7");
		score.addHarmony(0, 2.0, new Harmony(new Root(Pitch.getPitchValue("G")), dominantSevenQuality, null));
		score.addHarmony(1, 0.0, new Harmony(new Root(Pitch.getPitchValue("C")), majorQuality, null));
		score.addHarmony(2, 0.0, new Harmony(new Root(Pitch.getPitchValue("E")), minorQuality, new Bass(Pitch.getPitchValue("B"))));
		score.addHarmony(3, 0.0, new Harmony(new Root(Pitch.getPitchValue("F")), majorQuality, new Bass(Pitch.getPitchValue("A"))));
		score.addHarmony(4, 0.0, new Harmony(new Root(Pitch.getPitchValue("C")), majorQuality, new Bass(Pitch.getPitchValue("G"))));
		score.addHarmony(5, 0.0, new Harmony(new Root(Pitch.getPitchValue("F")), majorQuality, null));
		score.addHarmony(6, 0.0, new Harmony(new Root(Pitch.getPitchValue("C")), majorQuality, new Bass(Pitch.getPitchValue("E"))));
		score.addHarmony(7, 0.0, new Harmony(new Root(Pitch.getPitchValue("D")), dominantSevenQuality, null));
		score.addHarmony(8, 0.0, new Harmony(new Root(Pitch.getPitchValue("G")), majorQuality, null));
		score.addHarmony(9, 0.0, new Harmony(new Root(Pitch.getPitchValue("C")), majorQuality, null));
		score.addHarmony(10, 0.0, new Harmony(new Root(Pitch.getPitchValue("E")), minorQuality, new Bass(Pitch.getPitchValue("B"))));
		score.addHarmony(11, 0.0, new Harmony(new Root(Pitch.getPitchValue("F")), majorQuality, new Bass(Pitch.getPitchValue("A"))));
		score.addHarmony(12, 0.0, new Harmony(new Root(Pitch.getPitchValue("C")), majorQuality, new Bass(Pitch.getPitchValue("G"))));
		score.addHarmony(13, 0.0, new Harmony(new Root(Pitch.getPitchValue("F")), majorQuality, null));
		score.addHarmony(14, 0.0, new Harmony(new Root(Pitch.getPitchValue("F")), majorQuality, new Bass(Pitch.getPitchValue("G"))));
		score.addHarmony(14, 4.0, new Harmony(new Root(Pitch.getPitchValue("F")), majorQuality, new Bass(Pitch.getPitchValue("A"))));
		score.addHarmony(15, 0.0, new Harmony(new Root(Pitch.getPitchValue("C")), majorQuality, null));
		score.addHarmony(16, 0.0, new Harmony(new Root(Pitch.getPitchValue("C")), majorQuality, null));
	}
	
//	@Override
//	protected void applyVariation(ProgressionSegment segmentProgression, Inspiration inspiration, SegmentStructure segmentSubstructures, SegmentType segmentType, boolean isLast){
//		// TODO Auto-generated method stub
//
//	}
//
//	@Override
//	protected ProgressionSegment generateSegmentHarmony(Inspiration inspiration, SegmentStructure segmentSubstructures,
//			SegmentType segmentType) {
//		if (mModel == null)
//		{
//			mModel = loadTestModel();
//		}
//		
//		int testChordsPerLine = 4;
//		List<List<Chord>> chordLines = new ArrayList<List<Chord>>();
//		for (int i = 0; i < segmentSubstructures.linesPerSegment; i++) {
//			List<Constraint<Chord>> constraints = segmentSubstructures.chordConstraints.getConstraintsForLine(i);
//			Constraint.reifyConstraints(constraints,chordLines);
//			NHMM<Chord> constrainedLyricModel = new NHMM<Chord>(mModel, testChordsPerLine, constraints);
//			chordLines.add(constrainedLyricModel.generate(testChordsPerLine));
//		}
//		
//		return new ProgressionSegment(chordLines);
//	}
//
//	private static SingleOrderMarkovModel<Chord> loadTestModel() {
//		Chord[] states = loadTestStates();
//		double[] priors = loadTestPriors(states.length);
//		double[][] transitions = loadTestTransitions(states.length);
//		SingleOrderMarkovModel<Chord> newModel = new SingleOrderMarkovModel<Chord>(states, priors, transitions );
//		return newModel;
//	}
//
//	private static double[][] loadTestTransitions(int length) {
//		double[][] transitions = new double[length][length];
//
//		for (int i = 0; i < length; i++) {
//			for (int j = 0; j < length; j++) {
//				transitions[i][j] = 0.0; 
//			}
//		}
//		
//		transitions[0][0] = 1/3.0;
//		transitions[0][1] = 2/3.0;
//		transitions[1][2] = 1.0;
//		transitions[2][3] = 1.0;
//		transitions[3][4] = 1.0;
//		transitions[4][5] = 0.5;
//		transitions[4][8] = 0.5;
//		transitions[5][6] = 1.0;
//		transitions[6][7] = 1.0;
//		transitions[7][0] = 1.0;
//		transitions[8][0] = 1.0;
//		
//		return transitions;
//	}
//
//	private static double[] loadTestPriors(int length) {
//		double[] priors = new double[length];
//		
//		for (int i = 1; i < priors.length; i++) {
//			priors[i] = 0.0 / length; // all states equally likely to start
//		}
//		priors[0] = 0.5;
//		priors[4] = 0.5;
//		
//		return priors;
//	}
//
//	private static Chord[] loadTestStates() {
//		return new Chord[]{
//				Chord.parse("C", true),
//				Chord.parse("G/B", true),
//				Chord.parse("F/A", true),
//				Chord.parse("C/G", true),
//				Chord.parse("F", true),
//				Chord.parse("C/E", true),
//				Chord.parse("D", true),
//				Chord.parse("G", true),
//				Chord.parse("F/G", true),
//		};
//	}
	
}
