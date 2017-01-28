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
import markov.SparseSingleOrderMarkovModel;
import tabcomplete.main.TabDriver;
import tabcomplete.tab.CompletedTab;
import utils.Utils;

public class DataLoader {

	private static BackedDistribution<Integer> keyDistribution;
	private static BackedDistribution<String> gStructDistribution;

	// We have a distribution of rhyme schemes conditioned on SegmentType and number of lines per segment (remember, some segments don't have rhyme schemes) 
	// We need a distribution of subrhyme schemes conditioned on rhyme schemes (could also be conditioned on SegType)
	private static Map<SegmentType, Map<Integer, BackedDistribution<ConstraintBlock<Lyric>>>> rhymeConstraintsDistribution;
	
	// We have a distribution of the number of lines per segment conditioned on the SegmentType
	private static Map<SegmentType, BackedDistribution<Integer>> linesPerSegmentDistribution;
	
	// We need a distribution of the number of chords per line conditioned on segment type and lines per segment(and possibly conditioned on SegType)
	// We need a distribution of repetitive subsequences of chords per line conditioned on SegType and segment length
	private static Map<SegmentType, Map<Integer, BackedDistribution<ConstraintBlock<Chord>>>> chordConstraintsDistribution;

	// We need a distribution of the number of words per line conditioned on subrhyme scheme and number of lines in the segment
	// We need a distribution of repetitive subsequences of lyrics per line conditioned on rhyme scheme and SegType
	private static Map<SegmentType, Map<Integer, BackedDistribution<ConstraintBlock<Lyric>>>> lyricConstraintsDistribution;

	// We need a distribution of chord transitions, including intersegmental chord transitions, starting chords, ending chords, etc.
	private static Map<SegmentType, SparseSingleOrderMarkovModel<Chord>> chordTransitionDistribution;
	
	// We need a distribution of word transitions, including intersegmental word transitions, starting chords, ending chords, etc.
	private static Map<SegmentType, SparseSingleOrderMarkovModel<Lyric>> wordTransitionDistribution;
	
	static {
		loadDistribution();
	}

