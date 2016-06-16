package harmony;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import constraint.Constraint;
import constraint.ConstraintBlock;
import data.DataLoader;
import data.BackedDistribution;
import globalstructure.SegmentType;
import inspiration.Inspiration;
import markov.SparseNHMM;
import markov.SparseSingleOrderMarkovModel;
import substructure.SegmentSubstructure;

public class SegmentSpecificHarmonyEngineer extends HarmonyEngineer {

	private Map<SegmentType, Map<Integer, BackedDistribution<ConstraintBlock<Chord>>>> chordConstraintsDistribution = DataLoader.getChordConstraintsDistribution();
	private Map<SegmentType, SparseSingleOrderMarkovModel<Chord>> mModel = DataLoader.getChordMarkovModel();
	
	@Override
	protected void applyVariation(ProgressionSegment segmentProgression, SegmentType segmentType, boolean isLast) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected ProgressionSegment generateSegmentHarmony(Inspiration inspiration, SegmentSubstructure segmentSubstructures,
			SegmentType segmentKey) {
		
		List<List<Chord>> chordLines = new ArrayList<List<Chord>>();
		ConstraintBlock<Chord> constraintBlock = chordConstraintsDistribution.get(segmentKey).get(segmentSubstructures.linesPerSegment).sampleRandomly();
		
		int chordsPerLine;
		for (int i = 0; i < segmentSubstructures.linesPerSegment; i++) {
			List<Constraint<Chord>> constraints = segmentSubstructures.chordConstraints.getConstraintsForLine(i);
			Constraint.reifyConstraints(constraints,chordLines);
			chordsPerLine = constraintBlock.getLengthConstraint(i);
			SparseNHMM<Chord> constrainedLyricModel = new SparseNHMM<Chord>(mModel.get(segmentKey), chordsPerLine, constraints);
			chordLines.add(constrainedLyricModel.generate(chordsPerLine));
		}
		
		return new ProgressionSegment(chordLines);
	}


}
