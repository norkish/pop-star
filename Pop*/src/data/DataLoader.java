package data;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;

import condition.Rhyme;
import constraint.Constraint;
import constraint.ConstraintBlock;
import globalstructure.SegmentType;
import harmony.Chord;
import lyrics.Lyric;
import main.TabDriver;
import markov.SparseSingleOrderMarkovModel;
import tab.CompletedTab;
import utils.Utils;

public class DataLoader {

	private static Distribution<Integer> keyDistribution;
	private static Distribution<String> gStructDistribution;

	// We have a distribution of rhyme schemes conditioned on SegmentType and number of lines per segment (remember, some segments don't have rhyme schemes) 
	// We need a distribution of subrhyme schemes conditioned on rhyme schemes (could also be conditioned on SegType)
	private static Map<SegmentType, Map<Integer, Distribution<ConstraintBlock<Lyric>>>> rhymeConstraintsDistribution;
	
	// We have a distribution of the number of lines per segment conditioned on the SegmentType
	private static Map<SegmentType, Distribution<Integer>> linesPerSegmentDistribution;
	
	// We need a distribution of the number of chords per line conditioned on segment type and lines per segment(and possibly conditioned on SegType)
	// We need a distribution of repetitive subsequences of chords per line conditioned on SegType and segment length
	private static Map<SegmentType, Map<Integer, Distribution<ConstraintBlock<Chord>>>> chordConstraintsDistribution;

	// We need a distribution of the number of words per line conditioned on subrhyme scheme and number of lines in the segment
	// We need a distribution of repetitive subsequences of lyrics per line conditioned on rhyme scheme and SegType
	private static Map<SegmentType, Map<Integer, Distribution<ConstraintBlock<Lyric>>>> lyricConstraintsDistribution;

	// We need a distribution of chord transitions, including intersegmental chord transitions, starting chords, ending chords, etc.
	private static Map<SegmentType, Map<Chord, Distribution<Chord>>> chordTransitionDistribution;
	
	static {
		loadDistribution();
	}

