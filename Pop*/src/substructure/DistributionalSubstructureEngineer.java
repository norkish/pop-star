package substructure;

import java.util.Map;
import java.util.TreeMap;

import condition.Rhyme;
import constraint.Constraint;
import data.DataLoader;
import data.Distribution;
import globalstructure.SegmentType;
import lyrics.Lyric;

public class DistributionalSubstructureEngineer extends SubstructureEngineer {
	
	private Map<SegmentType, Distribution<String>> substructDistrByType = DataLoader.getSubstrDistr();
	private Map<SegmentType, Distribution<Integer>> linesPerSegmentDistribution = DataLoader.getLinesPerSegmentDistribution();

	@Override
	protected void applyVariation(SegmentSubstructure substructure, SegmentType segmentType, boolean isLast) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected SegmentSubstructure defineSubstructure(SegmentType segmentType) {
		int linesPerSegment = linesPerSegmentDistribution.get(segmentType).sampleRandomly();
		int measuresPerLine = 4;//measuresPerLineDistribution.get(segmentType).sampleRandomly();
		int minWordsPerLine = 4;
		int maxWordsPerLine = 8;
		int substructureRepetitions = 2;
		boolean relativeMinorKey = false;

		
		SegmentSubstructure substructure = new SegmentSubstructure(linesPerSegment, measuresPerLine, minWordsPerLine, maxWordsPerLine, substructureRepetitions, relativeMinorKey);
		
		String rhymeScheme = substructDistrByType.get(segmentType).sampleRandomly();
		Map<Character, Integer> lineNumByRhymeLabel = new TreeMap<Character, Integer>();
		char currChar;
		Integer lineNum;
		for (int i = 0; i < rhymeScheme.length(); i++) {
			currChar = rhymeScheme.charAt(i);
			lineNum = lineNumByRhymeLabel.get(currChar);
			if (lineNum != null) {
				substructure.addLyricConstraint(i, new Constraint<Lyric>(Constraint.FINAL_POSITION, new Rhyme<Lyric>(lineNum, Constraint.FINAL_POSITION), true));
			}
			lineNumByRhymeLabel.put(currChar, i);
		}

		
		return substructure;
	}

}