	private static void loadDistribution() {
		
		//Variable initialization
//		Map<Integer, List<Integer>> keyDistr = new HashMap<Integer, List<Integer>>(); // use most common pitch names
		Map<String, List<Integer>> gStructureDistr = new HashMap<String, List<Integer>>();
		Map<SegmentType, Map<Integer,List<Integer>>> linesPerSegmentDistr = new EnumMap<SegmentType, Map<Integer, List<Integer>>>(SegmentType.class);
		Map<SegmentType, Map<Integer, Map<ConstraintBlock<Lyric>,List<Integer>>>> rhymeConstraintsDistr = new EnumMap<SegmentType, Map<Integer,Map<ConstraintBlock<Lyric>,List<Integer>>>>(SegmentType.class);
		Map<SegmentType, Map<Integer, Map<ConstraintBlock<Lyric>,List<Integer>>>> lyricConstraintsDistr = new EnumMap<SegmentType, Map<Integer,Map<ConstraintBlock<Lyric>,List<Integer>>>>(SegmentType.class);
		Map<SegmentType, Map<Integer, Map<ConstraintBlock<Chord>,List<Integer>>>> chordConstraintsDistr = new EnumMap<SegmentType, Map<Integer,Map<ConstraintBlock<Chord>,List<Integer>>>>(SegmentType.class);
		Map<SegmentType, Map<Chord,Map<Chord,Integer>>> chordTransitions = new EnumMap<SegmentType,Map<Chord,Map<Chord,Integer>>>(SegmentType.class); 
		Map<SegmentType, Map<Lyric,Map<Lyric,Integer>>> wordTransitions = new EnumMap<SegmentType,Map<Lyric,Map<Lyric,Integer>>>(SegmentType.class); 
		
		for (SegmentType type : SegmentType.values()) {
			linesPerSegmentDistr.put(type, new HashMap<Integer, List<Integer>>(30));

			if (type.hasLyrics()) { 
				rhymeConstraintsDistr.put(type, new HashMap<Integer, Map<ConstraintBlock<Lyric>, List<Integer>>>(30));
				lyricConstraintsDistr.put(type, new HashMap<Integer, Map<ConstraintBlock<Lyric>, List<Integer>>>(30));
				wordTransitions.put(type, new HashMap<Lyric, Map<Lyric, Integer>>());
			}
			chordConstraintsDistr.put(type, new HashMap<Integer, Map<ConstraintBlock<Chord>, List<Integer>>>(30));
			chordTransitions.put(type, new HashMap<Chord, Map<Chord, Integer>>());
		}
		
		List<CompletedTab> tabs = TabDriver.loadValidatedTabs();

		StringBuilder gStructBldr = null;
		ConstraintBlock<Lyric> rhymeConstraintBlock = null, lyricRepetitionConstraintBlock = null;
		ConstraintBlock<Chord> chordRepetitionConstraintBlock = null;
		List<Constraint<Lyric>> rhymeConstraintsForLine, lyricRepetitionConstraintsForLine;
		List<Constraint<Chord>> chordRepetitionConstraintsForLine;
		Map<Lyric, Map<Lyric, Integer>> wordTransitionsForCurrSegment = null;
		Map<Chord, Map<Chord, Integer>> chordTransitionsForCurrSegment = null;
		SegmentType currSegmentLabel, prevSegmentLabel = null;
		int idxOfSegmentBeginning;
		int i;
		int segmentLineIdx = 0;
		char currSegmentLabelChar;
		int rhymeSchemeValue;
		List<SortedMap<Integer, Chord>> chords;
		List<String> words;
		CompletedTab completedTab;
		Chord prevChord = null;
		Map<Chord, Integer> chordDistrForPrevChord;
		Lyric prevLyric = null;
		Map<Lyric, Integer> wordDistrForPrevWord;
		SortedMap<Integer, Chord> chordsForLine;
		String wordsForLine;
		List<Lyric> parsedLyrics;
		// End variable initialization

		// For each tab
		for (int tabId = 0; tabId < tabs.size(); tabId++) {
			completedTab = tabs.get(tabId);
			if (completedTab.length() == 0) continue;
			
			chords = completedTab.chords;
			words = completedTab.words;
//			incrementCount(keyDistr,completedTab.key, tabId);
			
			gStructBldr = new StringBuilder();
			prevSegmentLabel = null;
			
			idxOfSegmentBeginning = -1;
			// For each line in the tab
			for (i = 0; i < completedTab.length(); i++) {
				
				chordsForLine = chords.get(i);
				wordsForLine = words.get(i);
				
				// extract structure from tab
				currSegmentLabelChar = completedTab.segmentLabelAt(i);
				currSegmentLabel = SegmentType.valueOf(currSegmentLabelChar);
				segmentLineIdx = i - idxOfSegmentBeginning;
				if (currSegmentLabel != prevSegmentLabel) { // new segment beginning
					if (idxOfSegmentBeginning != -1) { // if not initializing the first block, process previous block
						processPreviousSegment(linesPerSegmentDistr, rhymeConstraintsDistr, lyricConstraintsDistr,
								chordConstraintsDistr, rhymeConstraintBlock, lyricRepetitionConstraintBlock,
								chordRepetitionConstraintBlock, wordTransitionsForCurrSegment,
								chordTransitionsForCurrSegment, prevSegmentLabel, segmentLineIdx, prevChord, prevLyric,
								tabId);
					}
					
					// get set up for next block
					if (currSegmentLabel.hasLyrics()) {
						rhymeConstraintBlock = new ConstraintBlock<Lyric>();
						lyricRepetitionConstraintBlock = new ConstraintBlock<Lyric>();
						wordTransitionsForCurrSegment = wordTransitions.get(currSegmentLabel);
					}
					chordRepetitionConstraintBlock = new ConstraintBlock<Chord>();
					chordTransitionsForCurrSegment = chordTransitions.get(currSegmentLabel);
					
					idxOfSegmentBeginning = i;
					segmentLineIdx = 0;
					gStructBldr.append(currSegmentLabelChar);
					prevSegmentLabel = currSegmentLabel;
					prevChord = null; // ensures first transition will be from the "SEGMENT_START token"
					prevLyric = null; // ensures first transition will be from the "SEGMENT_START token"
				}
				rhymeConstraintsForLine = new ArrayList<Constraint<Lyric>>();
				lyricRepetitionConstraintsForLine = new ArrayList<Constraint<Lyric>>();
				chordRepetitionConstraintsForLine = new ArrayList<Constraint<Chord>>();
				
				rhymeSchemeValue = completedTab.rhymeSchemeAt(i);
				// TODO: allow rhyme scheme to extend beyond segment beginning.
//				if (rhymeSchemeValue != 0) {
				if (rhymeSchemeValue != 0 && segmentLineIdx - rhymeSchemeValue >= 0) {
					rhymeConstraintsForLine.add(new Constraint<Lyric>(Constraint.FINAL_POSITION, new Rhyme<Lyric>(segmentLineIdx - rhymeSchemeValue, Constraint.FINAL_POSITION), true));
				}

				// analyze chord line for transitions
				for (Chord nextChord : chordsForLine.values()) {
					chordDistrForPrevChord = getMapForKey(chordTransitionsForCurrSegment, prevChord);
					incrementCount(chordDistrForPrevChord, nextChord);
					prevChord = nextChord;
				}
				
				// analyze chord line for constraints
				//TODO
				
				if (currSegmentLabel.hasLyrics()) {
					// analyze lyric line for transitions
					parsedLyrics = Lyric.parseLyrics(wordsForLine);
					for (Lyric nextWord: parsedLyrics) {
						wordDistrForPrevWord = getMapForKey(wordTransitionsForCurrSegment, prevLyric);
						incrementCount(wordDistrForPrevWord, nextWord);
						prevLyric = nextWord;
					}
					
					// analyze lyric line for constraints
					//TODO
					rhymeConstraintBlock.addLineConstraints(rhymeConstraintsForLine);
					lyricRepetitionConstraintBlock.addLineConstraints(lyricRepetitionConstraintsForLine);
					lyricRepetitionConstraintBlock.addLengthConstraint(parsedLyrics.size());
				}
				
				chordRepetitionConstraintBlock.addLineConstraints(chordRepetitionConstraintsForLine);
				chordRepetitionConstraintBlock.addLengthConstraint(chordsForLine.size());
			}
			
			// include the last segment stats
			segmentLineIdx = i - idxOfSegmentBeginning;

			processPreviousSegment(linesPerSegmentDistr, rhymeConstraintsDistr, lyricConstraintsDistr,
					chordConstraintsDistr, rhymeConstraintBlock, lyricRepetitionConstraintBlock,
					chordRepetitionConstraintBlock, wordTransitionsForCurrSegment,
					chordTransitionsForCurrSegment, prevSegmentLabel, segmentLineIdx, prevChord, prevLyric,
					tabId);

			// insert structure into distribution
			incrementCount(gStructureDistr, gStructBldr.toString(), tabId);
		}
		
//		keyDistribution = new BackedDistribution<Integer>(keyDistr);
		
		gStructDistribution = new BackedDistribution<String>(Utils.sortByListSize(gStructureDistr, false));
		
		linesPerSegmentDistribution = new EnumMap<SegmentType, BackedDistribution<Integer>>(SegmentType.class);
		for (Entry<SegmentType, Map<Integer, List<Integer>>> entry : linesPerSegmentDistr.entrySet()) {
			linesPerSegmentDistribution.put(entry.getKey(), new BackedDistribution<Integer>(Utils.sortByListSize(entry.getValue(), false)));
		}

		rhymeConstraintsDistribution = createConditionalDistribution(rhymeConstraintsDistr);
		lyricConstraintsDistribution = createConditionalDistribution(lyricConstraintsDistr);
		chordConstraintsDistribution = createConditionalDistribution(chordConstraintsDistr);
		chordTransitionDistribution = createConditionalMarkovModel(chordTransitions);
		wordTransitionDistribution = createConditionalMarkovModel(wordTransitions);
	}

