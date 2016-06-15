package harmony;

import java.util.ArrayList;
import java.util.List;

import constraint.Constraint;
import data.DataLoader;
import globalstructure.SegmentType;
import inspiration.Inspiration;
import markov.SparseNHMM;
import markov.SparseSingleOrderMarkovModel;
import substructure.SegmentSubstructure;

public class SegmentSpecificHarmonyEngineer extends HarmonyEngineer {

	private SparseSingleOrderMarkovModel<Chord> mModel = DataLoader.getChordMarkovModel();
	
	@Override
	protected void applyVariation(ProgressionSegment segmentProgression, SegmentType segmentType, boolean isLast) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected ProgressionSegment generateSegmentHarmony(Inspiration inspiration, SegmentSubstructure segmentSubstructures,
			SegmentType segmentKey) {
		if (mModel == null)
		{
//			mModel = loadTestModel();
		}
		
		int testChordsPerLine = 4;
		List<List<Chord>> chordLines = new ArrayList<List<Chord>>();
		for (int i = 0; i < segmentSubstructures.linesPerSegment; i++) {
			List<Constraint<Chord>> constraints = segmentSubstructures.chordConstraints.getConstraintsForLine(i);
			Constraint.reifyConstraints(constraints,chordLines);
			SparseNHMM<Chord> constrainedLyricModel = new SparseNHMM<Chord>(mModel, testChordsPerLine, constraints);
			chordLines.add(constrainedLyricModel.generate(testChordsPerLine));
		}
		
		return new ProgressionSegment(chordLines);
	}


}