	private static void loadDistribution() {
		
		//Variable initialization
		Map<String, List<Integer>> gStructureDistr = new HashMap<String, List<Integer>>();
		Map<SegmentType, Map<Integer, Map<ConstraintBlock<Lyric>,List<Integer>>>> rhymeConstraintsDistr = new EnumMap<SegmentType, Map<Integer,Map<ConstraintBlock<Lyric>,List<Integer>>>>(SegmentType.class);
		Map<SegmentType, Map<Integer, Map<ConstraintBlock<Lyric>,List<Integer>>>> lyricConstraintsDistr = new EnumMap<SegmentType, Map<Integer,Map<ConstraintBlock<Lyric>,List<Integer>>>>(SegmentType.class);
		Map<SegmentType, Map<Integer, Map<ConstraintBlock<Chord>,List<Integer>>>> chordConstraintsDistr = new EnumMap<SegmentType, Map<Integer,Map<ConstraintBlock<Chord>,List<Integer>>>>(SegmentType.class);
		Map<SegmentType, Map<Integer,List<Integer>>> linesPerSegmentDistr = new EnumMap<SegmentType, Map<Integer, List<Integer>>>(SegmentType.class);
		Map<Integer, List<Integer>> keyDistr = new HashMap<Integer, List<Integer>>(); // use most common pitch names
		
		for (SegmentType type : SegmentType.values()) {
			linesPerSegmentDistr.put(type, new HashMap<Integer, List<Integer>>(30));

			if (type.hasLyrics()) { 
				rhymeConstraintsDistr.put(type, new HashMap<Integer, Map<ConstraintBlock<Lyric>, List<Integer>>>(30));
				lyricConstraintsDistr.put(type, new HashMap<Integer, Map<ConstraintBlock<Lyric>, List<Integer>>>(30));
			}
			chordConstraintsDistr.put(type, new HashMap<Integer, Map<ConstraintBlock<Chord>, List<Integer>>>(30));
		}
		
		List<CompletedTab> tabs = TabDriver.loadValidatedTabs();

		StringBuilder gStructBldr = null;
		ConstraintBlock<Lyric> rhymeConstraintBlock = null, lyricRepetitionConstraintBlock = null;
		ConstraintBlock<Chord> chordRepetitionConstraintBlock = null;
		List<Constraint<Lyric>> rhymeConstraintsForLine, lyricRepetitionConstraintsForLine;
		List<Constraint<Chord>> chordRepetitionConstraintsForLine;
		SegmentType currSegmentLabel, prevSegmentLabel = null;
		int idxOfSegmentBeginning;
		int i;
		int segmentLineIdx = 0;
		Map<ConstraintBlock<Lyric>, List<Integer>> rhymeSubstructDistrForTypeLen;
		char currSegmentLabelChar;
		int rhymeSchemeValue;
		List<SortedMap<Integer, Chord>> chords;
		List<String> words;
		CompletedTab completedTab;
		// End variable initialization

		for (int tabId = 0; tabId < tabs.size(); tabId++) {
			completedTab = tabs.get(tabId);
			if (completedTab.length() == 0) continue;
			
			chords = completedTab.chords;
			words = completedTab.words;
			incrementCount(keyDistr,completedTab.pitch, tabId);
			
			gStructBldr = new StringBuilder();
			prevSegmentLabel = null;
			
			idxOfSegmentBeginning = -1;
			for (i = 0; i < completedTab.length(); i++) {
				
				// extract structure from tab
				currSegmentLabelChar = completedTab.segmentLabelAt(i);
				currSegmentLabel = SegmentType.valueOf(currSegmentLabelChar);
				segmentLineIdx = i - idxOfSegmentBeginning;
				if (currSegmentLabel != prevSegmentLabel) {
					if (idxOfSegmentBeginning != -1) { // if not initializing the first block
						incrementCount(linesPerSegmentDistr.get(prevSegmentLabel), segmentLineIdx, tabId);
						if (prevSegmentLabel.hasLyrics()) {
							rhymeSubstructDistrForTypeLen = getMapForKey(rhymeConstraintsDistr.get(prevSegmentLabel),segmentLineIdx);
							incrementCount(rhymeSubstructDistrForTypeLen, rhymeConstraintBlock, tabId);
						}
					}
					rhymeConstraintBlock = new ConstraintBlock<Lyric>();
					lyricRepetitionConstraintBlock = new ConstraintBlock<Lyric>();
					chordRepetitionConstraintBlock = new ConstraintBlock<Chord>();
					
					idxOfSegmentBeginning = i;
					segmentLineIdx = 0;
					gStructBldr.append(currSegmentLabelChar);
					prevSegmentLabel = currSegmentLabel;
				}
				rhymeConstraintsForLine = new ArrayList<Constraint<Lyric>>();
				lyricRepetitionConstraintsForLine = new ArrayList<Constraint<Lyric>>();
				chordRepetitionConstraintsForLine = new ArrayList<Constraint<Chord>>();
				
				rhymeSchemeValue = completedTab.rhymeSchemeAt(i);
				// TODO: allow rhyming to extend beyond segment beginning.
//				if (rhymeSchemeValue != 0) {
				if (rhymeSchemeValue != 0 && segmentLineIdx - rhymeSchemeValue >= 0) {
					rhymeConstraintsForLine.add(new Constraint<Lyric>(Constraint.FINAL_POSITION, new Rhyme<Lyric>(segmentLineIdx - rhymeSchemeValue, Constraint.FINAL_POSITION), true));
				}

				// analyze chord line
				
				// analyze lyric line
				
				rhymeConstraintBlock.addLineConstraints(rhymeConstraintsForLine);
				lyricRepetitionConstraintBlock.addLineConstraints(lyricRepetitionConstraintsForLine);
				lyricRepetitionConstraintBlock.addLengthConstraint(words.size());
				chordRepetitionConstraintBlock.addLineConstraints(chordRepetitionConstraintsForLine);
				chordRepetitionConstraintBlock.addLengthConstraint(chords.size());
			}
			
			// include the last segment stats
			segmentLineIdx = i - idxOfSegmentBeginning;
			incrementCount(linesPerSegmentDistr.get(prevSegmentLabel), segmentLineIdx, tabId);
			if (prevSegmentLabel != SegmentType.OUTRO && prevSegmentLabel != SegmentType.INTRO) {
				rhymeSubstructDistrForTypeLen = getMapForKey(rhymeConstraintsDistr.get(prevSegmentLabel),segmentLineIdx);
				incrementCount(rhymeSubstructDistrForTypeLen, rhymeConstraintBlock, tabId);
			}

			// insert structure into distribution
			incrementCount(gStructureDistr, gStructBldr.toString(), tabId);
		}
		
		keyDistribution = new Distribution<Integer>(keyDistr);
		
		gStructDistribution = new Distribution<String>(Utils.sortByListSize(gStructureDistr, false));
		
		linesPerSegmentDistribution = new EnumMap<SegmentType, Distribution<Integer>>(SegmentType.class);
		for (Entry<SegmentType, Map<Integer, List<Integer>>> entry : linesPerSegmentDistr.entrySet()) {
			linesPerSegmentDistribution.put(entry.getKey(), new Distribution<Integer>(Utils.sortByListSize(entry.getValue(), false)));
		}

		rhymeConstraintsDistribution = createConditionalDistribution(rhymeConstraintsDistr);
		lyricConstraintsDistribution = createConditionalDistribution(lyricConstraintsDistr);
		chordConstraintsDistribution = createConditionalDistribution(chordConstraintsDistr);
	}