	private static <T> Map<SegmentType, SparseSingleOrderMarkovModel<T>> createConditionalMarkovModel(
			Map<SegmentType, Map<T, Map<T, Integer>>> transitionMatricesBySegment) {
		Map<SegmentType, SparseSingleOrderMarkovModel<T>> conditionalMarkovModel = new EnumMap<SegmentType, SparseSingleOrderMarkovModel<T>>(SegmentType.class);
		
		SparseSingleOrderMarkovModel<T> model;
		
		T fromToken, toToken;
		Map<T, Integer> countsByToToken;
		Map<T, Map<T, Integer>> chordTransitionMapsForSegment;
		Map<Integer, Double> transitionsFromFromToken;
		double totalForFromToken;
		int tokenSetSizeEstimate;
		for (Entry<SegmentType, Map<T, Map<T, Integer>>> entry : transitionMatricesBySegment.entrySet()) {
			chordTransitionMapsForSegment = entry.getValue();
			if (chordTransitionMapsForSegment.size() == 0) continue; // no data for this segment type
			
			tokenSetSizeEstimate = chordTransitionMapsForSegment.size()+10;
			Map<T,Integer> statesByIndex = new HashMap<T,Integer>(tokenSetSizeEstimate);
			Map<Integer,Double> priors = new HashMap<Integer, Double>();
			Map<Integer, Map<Integer, Double>> transitions = new HashMap<Integer, Map<Integer, Double>>(tokenSetSizeEstimate);
			
			// first priors
			countsByToToken = chordTransitionMapsForSegment.remove(null);
			totalForFromToken = 0.;
			for (Entry<T,Integer> countsForToToken : countsByToToken.entrySet()) {
				if (countsForToToken.getKey() == null) continue; // can't have end right after beginning, can we? 
				totalForFromToken += countsForToToken.getValue();
			}
			for (Entry<T,Integer> countsForToToken : countsByToToken.entrySet()) {
				toToken = countsForToToken.getKey();
				if (toToken == null) continue; // can't have end right after beginning, can we?
				statesByIndex.put(toToken, statesByIndex.size());
				priors.put(statesByIndex.get(toToken), countsForToToken.getValue()/totalForFromToken);
			}
			
			// now transitions
			for (Entry<T,Map<T,Integer>> chordTransitionMap : chordTransitionMapsForSegment.entrySet()) {
				fromToken = chordTransitionMap.getKey();
				countsByToToken = chordTransitionMap.getValue();
				
				if (!statesByIndex.containsKey(fromToken)) {
					statesByIndex.put(fromToken, statesByIndex.size());
				}
				transitionsFromFromToken = new HashMap<Integer, Double>(tokenSetSizeEstimate);
				transitions.put(statesByIndex.get(fromToken), transitionsFromFromToken);
				
				totalForFromToken = 0.;
				for (Entry<T,Integer> countsForToToken : countsByToToken.entrySet()) {
					if (countsForToToken.getKey() == null) continue; // TODO: figure out how to incorporate transitions to stop tokens
					totalForFromToken += countsForToToken.getValue();
				}
				for (Entry<T,Integer> countsForToToken : countsByToToken.entrySet()) {
					toToken = countsForToToken.getKey();
					if (toToken == null) continue; // TODO: figure out how to incorporate transitions to stop tokens
					if (!statesByIndex.containsKey(toToken)) {
						statesByIndex.put(toToken, statesByIndex.size());
					}
					transitionsFromFromToken.put(statesByIndex.get(toToken), countsForToToken.getValue()/totalForFromToken);
				}
			}

			model = new SparseSingleOrderMarkovModel<T>(statesByIndex, priors, transitions);
			conditionalMarkovModel.put(entry.getKey(), model);
		}
		
		return conditionalMarkovModel;
	}

