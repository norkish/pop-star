package data;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;

import globalstructure.SegmentType;
import harmony.Chord;
import main.TabDriver;
import tab.CompletedTab;
import utils.Utils;

public class DataLoader {

	private static Distribution<Integer> keyDistribution;
	private static Distribution<String> gStructDistribution;
	// We have a distribution of rhyme schemes conditioned on SegmentType and number of lines per segment
	private static Map<SegmentType, Map<Integer, Distribution<String>>> rhymeSubstructureDistribution;
	// We have a distribution of the number of lines per segment conditioned on the SegmentType
	private static Map<SegmentType, Distribution<Integer>> linesPerSegmentDistribution;
	
	// We need a distribution of the number of chords per line conditioned on rhyme schemes (and possibly conditioned on SegType)
	private static Map<SegmentType, Map<Integer, Distribution<Integer>>> chordsPerLineDistribution; // conditioned on segment and on lines per segment
	// We need a distribution of repetitive subsequences of chords per line conditioned on rhyme scheme and SegType
	
	// We need a distribution of subrhyme schemes conditioned on rhyme schemes (could also be conditioned on SegType)
	
	// We need a distribution of the number of words per line conditioned on subrhyme scheme and number of lines in the segment
	// We need a distribution of repetitive subsequences of lyrics per line conditioned on rhyme scheme and SegType
	// We need a distribution of the variation in words per line between paired lines (as per rhyme scheme)
	// We need a distribution of chord transitions, including intersegmental chord transitions, starting chords, ending chords, etc.
	
	static {
		loadDistribution();
	}

