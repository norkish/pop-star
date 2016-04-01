package substructure;

import condition.RhymeCondition;
import condition.StrongResolutionCondition;
import constraint.Constraint;
import constraint.DelayedBinaryConstraint;
import constraint.UnaryConstraint;
import globalstructure.SegmentType;
import harmony.Chord;
import lyrics.Lyric;
import pitch.Pitch;

public class TestSubstructureEngineer extends SubstructureEngineer {

	@Override
	protected void applyVariation(Substructure substructure, SegmentType segmentType, boolean isLast) {
		// TODO Auto-generated method stub

	}

	@Override
	protected Substructure defineSubstructure(SegmentType segmentType) {
		int linesPerSegment = 4;
		int measuresPerLine = 4;
		int minWordsPerLine = 4;
		int maxWordsPerLine = 8;
		int substructureRepetitions = 2;
		boolean relativeMinorKey = false;

		Substructure substructure = new Substructure(linesPerSegment, measuresPerLine, minWordsPerLine, maxWordsPerLine, substructureRepetitions, relativeMinorKey);
		
		substructure.addChordConstraint(1, new UnaryConstraint<Chord>(Constraint.LAST, new StrongResolutionCondition<Chord>(), false));
		substructure.addChordConstraint(2, new DelayedBinaryConstraint<Chord>(0, Constraint.ALL_POSITIONS, 0, null, false));
		substructure.addPitchConstraint(2, new DelayedBinaryConstraint<Pitch>(0, Constraint.ALL_POSITIONS, 0, null, false));
		substructure.addLyricConstraint(2, new DelayedBinaryConstraint<Lyric>(1, Constraint.LAST, Constraint.LAST, new RhymeCondition<Lyric>(1.0), true));
		substructure.addLyricConstraint(3, new DelayedBinaryConstraint<Lyric>(1, Constraint.LAST, Constraint.LAST, new RhymeCondition<Lyric>(1.0), true));
		substructure.addChordConstraint(3, new UnaryConstraint<Chord>(Constraint.LAST, new StrongResolutionCondition<Chord>(), true));
				
		return substructure;
	}

}
