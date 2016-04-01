package substructure;

import condition.ExactBinaryMatch;
import condition.Rhyme;
import condition.StrongResolution;
import constraint.Constraint;
import globalstructure.SegmentType;
import harmony.Chord;
import lyrics.Lyric;
import pitch.Pitch;

public class TestSubstructureEngineer extends SubstructureEngineer {

	@Override
	protected void applyVariation(SegmentSubstructure substructure, SegmentType segmentType, boolean isLast) {
		// TODO Auto-generated method stub

	}

	@Override
	protected SegmentSubstructure defineSubstructure(SegmentType segmentType) {
		int linesPerSegment = 4;
		int measuresPerLine = 4;
		int minWordsPerLine = 4;
		int maxWordsPerLine = 8;
		int substructureRepetitions = 2;
		boolean relativeMinorKey = false;

		SegmentSubstructure substructure = new SegmentSubstructure(linesPerSegment, measuresPerLine, minWordsPerLine, maxWordsPerLine, substructureRepetitions, relativeMinorKey);
		
		substructure.addChordConstraint(1, new Constraint<Chord>(Constraint.FINAL_POSITION, new StrongResolution<Chord>(), false));
		
		for (int i = 0; i < 4; i++) {
			substructure.addChordConstraint(2, new Constraint<Chord>(i, new ExactBinaryMatch<Chord>(0,i), true));
		}
		for (int i = 0; i < 0; i++) {
			substructure.addPitchConstraint(2, new Constraint<Pitch>(i, new ExactBinaryMatch<Pitch>(0, i), true));
		}
		
		substructure.addLyricConstraint(3, new Constraint<Lyric>(Constraint.FINAL_POSITION, new Rhyme<Lyric>(1, Constraint.FINAL_POSITION), true));
		substructure.addChordConstraint(3, new Constraint<Chord>(Constraint.FINAL_POSITION, new StrongResolution<Chord>(), true));
				
		return substructure;
	}

}
