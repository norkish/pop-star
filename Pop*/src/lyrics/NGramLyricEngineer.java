package lyrics;

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

public class NGramLyricEngineer extends LyricalEngineer {

	private Map<SegmentType, Map<Integer, BackedDistribution<ConstraintBlock<Lyric>>>> lyricConstraintsDistribution = DataLoader.getLyricConstraintsDistribution();
	private Map<SegmentType, SparseSingleOrderMarkovModel<Lyric>> mModel = DataLoader.getLyricMarkovModel();
	
	@Override
	protected LyricSegment generateSegmentLyrics(Inspiration inspiration, SegmentSubstructure segmentSubstructures,
			SegmentType segmentKey) {
		List<List<Lyric>> lyricLines = new ArrayList<List<Lyric>>();
		if (!segmentKey.hasLyrics()) {
			for (int i = 0; i < segmentSubstructures.linesPerSegment; i++) {
				lyricLines.add(new ArrayList<Lyric>());
			}
			return new LyricSegment(lyricLines);
		}
		
		ConstraintBlock<Lyric> constraintBlock = lyricConstraintsDistribution.get(segmentKey).get(segmentSubstructures.linesPerSegment).sampleRandomly();
		
		int chordsPerLine;
		for (int i = 0; i < segmentSubstructures.linesPerSegment; i++) {
			List<Constraint<Lyric>> constraints = segmentSubstructures.lyricConstraints.getConstraintsForLine(i);
			Constraint.reifyConstraints(constraints,lyricLines);
			chordsPerLine = constraintBlock.getLengthConstraint(i);
			SparseNHMM<Lyric> constrainedLyricModel = new SparseNHMM<Lyric>(mModel.get(segmentKey), chordsPerLine, constraints);
			lyricLines.add(constrainedLyricModel.generate(chordsPerLine));
		}
		
		return new LyricSegment(lyricLines);
	}
}
