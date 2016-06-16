package substructure;

import java.util.Map;

import constraint.ConstraintBlock;
import data.DataLoader;
import data.BackedDistribution;
import globalstructure.SegmentType;
import harmony.Chord;
import lyrics.Lyric;

public class DistributionalSubstructureEngineer extends SubstructureEngineer {
	
	private Map<SegmentType, Map<Integer, BackedDistribution<ConstraintBlock<Lyric>>>> rhymeConstraintsDistribution = DataLoader.getRhymeConstraintsDistribution();
	private Map<SegmentType, Map<Integer, BackedDistribution<ConstraintBlock<Lyric>>>> lyricConstraintsDistribution = DataLoader.getLyricConstraintsDistribution();
	private Map<SegmentType, Map<Integer, BackedDistribution<ConstraintBlock<Chord>>>> chordConstraintsDistribution = DataLoader.getChordConstraintsDistribution();
	private Map<SegmentType, BackedDistribution<Integer>> linesPerSegmentDistribution = DataLoader.getLinesPerSegmentDistribution();

	@Override
	protected void applyVariation(SegmentSubstructure substructure, SegmentType segmentType, boolean isLast) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected SegmentSubstructure defineSubstructure(SegmentType segmentType) {
		int linesPerSegment = linesPerSegmentDistribution.get(segmentType).sampleRandomly();
		
		SegmentSubstructure substructure = new SegmentSubstructure(linesPerSegment);
		
		if (segmentType.hasLyrics()) {
			ConstraintBlock<Lyric> rhymeScheme = rhymeConstraintsDistribution.get(segmentType).get(linesPerSegment).sampleRandomly();
			substructure.addLyricConstraints(rhymeScheme);
			ConstraintBlock<Lyric> lyricConstraints = lyricConstraintsDistribution.get(segmentType).get(linesPerSegment).sampleRandomly();
			substructure.addLyricConstraints(lyricConstraints);
			substructure.addLyricLengthConstraints(lyricConstraints.getLengthConstraints());
		}

		ConstraintBlock<Chord> chordConstraints = chordConstraintsDistribution.get(segmentType).get(linesPerSegment).sampleRandomly();
		substructure.addChordConstraints(chordConstraints);
		substructure.addChordLengthConstraints(chordConstraints.getLengthConstraints());
		
		return substructure;
	}

	public static void main(String[] args) {
		DistributionalSubstructureEngineer e = new DistributionalSubstructureEngineer();
		SegmentSubstructure structure = e.defineSubstructure(SegmentType.INTRO);
		System.out.println(structure);
		structure = e.defineSubstructure(SegmentType.VERSE);
		System.out.println(structure);
		
	}
}