	/**
	 * @param linesPerSegmentDistr
	 * @param rhymeConstraintsDistr
	 * @param lyricConstraintsDistr
	 * @param chordConstraintsDistr
	 * @param rhymeConstraintBlock
	 * @param lyricRepetitionConstraintBlock
	 * @param chordRepetitionConstraintBlock
	 * @param wordTransitionsForCurrSegment
	 * @param chordTransitionsForCurrSegment
	 * @param prevSegmentLabel
	 * @param segmentLineIdx
	 * @param prevChord
	 * @param prevLyric
	 * @param tabId
	 */
	private static void processPreviousSegment(Map<SegmentType, Map<Integer, List<Integer>>> linesPerSegmentDistr,
			Map<SegmentType, Map<Integer, Map<ConstraintBlock<Lyric>, List<Integer>>>> rhymeConstraintsDistr,
			Map<SegmentType, Map<Integer, Map<ConstraintBlock<Lyric>, List<Integer>>>> lyricConstraintsDistr,
			Map<SegmentType, Map<Integer, Map<ConstraintBlock<Chord>, List<Integer>>>> chordConstraintsDistr,
			ConstraintBlock<Lyric> rhymeConstraintBlock, ConstraintBlock<Lyric> lyricRepetitionConstraintBlock,
			ConstraintBlock<Chord> chordRepetitionConstraintBlock,
			Map<Lyric, Map<Lyric, Integer>> wordTransitionsForCurrSegment,
			Map<Chord, Map<Chord, Integer>> chordTransitionsForCurrSegment, SegmentType prevSegmentLabel,
			Integer segmentLineIdx, Chord prevChord, Lyric prevLyric, Integer tabId) {
		
		incrementCount(linesPerSegmentDistr.get(prevSegmentLabel), segmentLineIdx, tabId);
		if (prevSegmentLabel.hasLyrics()) {
			Map<ConstraintBlock<Lyric>, List<Integer>> rhymeSubstructDistrForTypeLen = getMapForKey(rhymeConstraintsDistr.get(prevSegmentLabel),segmentLineIdx);
			incrementCount(rhymeSubstructDistrForTypeLen, rhymeConstraintBlock, tabId);
			Map<ConstraintBlock<Lyric>, List<Integer>> lyricConstraintsDistrForTypeLen = getMapForKey(lyricConstraintsDistr.get(prevSegmentLabel),segmentLineIdx);
			incrementCount(lyricConstraintsDistrForTypeLen, lyricRepetitionConstraintBlock, tabId);
			Map<Lyric, Integer> wordDistrForPrevWord = getMapForKey(wordTransitionsForCurrSegment, prevLyric);
			incrementCount(wordDistrForPrevWord, null); // add transition to the "SEGMENT_END token"
		}
		Map<ConstraintBlock<Chord>, List<Integer>> chordConstraintsDistrForTypeLen = getMapForKey(chordConstraintsDistr.get(prevSegmentLabel),segmentLineIdx);
		incrementCount(chordConstraintsDistrForTypeLen, chordRepetitionConstraintBlock, tabId);
		Map<Chord, Integer> chordDistrForPrevChord = getMapForKey(chordTransitionsForCurrSegment, prevChord);
		incrementCount(chordDistrForPrevChord, null); // add transition to the "SEGMENT_END token"
		
	}

