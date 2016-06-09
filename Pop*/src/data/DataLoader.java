package data;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import globalstructure.SegmentType;
import main.TabDriver;
import tab.CompletedTab;
import utils.Utils;

public class DataLoader {

	private static Distribution<String> gStructDistribution;
	private static Map<SegmentType, Distribution<String>> rhymeSubstructureDistribution;
	private static Map<SegmentType, Distribution<Integer>> linesPerSegmentDistribution;

	static {
		loadDistribution();
	}

	private static void loadDistribution() {
		
		Map<String, Integer> gStructureDistr = new HashMap<String, Integer>();
		Map<Character, Map<String,Integer>> rhymeSubstructDistr = new HashMap<Character, Map<String,Integer>>(8);
		Map<Character, Map<Integer,Integer>> linesPerSegmentDistr = new HashMap<Character, Map<Integer, Integer>>(8);
		
		char[] segmentLabels = new char[]{'I','V','C','B','O'};
		
		for (char c : segmentLabels) {
			rhymeSubstructDistr.put(c, new HashMap<String, Integer>());
			linesPerSegmentDistr.put(c, new HashMap<Integer, Integer>(30));
		}
		
		List<CompletedTab> tabs = TabDriver.loadValidatedTabs();

		//Var initialization
		StringBuilder gStructBldr;
		char currSegmentLabel, prevSegmentLabel = 0;
		int idxOfSegmentBeginning;
		int i;

		for (CompletedTab completedTab : tabs) {
			gStructBldr = new StringBuilder();
			prevSegmentLabel = '\0';
			
			idxOfSegmentBeginning = -1;
			for (i = 0; i < completedTab.length(); i++) {
				
				// extract structure from tab
				currSegmentLabel = completedTab.segmentLabelAt(i);
				if (currSegmentLabel != prevSegmentLabel) {
					if (idxOfSegmentBeginning != -1) {
						incrementCount(linesPerSegmentDistr.get(prevSegmentLabel), i - idxOfSegmentBeginning);
					}
					idxOfSegmentBeginning = i;
					gStructBldr.append(currSegmentLabel);
					prevSegmentLabel = currSegmentLabel;
				}
			}
			
			// include the last segment stats
			incrementCount(linesPerSegmentDistr.get(prevSegmentLabel), i - idxOfSegmentBeginning);

			// insert structure into distribution
			incrementCount(gStructureDistr, gStructBldr.toString());
		}
		
		gStructDistribution = new Distribution<String>(Utils.sort(gStructureDistr, false));
		linesPerSegmentDistribution = new EnumMap<SegmentType, Distribution<Integer>>(SegmentType.class);
		for (Entry<Character, Map<Integer, Integer>> entry : linesPerSegmentDistr.entrySet()) {
			linesPerSegmentDistribution.put(SegmentType.valueOf(entry.getKey()), new Distribution<Integer>(Utils.sort(entry.getValue(), false)));
		}
	}

	private static <T> void incrementCount(Map<T, Integer> gStructureDistr, T gStructure) {
		Integer count = gStructureDistr.get(gStructure);
		if (count == null) {
			gStructureDistr.put(gStructure, 1);
		} else {
			gStructureDistr.put(gStructure, count + 1);
		}		
	}

	private static Distribution<String> getGlobalStructureDistribution() {
		return gStructDistribution;
	}
	
	public static void main(String[] args) {
		Distribution<String> gStructDist = DataLoader.getGlobalStructureDistribution();
		Map<SegmentType, Distribution<Integer>> linesPerSegmentDistribution = DataLoader.getLinesPerSegmentDistribution();

		System.out.println(gStructDist);
		System.out.println(linesPerSegmentDistribution);
		
		for (int i = 0; i < 10; i++) {
			System.out.println(gStructDist.sampleRandomly());
		}
		for (int i = 0; i < 3; i++) {
			System.out.println(linesPerSegmentDistribution.get(SegmentType.INTRO).sampleRandomly());
			System.out.println(linesPerSegmentDistribution.get(SegmentType.CHORUS).sampleRandomly());
			System.out.println(linesPerSegmentDistribution.get(SegmentType.BRIDGE).sampleRandomly());
			System.out.println(linesPerSegmentDistribution.get(SegmentType.OUTRO).sampleRandomly());
			System.out.println(linesPerSegmentDistribution.get(SegmentType.VERSE).sampleRandomly());
		}
	}

	public static Map<SegmentType, Distribution<String>> getSubstrDistr() {
		return rhymeSubstructureDistribution;
	}

	public static Map<SegmentType, Distribution<Integer>> getLinesPerSegmentDistribution() {
		return linesPerSegmentDistribution;
	}
}
