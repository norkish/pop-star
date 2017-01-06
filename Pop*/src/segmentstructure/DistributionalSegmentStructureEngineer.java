package segmentstructure;

import java.util.List;

import composition.Measure;
import globalstructure.SegmentType;

public class DistributionalSegmentStructureEngineer extends SegmentStructureEngineer {

	@Override
	public SegmentStructure defineSegmentStructure(SegmentType segmentType) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Measure> instantiateSegmentStructure(SegmentType segmentType, SegmentStructure segmentStructure,
			boolean lastOfKind, boolean lastSegment) {
		// TODO Auto-generated method stub
		return null;
	}
	
	
	// legacy code
//	private Map<SegmentType, Map<Integer, BackedDistribution<ConstraintBlock<Lyric>>>> rhymeConstraintsDistribution = DataLoader.getRhymeConstraintsDistribution();
//	private Map<SegmentType, Map<Integer, BackedDistribution<ConstraintBlock<Lyric>>>> lyricConstraintsDistribution = DataLoader.getLyricConstraintsDistribution();
//	private Map<SegmentType, Map<Integer, BackedDistribution<ConstraintBlock<Chord>>>> chordConstraintsDistribution = DataLoader.getChordConstraintsDistribution();
//	private Map<SegmentType, BackedDistribution<Integer>> linesPerSegmentDistribution = DataLoader.getLinesPerSegmentDistribution();
//
//	@Override
//	protected void applyVariation(SegmentStructure substructure, SegmentType segmentType, boolean isLast) {
//		// TODO Auto-generated method stub
//		
//	}
//
//	@Override
//	protected SegmentStructure defineSubstructure(SegmentType segmentType) {
//		int linesPerSegment = linesPerSegmentDistribution.get(segmentType).sampleRandomly();
//		
//		SegmentStructure substructure = new SegmentStructure(linesPerSegment);
//		
//		if (segmentType.hasLyrics()) {
//			ConstraintBlock<Lyric> rhymeScheme = rhymeConstraintsDistribution.get(segmentType).get(linesPerSegment).sampleRandomly();
//			substructure.addLyricConstraints(rhymeScheme);
//			ConstraintBlock<Lyric> lyricConstraints = lyricConstraintsDistribution.get(segmentType).get(linesPerSegment).sampleRandomly();
//			substructure.addLyricConstraints(lyricConstraints);
//			substructure.addLyricLengthConstraints(lyricConstraints.getLengthConstraints());
//		}
//
//		ConstraintBlock<Chord> chordConstraints = chordConstraintsDistribution.get(segmentType).get(linesPerSegment).sampleRandomly();
//		substructure.addChordConstraints(chordConstraints);
//		substructure.addChordLengthConstraints(chordConstraints.getLengthConstraints());
//		
//		return substructure;
//	}
//
//	public static void main(String[] args) {
//		DistributionalSegmentStructureEngineer e = new DistributionalSegmentStructureEngineer();
//		SegmentStructure structure = e.defineSubstructure(SegmentType.INTRO);
//		System.out.println(structure);
//		structure = e.defineSubstructure(SegmentType.VERSE);
//		System.out.println(structure);
//	}
}
