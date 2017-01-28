package data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;

import data.MusicXMLParser.Harmony;
import data.MusicXMLParser.Key;
import data.MusicXMLParser.Note;
import data.MusicXMLParser.NoteLyric;
import data.MusicXMLParser.Time;
import globalstructure.SegmentType;
import tabcomplete.rhyme.StressedPhone;
import utils.Pair;
import utils.Triple;

public class ParsedMusicXMLObject {

	public boolean followRepeats;
	public int lyricCount;

	// this is a list of the measure numbers in the order they are played, thus if there are repeats, sequences of measure numbers will be reinserted
	public List<Integer> playedToAbsoluteMeasureNumberMap = new ArrayList<Integer>();
	public List<SortedSet<Integer>> absoluteToPlayedMeasureNumbersMap = new ArrayList<SortedSet<Integer>>();
	
	// this represents the total number of notes that had text associated with them
	public int totalSyllables;

	// this represents the total number of notes that had text associated with them which could be associated with an entry in the cmu english dict
	public int totalSyllablesWithStressFromEnglishDictionary;
	public SortedMap<Integer, Time> timeByAbsoluteMeasure;
	public SortedMap<Integer, Key> normalizedKeyByAbsoluteMeasure;
	
	//measure, offset in divs, note
	public List<Triple<Integer, Integer, Note>> notesByPlayedMeasure;
	//measure, offset in divs, 
	public List<Triple<Integer, Integer, Harmony>> unoverlappingHarmonyByPlayedMeasure;
	
	// these are just for the purposes of error-reporting
	public List<String> lyricsWithoutStress = new ArrayList<String>();
	public List<NoteLyric> syllablesNotLookedUp = new ArrayList<NoteLyric>();
	public List<Pair<List<NoteLyric>, List<Triple<String, StressedPhone[], Integer>>>> lyricsWithDifferentSyllableCountThanAssociatedNotes 
		= new ArrayList<Pair<List<NoteLyric>, List<Triple<String, StressedPhone[], Integer>>>>();
	
	public int noteCount = -1; // needs to be set
	public SortedMap<Integer, Integer> divsPerQuarterByAbsoluteMeasure;
	public SortedMap<Integer, SegmentType> globalStructure;
	
	public ParsedMusicXMLObject(boolean followRepeats) {
		this.followRepeats = followRepeats;
	}

	@Override
	public String toString() {
		final int maxLen = 10;
		StringBuilder builder = new StringBuilder();
		builder.append("ParsedMusicXMLObject [\nfollowRepeats=").append(followRepeats).append(",\n lyricCount=")
				.append(lyricCount).append(",\n totalSyllables=").append(totalSyllables)
				.append(",\n totalSyllablesWithStressFromEnglishDictionary=")
				.append(totalSyllablesWithStressFromEnglishDictionary).append(",\n timeByMeasure=")
				.append(timeByAbsoluteMeasure != null ? toString(timeByAbsoluteMeasure.entrySet(), maxLen) : null)
				.append(",\n keyByMeasure=")
				.append(normalizedKeyByAbsoluteMeasure != null ? toString(normalizedKeyByAbsoluteMeasure.entrySet(), maxLen) : null)
				.append(",\n notesByMeasure=").append(notesByPlayedMeasure != null ? toString(notesByPlayedMeasure, maxLen) : null)
				.append(",\n lyricsWithoutStress=")
				.append(lyricsWithoutStress != null ? toString(lyricsWithoutStress, maxLen) : null)
				.append(",\n syllablesNotLookedUp=")
				.append(syllablesNotLookedUp != null ? toString(syllablesNotLookedUp, maxLen) : null)
				.append(",\n lyricsWithDifferentSyllableCountThanAssociatedNotes=")
				.append(lyricsWithDifferentSyllableCountThanAssociatedNotes != null
						? toString(lyricsWithDifferentSyllableCountThanAssociatedNotes, maxLen) : null)
				.append("\n]");
		return builder.toString();
	}

	private String toString(Collection<?> collection, int maxLen) {
		StringBuilder builder = new StringBuilder();
		builder.append("[");
		int i = 0;
		for (Iterator<?> iterator = collection.iterator(); iterator.hasNext() && i < maxLen; i++) {
			if (i > 0)
				builder.append(", ");
			builder.append(iterator.next());
		}
		builder.append("]");
		return builder.toString();
	}

	private int measureCount = 0; 
	public int getMeasureCount() {
		if (measureCount == 0) {
			recalculateMeasureCount();
		}
		return measureCount;
	}

	private void recalculateMeasureCount() {
		measureCount = 0;
		if (!notesByPlayedMeasure.isEmpty()) {
			measureCount = notesByPlayedMeasure.get(notesByPlayedMeasure.size()-1).getFirst() + 1;
		}
		if (!unoverlappingHarmonyByPlayedMeasure.isEmpty()) {
			measureCount = Math.max(measureCount, unoverlappingHarmonyByPlayedMeasure.get(unoverlappingHarmonyByPlayedMeasure.size()-1).getFirst() + 1);
		}
	}
}