	/**
	 * @param rhymeSubstructDistr
	 * @return 
	 */
	private static <T> Map<SegmentType, Map<Integer, BackedDistribution<ConstraintBlock<T>>>> createConditionalDistribution(
			Map<SegmentType, Map<Integer, Map<ConstraintBlock<T>, List<Integer>>>> rhymeSubstructDistr) {
		Map<SegmentType, Map<Integer, BackedDistribution<ConstraintBlock<T>>>> constraintsDistribution = new EnumMap<SegmentType, Map<Integer, BackedDistribution<ConstraintBlock<T>>>>(SegmentType.class);
		HashMap<Integer, BackedDistribution<ConstraintBlock<T>>> rhymeSubstrDistrBySegType;
		for (Entry<SegmentType, Map<Integer, Map<ConstraintBlock<T>, List<Integer>>>> entry : rhymeSubstructDistr.entrySet()) {
			rhymeSubstrDistrBySegType = new HashMap<Integer, BackedDistribution<ConstraintBlock<T>>>(20);
			constraintsDistribution.put(entry.getKey(), rhymeSubstrDistrBySegType);
			// given a segment type, we now look at rhyme schemes of each length for that type
			for (Entry<Integer, Map<ConstraintBlock<T>, List<Integer>>> rhymeSchemesByLen : entry.getValue().entrySet()) {
				rhymeSubstrDistrBySegType.put(rhymeSchemesByLen.getKey(), new BackedDistribution<ConstraintBlock<T>>(rhymeSchemesByLen.getValue()));
			}
		}
		return constraintsDistribution;
	}