	private static void loadDistribution() {
		
		//Variable initialization
		Map<String, List<Integer>> gStructureDistr = new HashMap<String, List<Integer>>();
		Map<SegmentType, Map<Integer, Map<String,List<Integer>>>> rhymeSubstructDistr = new EnumMap<SegmentType, Map<Integer,Map<String,List<Integer>>>>(SegmentType.class);
		Map<SegmentType, Map<Integer,List<Integer>>> linesPerSegmentDistr = new EnumMap<SegmentType, Map<Integer, List<Integer>>>(SegmentType.class);
		Map<SegmentType, Map<Integer,Map<Integer,List<Integer>>>> measuresPerLineDistr = new EnumMap<SegmentType, Map<Integer, Map<Integer,List<Integer>>>>(SegmentType.class);
		Map<Integer, List<Integer>> keyDistr = new HashMap<Integer, List<Integer>>(); // use most common pitch names
		
		for (SegmentType type : SegmentType.values()) {
			linesPerSegmentDistr.put(type, new HashMap<Integer, List<Integer>>(30));

			if (type.hasLyrics()) { 
				rhymeSubstructDistr.put(type, new HashMap<Integer, Map<String, List<Integer>>>(30));
			}
		}
		
		List<CompletedTab> tabs = TabDriver.loadValidatedTabs();

		StringBuilder gStructBldr, rhymeStructBldr = null;
		SegmentType currSegmentLabel, prevSegmentLabel = null;
		int idxOfSegmentBeginning;
		int i;
		int segmentLength = 0;
		Map<String, List<Integer>> rhymeSubstructDistrForTypeLen;
		Map<Integer, List<Integer>> measuresPerLineDistrForTypeLen;
		char currSegmentLabelChar;
		double totalMeasuresInSegment;
		List<SortedMap<Integer, Chord>> chords;
		CompletedTab completedTab;
		// End variable initialization

		for (int tabId = 0; tabId < tabs.size(); tabId++) {
			completedTab = tabs.get(tabId);
			if (completedTab.length() == 0) continue;
			
			chords = completedTab.chords;
			incrementCount(keyDistr,completedTab.pitch, tabId);
			
			gStructBldr = new StringBuilder();
			prevSegmentLabel = null;
			
			idxOfSegmentBeginning = -1;
			totalMeasuresInSegment = 0;
			for (i = 0; i < completedTab.length(); i++) {
				
				// extract structure from tab
				currSegmentLabelChar = completedTab.segmentLabelAt(i);
				currSegmentLabel = SegmentType.valueOf(currSegmentLabelChar);
				if (currSegmentLabel != prevSegmentLabel) {
					if (idxOfSegmentBeginning != -1) {
						segmentLength = i - idxOfSegmentBeginning;
						incrementCount(linesPerSegmentDistr.get(prevSegmentLabel), segmentLength, tabId);
						if (prevSegmentLabel.hasLyrics()) {
							rhymeSubstructDistrForTypeLen = getMapForKey(rhymeSubstructDistr.get(prevSegmentLabel),segmentLength);
							incrementCount(rhymeSubstructDistrForTypeLen, rhymeStructBldr.toString(), tabId);
						}
						measuresPerLineDistrForTypeLen = getMapForKey(measuresPerLineDistr.get(prevSegmentLabel), segmentLength);
						incrementCount(measuresPerLineDistrForTypeLen, (int) Math.ceil(totalMeasuresInSegment/segmentLength), tabId);
					}
					rhymeStructBldr = new StringBuilder();
					
					idxOfSegmentBeginning = i;
					totalMeasuresInSegment = 0;
					gStructBldr.append(currSegmentLabelChar);
					prevSegmentLabel = currSegmentLabel;
				}
				rhymeStructBldr.append(completedTab.rhymeSchemeAt(i));
				totalMeasuresInSegment += chords.size();
				
				// analyze chord line
				
			}
			
			// include the last segment stats
			segmentLength = i - idxOfSegmentBeginning;
			incrementCount(linesPerSegmentDistr.get(prevSegmentLabel), segmentLength, tabId);
			if (prevSegmentLabel != SegmentType.OUTRO && prevSegmentLabel != SegmentType.INTRO) {
				rhymeSubstructDistrForTypeLen = getMapForKey(rhymeSubstructDistr.get(prevSegmentLabel),segmentLength);
				incrementCount(rhymeSubstructDistrForTypeLen, rhymeStructBldr.toString(), tabId);
			}
			measuresPerLineDistrForTypeLen = getMapForKey(measuresPerLineDistr.get(prevSegmentLabel), segmentLength);
			incrementCount(measuresPerLineDistrForTypeLen, (int) Math.ceil(totalMeasuresInSegment/segmentLength), tabId);

			// insert structure into distribution
			incrementCount(gStructureDistr, gStructBldr.toString(), tabId);
		}
		
		keyDistribution = new Distribution<Integer>(keyDistr);
		
		gStructDistribution = new Distribution<String>(Utils.sortByListSize(gStructureDistr, false));
		
		rhymeSubstructureDistribution = new EnumMap<SegmentType, Map<Integer, Distribution<String>>>(SegmentType.class);
		HashMap<Integer, Distribution<String>> rhymeSubstrDistrBySegType;
		for (Entry<SegmentType, Map<Integer, Map<String, List<Integer>>>> entry : rhymeSubstructDistr.entrySet()) {
			rhymeSubstrDistrBySegType = new HashMap<Integer, Distribution<String>>(20);
			rhymeSubstructureDistribution.put(entry.getKey(), rhymeSubstrDistrBySegType);
			// given a segment type, we now look at rhyme schemes of each length for that type
			for (Entry<Integer, Map<String, List<Integer>>> rhymeSchemesByLen : entry.getValue().entrySet()) {
				rhymeSubstrDistrBySegType.put(rhymeSchemesByLen.getKey(), new Distribution<String>(Utils.sortByListSize(rhymeSchemesByLen.getValue(), false)));
			}
		}

		linesPerSegmentDistribution = new EnumMap<SegmentType, Distribution<Integer>>(SegmentType.class);
		for (Entry<SegmentType, Map<Integer, List<Integer>>> entry : linesPerSegmentDistr.entrySet()) {
			linesPerSegmentDistribution.put(entry.getKey(), new Distribution<Integer>(Utils.sortByListSize(entry.getValue(), false)));
		}
		
		chordsPerLineDistribution = new EnumMap<SegmentType, Map<Integer, Distribution<Integer>>>(SegmentType.class);
		HashMap<Integer, Distribution<Integer>> measuresPerLineDistrBySegType;
		for (Entry<SegmentType, Map<Integer, Map<Integer, List<Integer>>>> entry : measuresPerLineDistr.entrySet()) {
			measuresPerLineDistrBySegType = new HashMap<Integer, Distribution<Integer>>(20);
			chordsPerLineDistribution.put(entry.getKey(), measuresPerLineDistrBySegType);
			// given a segment type, we now look at rhyme schemes of each length for that type
			for (Entry<Integer, Map<Integer, List<Integer>>> measuresPerLineBySegmentLen : entry.getValue().entrySet()) {
				measuresPerLineDistrBySegType.put(measuresPerLineBySegmentLen.getKey(), new Distribution<Integer>(Utils.sortByListSize(measuresPerLineBySegmentLen.getValue(), false)));
			}
		}

	}

	private static <T> Map<T, List<Integer>> getMapForKey(Map<Integer, Map<T, List<Integer>>> map, int key) {
		Map<T, List<Integer>> subMapForKey = map.get(key);
		if (subMapForKey == null) {
			subMapForKey = new HashMap<T, List<Integer>>();
			map.put(key, subMapForKey);
		}
		return subMapForKey;
	}


	private static <T> void incrementCount(Map<T, List<Integer>> gStructureDistr, T gStructure, int id) {
		List<Integer> list = gStructureDistr.get(gStructure);
		if (list == null) {
			list = new ArrayList<Integer>();
			gStructureDistr.put(gStructure, list);
		} 
		list.add(id);
	}

	public static Distribution<String> getGlobalStructureDistribution() {
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

	public static Map<SegmentType, Map<Integer, Distribution<String>>> getRhymingSubstructureDistribution() {
		return rhymeSubstructureDistribution;
	}

	public static Map<SegmentType, Distribution<Integer>> getLinesPerSegmentDistribution() {
		return linesPerSegmentDistribution;
	}

	public static Map<SegmentType, Map<Integer, Distribution<Integer>>> getChordsPerLineDistribution() {
		return chordsPerLineDistribution;
	}

	public static Distribution<Integer> getKeyDistribution() {
		return keyDistribution;
	}
}
