package harmony;

import java.util.ArrayList;
import java.util.List;

import constraint.Constraint;
import globalstructure.SegmentType;
import inspiration.Inspiration;
import lyrics.Lyric;
import lyrics.LyricSegment;
import markov.NHMM;
import markov.SingleOrderMarkovModel;
import substructure.SegmentSubstructure;

public class TestHarmonyEngineer extends HarmonyEngineer {

	private SingleOrderMarkovModel<Chord> mModel;
	
	@Override
	protected void applyVariation(ProgressionSegment segmentProgression, SegmentType segmentType, boolean isLast) {
		// TODO Auto-generated method stub

	}

	@Override
	protected ProgressionSegment generateSegmentHarmony(Inspiration inspiration, SegmentSubstructure segmentSubstructures,
			SegmentType segmentType) {
		if (mModel == null)
		{
			mModel = loadTestModel();
		}
		
		int testChordsPerLine = 4;
		List<List<Chord>> chordLines = new ArrayList<List<Chord>>();
		for (int i = 0; i < segmentSubstructures.linesPerSegment; i++) {
			List<Constraint<Chord>> constraints = segmentSubstructures.chordConstraints.get(i);
			Constraint.reifyConstraints(constraints,chordLines);
			NHMM<Chord> constrainedLyricModel = new NHMM<Chord>(mModel, testChordsPerLine, segmentSubstructures.chordConstraints.get(i));
			chordLines.add(constrainedLyricModel.generate(testChordsPerLine));
		}
		
		return new ProgressionSegment(chordLines);
	}

	private static SingleOrderMarkovModel<Chord> loadTestModel() {
		Chord[] states = loadTestStates();
		double[] priors = loadTestPriors(states.length);
		double[][] transitions = loadTestTransitions(states.length);
		SingleOrderMarkovModel<Chord> newModel = new SingleOrderMarkovModel<Chord>(states, priors, transitions );
		return newModel;
	}

	private static double[][] loadTestTransitions(int length) {
		double[][] transitions = new double[length][length];

		for (int i = 0; i < length; i++) {
			for (int j = 0; j < length; j++) {
				transitions[i][j] = 0.0; 
			}
		}
		
		transitions[0][0] = 1/3.0;
		transitions[0][1] = 2/3.0;
		transitions[1][2] = 1.0;
		transitions[2][3] = 1.0;
		transitions[3][4] = 1.0;
		transitions[4][5] = 0.5;
		transitions[4][8] = 0.5;
		transitions[5][6] = 1.0;
		transitions[6][7] = 1.0;
		transitions[7][0] = 1.0;
		transitions[8][0] = 1.0;
		
		return transitions;
	}

	private static double[] loadTestPriors(int length) {
		double[] priors = new double[length];
		
		for (int i = 1; i < priors.length; i++) {
			priors[i] = 0.0 / length; // all states equally likely to start
		}
		priors[0] = 0.5;
		priors[4] = 0.5;
		
		return priors;
	}

	private static Chord[] loadTestStates() {
		return new Chord[]{
				new Chord("C"),
				new Chord("G/B"),
				new Chord("F/A"),
				new Chord("C/G"),
				new Chord("F"),
				new Chord("C/E"),
				new Chord("D"),
				new Chord("G"),
				new Chord("F/G")
		};
	}
	
}