	private static <T,S,K> Map<T, S> getMapForKey(Map<K, Map<T, S>> map, K key) {
		Map<T, S> subMapForKey = map.get(key);
		if (subMapForKey == null) {
			subMapForKey = new HashMap<T, S>();
			map.put(key, subMapForKey);
		}
		return subMapForKey;
	}

	private static <T> void incrementCount(Map<T, List<Integer>> map, T key, int id) {
		List<Integer> list = map.get(key);
		if (list == null) {
			list = new ArrayList<Integer>();
			map.put(key, list);
		} 
		list.add(id);
	}

	private static <T> void incrementCount(Map<T, Integer> map, T key) {
		Integer count = map.get(key);
		if (count == null) {
			map.put(key, 1);
		} else {
			map.put(key, count+1);
		}
	}
	
	public static BackedDistribution<String> getGlobalStructureDistribution() {
		return gStructDistribution;
	}
	
	public static void main(String[] args) {
		BackedDistribution<String> gStructDist = DataLoader.getGlobalStructureDistribution();
		Map<SegmentType, BackedDistribution<Integer>> linesPerSegmentDistribution = DataLoader.getLinesPerSegmentDistribution();

		System.out.println(gStructDist);
		System.out.println(linesPerSegmentDistribution);
		
		for (int i = 0; i < 10; i++) {
			System.out.println(gStructDist.sampleAccordingToDistribution());
		}
		for (int i = 0; i < 3; i++) {
			System.out.println(linesPerSegmentDistribution.get(SegmentType.INTRO).sampleAccordingToDistribution());
			System.out.println(linesPerSegmentDistribution.get(SegmentType.CHORUS).sampleAccordingToDistribution());
			System.out.println(linesPerSegmentDistribution.get(SegmentType.BRIDGE).sampleAccordingToDistribution());
			System.out.println(linesPerSegmentDistribution.get(SegmentType.OUTRO).sampleAccordingToDistribution());
			System.out.println(linesPerSegmentDistribution.get(SegmentType.VERSE).sampleAccordingToDistribution());
		}
	}

	public static Map<SegmentType, Map<Integer, BackedDistribution<ConstraintBlock<Lyric>>>> getRhymeConstraintsDistribution() {
		return rhymeConstraintsDistribution;
	}

	public static Map<SegmentType, BackedDistribution<Integer>> getLinesPerSegmentDistribution() {
		return linesPerSegmentDistribution;
	}

//	public static BackedDistribution<Integer> getKeyDistribution() {
//		return keyDistribution;
//	}

	public static Map<SegmentType, Map<Integer, BackedDistribution<ConstraintBlock<Lyric>>>> getLyricConstraintsDistribution() {
		return lyricConstraintsDistribution;
	}

	public static Map<SegmentType, Map<Integer, BackedDistribution<ConstraintBlock<Chord>>>> getChordConstraintsDistribution() {
		return chordConstraintsDistribution;
	}

	public static Map<SegmentType, SparseSingleOrderMarkovModel<Chord>> getChordMarkovModel() {
		return chordTransitionDistribution;
	}

	public static Map<SegmentType, SparseSingleOrderMarkovModel<Lyric>> getLyricMarkovModel() {
		return wordTransitionDistribution; 
	}
}