	/**
	 * @param rhymeSubstructDistr
	 * @return 
	 */
	private static <T> Map<SegmentType, Map<Integer, Distribution<ConstraintBlock<T>>>> createConditionalDistribution(
			Map<SegmentType, Map<Integer, Map<ConstraintBlock<T>, List<Integer>>>> rhymeSubstructDistr) {
		Map<SegmentType, Map<Integer, Distribution<ConstraintBlock<T>>>> constraintsDistribution = new EnumMap<SegmentType, Map<Integer, Distribution<ConstraintBlock<T>>>>(SegmentType.class);
		HashMap<Integer, Distribution<ConstraintBlock<T>>> rhymeSubstrDistrBySegType;
		for (Entry<SegmentType, Map<Integer, Map<ConstraintBlock<T>, List<Integer>>>> entry : rhymeSubstructDistr.entrySet()) {
			rhymeSubstrDistrBySegType = new HashMap<Integer, Distribution<ConstraintBlock<T>>>(20);
			constraintsDistribution.put(entry.getKey(), rhymeSubstrDistrBySegType);
			// given a segment type, we now look at rhyme schemes of each length for that type
			for (Entry<Integer, Map<ConstraintBlock<T>, List<Integer>>> rhymeSchemesByLen : entry.getValue().entrySet()) {
				rhymeSubstrDistrBySegType.put(rhymeSchemesByLen.getKey(), new Distribution<ConstraintBlock<T>>(rhymeSchemesByLen.getValue()));
			}
		}
		return constraintsDistribution;
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

	public static Map<SegmentType, Map<Integer, Distribution<ConstraintBlock<Lyric>>>> getRhymeConstraintsDistribution() {
		return rhymeConstraintsDistribution;
	}

	public static Map<SegmentType, Distribution<Integer>> getLinesPerSegmentDistribution() {
		return linesPerSegmentDistribution;
	}

	public static Distribution<Integer> getKeyDistribution() {
		return keyDistribution;
	}

	public static Map<SegmentType, Map<Integer, Distribution<ConstraintBlock<Lyric>>>> getLyricConstraintsDistribution() {
		return lyricConstraintsDistribution;
	}

	public static Map<SegmentType, Map<Integer, Distribution<ConstraintBlock<Chord>>>> getChordConstraintsDistribution() {
		return chordConstraintsDistribution;
	}

	public static SparseSingleOrderMarkovModel<Chord> getChordMarkovModel() {
		return null;//chordTransitionDistribution;
	}
}
