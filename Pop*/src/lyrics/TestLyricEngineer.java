package lyrics;

import java.util.ArrayList;
import java.util.List;

import condition.ConstraintCondition;
import condition.DelayedConstraintCondition;
import constraint.Constraint;
import globalstructure.SegmentType;
import inspiration.Inspiration;
import markov.NHMM;
import markov.SingleOrderMarkovModel;
import substructure.SegmentSubstructure;

public class TestLyricEngineer extends LyricalEngineer {

	private SingleOrderMarkovModel<Lyric> mModel;

	@Override
	protected LyricSegment generateSegmentLyrics(Inspiration inspiration, SegmentSubstructure segmentSubstructures,
			SegmentType segmentKey) {
		
		if (mModel == null)
		{
			mModel = loadTestModel();
		}
		
		int[] testLengths = new int[]{6,5,8,7};
		List<List<Lyric>> lyricLines = new ArrayList<List<Lyric>>();
		for (int i = 0; i < segmentSubstructures.linesPerSegment; i++) {
			List<Constraint<Lyric>> constraints = segmentSubstructures.lyricConstraints.getConstraintsForLine(i);
			Constraint.reifyConstraints(constraints,lyricLines);
			NHMM<Lyric> constrainedLyricModel = new NHMM<Lyric>(mModel, testLengths[i], constraints);
			lyricLines.add(constrainedLyricModel.generate(testLengths[i]));
		}
		
		return new LyricSegment(lyricLines);
	}

	

	private static SingleOrderMarkovModel<Lyric> loadTestModel() {
		Lyric[] states = loadTestStates();
		double[] priors = loadTestPriors(states.length);
		double[][] transitions = loadTestTransitions(states.length);
		SingleOrderMarkovModel<Lyric> newModel = new SingleOrderMarkovModel<Lyric>(states, priors, transitions );
		return newModel;
	}

	private static double[][] loadTestTransitions(int length) {
		double[][] transitions = new double[length][length];

		for (int i = 0; i < length; i++) {
			for (int j = 0; j < length; j++) {
				// we set transitions from word to next word to .5
				if(j == (i + 1))
				{
					transitions[i][j] = 0.5;
				}
				else
				{
					transitions[i][j] = 0.0; 
				}
			}
		}
		
		return transitions;
	}

	private static double[] loadTestPriors(int length) {
		double[] priors = new double[length];
		
		priors[0] = .25; // all states equally likely to start
		priors[6] = .25; // all states equally likely to start
		priors[11] = .25; // all states equally likely to start
		priors[19] = .25; // all states equally likely to start
		
		return priors;
	}

	private static Lyric[] loadTestStates() {
		return new Lyric[]{
				new Lyric("It's"),
				new Lyric("nine"),
				new Lyric("o'clock"),
				new Lyric("on"),
				new Lyric("a"),
				new Lyric("Saturday"),
				new Lyric("The"),
				new Lyric("regular"),
				new Lyric("crowd"),
				new Lyric("shuffles"),
				new Lyric("in"),
				new Lyric("There's"),
				new Lyric("an"),
				new Lyric("old"),
				new Lyric("man"),
				new Lyric("sitting"),
				new Lyric("next"),
				new Lyric("to"),
				new Lyric("me"),
				new Lyric("Making"),
				new Lyric("love"),
				new Lyric("to"),
				new Lyric("his"),
				new Lyric("tonic"),
				new Lyric("and"),
				new Lyric("gin")
		};
	}
}
