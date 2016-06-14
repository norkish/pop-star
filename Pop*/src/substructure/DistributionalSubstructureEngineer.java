package substructure;

import java.util.Map;

import condition.Rhyme;
import constraint.Constraint;
import data.DataLoader;
import data.Distribution;
import globalstructure.SegmentType;
import lyrics.Lyric;

public class DistributionalSubstructureEngineer extends SubstructureEngineer {
	
	private Map<SegmentType, Map<Integer, Distribution<String>>> rhymeSubstructDistrByType = DataLoader.getRhymingSubstructureDistribution();
	private Map<SegmentType, Distribution<Integer>> linesPerSegmentDistribution = DataLoader.getLinesPerSegmentDistribution();
	private Map<SegmentType, Map<Integer, Distribution<Integer>>> measuresPerLineDistribution = DataLoader.getChordsPerLineDistribution();

	@Override
	protected void applyVariation(SegmentSubstructure substructure, SegmentType segmentType, boolean isLast) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected SegmentSubstructure defineSubstructure(SegmentType segmentType) {
		int linesPerSegment = linesPerSegmentDistribution.get(segmentType).sampleRandomly();
		int measuresPerLine = measuresPerLineDistribution.get(segmentType).get(linesPerSegment).sampleRandomly();
		
		SegmentSubstructure substructure = new SegmentSubstructure(linesPerSegment, measuresPerLine);
		
		if (segmentType.hasLyrics()) {
			String rhymeScheme = rhymeSubstructDistrByType.get(segmentType).get(linesPerSegment).sampleRandomly();
			Integer linesPrev;
			char rhymeSchemeChar;
			for (int i = 0; i < rhymeScheme.length(); i++) {
				rhymeSchemeChar = rhymeScheme.charAt(i);
				if (rhymeSchemeChar != '0') {
					linesPrev = rhymeSchemeChar - '0';
					if (linesPrev <= i) {
						substructure.addLyricConstraint(i, new Constraint<Lyric>(Constraint.FINAL_POSITION, new Rhyme<Lyric>(i-linesPrev, Constraint.FINAL_POSITION), true));
					}
				}
			}
		}
		
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
