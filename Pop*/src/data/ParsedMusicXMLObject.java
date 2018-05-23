package data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;

import constraint.Constraint;
import data.MusicXMLParser.Harmony;
import data.MusicXMLParser.Key;
import data.MusicXMLParser.Note;
import data.MusicXMLParser.NoteLyric;
import data.MusicXMLParser.Syllabic;
import data.MusicXMLParser.Time;
import globalstructure.SegmentType;
import tabcomplete.rhyme.StressedPhone;
import utils.Pair;
import utils.Triple;
import utils.Utils;

public class ParsedMusicXMLObject {

	public static class MusicXMLAlignmentEvent {
	
		@Override
		public String toString() {
			return note + ", " + noteOnset + ", " + harmony + ", " + harmonyOnset + ", " + lyric + ", " + lyricOnset
					+ ", " + beat + ", " + beatsToMeasureEnd + ", " + segmentType + ", " + measureOffsetInSegment;
		}

		public Note note;
		public double tiedDurationOfCurrentNote;
		public boolean noteOnset;
		public double currBeatsSinceOnset;
		public Harmony harmony;
		public boolean harmonyOnset;
		public NoteLyric lyric;
		public String strippedLyricLowerCaseText;
		public boolean lyricOnset;
		public int measure;
		public double beat;
		public double beatsToMeasureEnd;
		public SegmentType segmentType;
		private int measureOffsetInSegment;
		private Set<String> matchingLyricRegions;
		private Set<String> matchingPitchRegions;
		private Set<String> matchingRhythmRegions;
		private Set<String> matchingHarmonyRegions;
		private Set<String> matchingChorusRegions;
		private Set<String> matchingVerseRegions;
		private Set<String> rhymingRegions;
	
		public MusicXMLAlignmentEvent(Note note, boolean noteOnset, double currBeatsSinceOnset, Harmony harmony, boolean harmonyOnset,
				NoteLyric lyric, boolean lyricOnset, int currMeasure, double currBeat, double currBeatsToMeasureEnd, SegmentType segmentType, int measureOffsetInSegment, 
				Set<String> matchingLyricRegions, Set<String> matchingPitchRegions, 
				Set<String> matchingRhythmRegions, Set<String> matchingHarmonyRegions, 
				Set<String> matchingChorusRegions, Set<String> matchingVerseRegions,
				Set<String> rhymingRegions) {
			this.note = note;
			this.noteOnset = noteOnset;
			this.currBeatsSinceOnset = currBeatsSinceOnset;
			this.harmony = harmony;
			this.harmonyOnset = harmonyOnset;
			this.lyric = lyric;
			this.strippedLyricLowerCaseText = lyric == null? "":lyric.text.replaceAll("[^a-zA-Z ]", "").toLowerCase();
			this.lyricOnset = lyricOnset;
			this.measure = currMeasure;
			this.beat = currBeat;
			this.beatsToMeasureEnd = currBeatsToMeasureEnd;
			this.segmentType = segmentType;
			this.measureOffsetInSegment = measureOffsetInSegment;
			this.matchingLyricRegions = matchingLyricRegions;
			this.matchingPitchRegions = matchingPitchRegions;
			this.matchingRhythmRegions = matchingRhythmRegions;
			this.matchingHarmonyRegions = matchingHarmonyRegions;
			this.matchingChorusRegions = matchingChorusRegions;
			this.matchingVerseRegions = matchingVerseRegions;
			this.rhymingRegions = rhymingRegions;
		}

		public Set<String> getLyricGroups() {
			return matchingLyricRegions;
		}

		public Set<String> getPitchGroups() {
			return matchingPitchRegions;
		}

		public Set<String> getHarmonyGroups() {
			return matchingHarmonyRegions;
		}
		
		public Set<String> getRhythmGroups() {
			return matchingRhythmRegions;
		}
		
		public Set<String> getRhymeGroups() {
			return rhymingRegions;
		}

		public Set<String> getChorusGroups() {
			return matchingChorusRegions;
		}

		public Set<String> getVerseGroups() {
			return matchingVerseRegions;
		}
	}

	public String filename; 
	public boolean followRepeats;
	public int lyricCount;

	// this is a list of the measure numbers in the order they are played, thus if there are repeats, sequences of measure numbers will be reinserted
	public List<Integer> playedToAbsoluteMeasureNumberMap = new ArrayList<Integer>();
	public List<List<Integer>> absoluteToPlayedMeasureNumbersMap = new ArrayList<List<Integer>>();
		
	// this represents the total number of notes that had text associated with them
	public int totalSyllables;

	// this represents the total number of notes that had text associated with them which could be associated with an entry in the cmu english dict
	public int totalSyllablesWithStressFromEnglishDictionary;
	private SortedMap<Integer, Time> timeByAbsoluteMeasure;
	public SortedMap<Integer, Key> normalizedKeyByAbsoluteMeasure;
	
	//measure, offset in divs, note
	private List<Triple<Integer, Integer, Note>> notesByPlayedMeasure;
	//measure, offset in divs, 
	public List<Triple<Integer, Integer, Harmony>> unoverlappingHarmonyByPlayedMeasure;
	
	// these are just for the purposes of error-reporting
	public List<String> lyricsWithoutStress = new ArrayList<String>();
	public List<NoteLyric> syllablesNotLookedUp = new ArrayList<NoteLyric>();
	public List<Pair<List<NoteLyric>, List<Triple<String, StressedPhone[], StressedPhone>>>> lyricsWithDifferentSyllableCountThanAssociatedNotes 
		= new ArrayList<Pair<List<NoteLyric>, List<Triple<String, StressedPhone[], StressedPhone>>>>();
	
	public int noteCount = -1; // needs to be set
	private SortedMap<Integer, Integer> divsPerQuarterByAbsoluteMeasure;
	
	/**
	 * Key is measure where segment Form starts (ignoring pickups or delays), value contains 
	 * 1) segment type of the form and 2) the delta from the form start of the measure of the
	 * first actual note belonging to the form and and 3) offset within that measure 
	 */
	private SortedMap<Integer, Triple<SegmentType, Integer, Double>> globalStructureByFormStart;
	public SortedMap<Integer, SortedMap<Double, List<Constraint<NoteLyric>>>> segmentLyricStructure;
	public SortedMap<Integer, SortedMap<Double, List<Constraint<Integer>>>> segmentPitchStructure;
	public SortedMap<Integer, SortedMap<Double, List<Constraint<Double>>>> segmentRhythmStructure;
	public SortedMap<Integer, SortedMap<Double, List<Constraint<Harmony>>>> segmentHarmonyStructure;
	public SortedMap<Integer, Double> phraseBeginnings; 
	
	public ParsedMusicXMLObject(String filename, boolean followRepeats) {
		this.filename = filename;
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
	private SortedMap<Integer, SortedMap<Integer, Note>> notesByPlayedMeasureMap;
	private SortedMap<Integer, SortedMap<Double, SegmentType>> globalStructureBySegmentTokenStart;
	private int averageOctave = -1;
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

	public SortedMap<Integer, SortedMap<Integer, Harmony>> getUnoverlappingHarmonyByPlayedMeasureAsMap() {
		SortedMap<Integer, SortedMap<Integer, Harmony>> harmonyByMeasure = new TreeMap<Integer, SortedMap<Integer, Harmony>>();
		for (Triple<Integer,Integer,Harmony> triple : unoverlappingHarmonyByPlayedMeasure) {
			Integer measure = triple.getFirst();
			Integer divOffset = triple.getSecond();
			Harmony harmony = triple.getThird();
			SortedMap<Integer, Harmony> harmoniesByOffset = harmonyByMeasure.get(measure);
			if (harmoniesByOffset == null) {
				harmoniesByOffset = new TreeMap<Integer, Harmony>();
				harmonyByMeasure.put(measure, harmoniesByOffset);
			}
			
			// should be no overlapping harmonies, already handled in xml parser
			harmoniesByOffset.put(divOffset, harmony);
		}
		return harmonyByMeasure;
	}

	private SortedMap<Integer, SortedMap<Integer, Note>> generateNotesByPlayedMeasureAsMap() {
		SortedMap<Integer, SortedMap<Integer, Note>> notesByMeasure = new TreeMap<Integer, SortedMap<Integer, Note>>();
		for (Triple<Integer,Integer,Note> triple : notesByPlayedMeasure) {
			Integer measure = triple.getFirst();
			Integer divOffset = triple.getSecond();
			Note note = triple.getThird();
			SortedMap<Integer, Note> notesByOffset = notesByMeasure.get(measure);
			if (notesByOffset == null) {
				notesByOffset = new TreeMap<Integer, Note>();
				notesByMeasure.put(measure, notesByOffset);
			}
			
			// keep the highest note if there are multiple
			Note currNote = notesByOffset.get(divOffset);
			if (currNote == null || note.pitch > currNote.pitch) {
				notesByOffset.put(divOffset, note);
			}
		}
		return notesByMeasure;
	}

	public void setTimeByAbsoluteMeasure(SortedMap<Integer, Time> timeByAbsoluteMeasure) {
		this.timeByAbsoluteMeasure = timeByAbsoluteMeasure;
	}

	public Time getTimeForAbsoluteMeasure(int absoluteMeasure) {
		Time time = Utils.valueForKeyBeforeOrEqualTo(absoluteMeasure, timeByAbsoluteMeasure);

//		// We'll count anything in 2/2 as 4/4
//		if (time.equals(Time.TWO_TWO)) {
//			return Time.FOUR_FOUR;
//		}
		
		return time;
	}

	public void setDivsPerQuarterByAbsoluteMeasure(SortedMap<Integer, Integer> divsPerQuarterByAbsoluteMeasure) {
		this.divsPerQuarterByAbsoluteMeasure = divsPerQuarterByAbsoluteMeasure;
	}
	
	public double divsToBeats(int divsOffset, int playedMeasure) {
		int absoluteMeasure = playedToAbsoluteMeasureNumberMap.get(playedMeasure);
		return (divsOffset*1.0/Utils.valueForKeyBeforeOrEqualTo(absoluteMeasure, divsPerQuarterByAbsoluteMeasure)) * (getTimeForAbsoluteMeasure(absoluteMeasure).beatType/4.0);
	}

	public Integer beatsToDivs(Double beatsOffset, Integer playedMeasure) {
		int absoluteMeasure = playedToAbsoluteMeasureNumberMap.get(playedMeasure);
		return (int) ((beatsOffset / (getTimeForAbsoluteMeasure(absoluteMeasure).beatType/4.0)) * Utils.valueForKeyBeforeOrEqualTo(absoluteMeasure, divsPerQuarterByAbsoluteMeasure));
	}
	
	public double getDivsPerQuarterForAbsoluteMeasure(int absoluteMeasureNumber) {
		return (double) Utils.valueForKeyBeforeOrEqualTo(absoluteMeasureNumber, divsPerQuarterByAbsoluteMeasure);
	}

	public SortedMap<Integer, SortedMap<Integer, Note>> getNotesByPlayedMeasureAsMap() {
		return notesByPlayedMeasureMap;
	}
	
	public void setNotesByPlayedMeasure(List<Triple<Integer, Integer, Note>> notesByPlayedMeasure) {
		this.notesByPlayedMeasure = notesByPlayedMeasure;
		notesByPlayedMeasureMap = generateNotesByPlayedMeasureAsMap();
	}

	public List<Triple<Integer, Integer, Note>> getNotesByPlayedMeasure() {
		return this.notesByPlayedMeasure;
	}

	public void setGlobalStructure(SortedMap<Integer, Triple<SegmentType, Integer, Double>> globalStructure) {
		this.globalStructureByFormStart = globalStructure;
		this.globalStructureBySegmentTokenStart = globalStructure == null ? null : generateGlobalStructureBySegmentTokenStart();
	}
	
	private SortedMap<Integer, SortedMap<Double, SegmentType>> generateGlobalStructureBySegmentTokenStart() {
		SortedMap<Integer, SortedMap<Double, SegmentType>> newGlobalStructureBySegmentTokenStart = new TreeMap<Integer, SortedMap<Double, SegmentType>>();
		
		for (Integer formStartMeasure : globalStructureByFormStart.keySet()) {
			Triple<SegmentType, Integer, Double> segment = globalStructureByFormStart.get(formStartMeasure);
			SegmentType type = segment.getFirst();
			Integer measureDeltaToStartToken = segment.getSecond();
			Double beatOffsetOfStartToken = segment.getThird();
			int measureOfStartToken = formStartMeasure + measureDeltaToStartToken;
			SortedMap<Double, SegmentType> newGlobalStructureBySegmentTokenStartOffsets = newGlobalStructureBySegmentTokenStart.get(measureOfStartToken);
			if (newGlobalStructureBySegmentTokenStartOffsets == null) {
				newGlobalStructureBySegmentTokenStartOffsets = new TreeMap<Double, SegmentType>();
				newGlobalStructureBySegmentTokenStart.put(measureOfStartToken, newGlobalStructureBySegmentTokenStartOffsets);
			}
		
			newGlobalStructureBySegmentTokenStartOffsets.put(beatOffsetOfStartToken, type);
		}
		
		return newGlobalStructureBySegmentTokenStart;
	}

	public SortedMap<Integer, SortedMap<Double, SegmentType>> getGlobalStructureBySegmentTokenStart() {
		return globalStructureBySegmentTokenStart;
	}

	public SortedMap<Integer, Triple<SegmentType, Integer, Double>> getGlobalStructureByFormStart() {
		return globalStructureByFormStart;
	}

	public int getAverageOctave() {
		if (averageOctave  == -1) {
			averageOctave = computeAverageOctave();
		}
		return averageOctave;
	}

	private int computeAverageOctave() {
		double total = 0.0;
		int count = 0;
		
		for (Triple<Integer, Integer, Note> noteTriple : notesByPlayedMeasure) {
			Note note = noteTriple.getThird();
			int pitch = note.pitch;
			if (pitch >= 0) {
				total += note.pitch/12.0;
				count++;
			}
		}
		
		return (int) (total/count);
	}

	private double durationInBeats = -1.0;
	public double getDurationInBeats() {
		if (durationInBeats == -1.0) {
			durationInBeats = calculateDurationInBeats();
		}
		return durationInBeats;
	}

	private double calculateDurationInBeats() {
		double totalBeats = 0.0;
		Time prevTime = null;
		int prevMeasure = -1;
		for (Integer measure : timeByAbsoluteMeasure.keySet()) {
			if (prevMeasure != -1) {
				totalBeats += prevTime.beats * (measure-prevMeasure);
			}
			prevMeasure = measure;
			prevTime = timeByAbsoluteMeasure.get(measure);
		}
		
		if (prevMeasure != -1) {
			totalBeats += prevTime.beats * (getMeasureCount()-prevMeasure);
		}
		
		return totalBeats;
	}

	public Pair<Integer, Double> getMeasureAndBeatForEvent(int mXML1EventNo, int eventsPerBeat) {
		int measure = 0;
		double beat = 0;
		
		int currEventNo = 0;
		
		Time prevTime = null;
		int prevMsr = 0;
		int eventsPerMeasure = -1;
		for (Integer currMsr : timeByAbsoluteMeasure.keySet()) {
			if (prevTime != null) {
				int eventsInPrevBlock = (currMsr - prevMsr) * eventsPerMeasure; // num msrs * events per measure
				if (eventsInPrevBlock + currEventNo <= mXML1EventNo) {
					currEventNo += eventsInPrevBlock;
				} else {
					break;
				}
			}
			
			Time time = timeByAbsoluteMeasure.get(currMsr);
			if (time.equals(Time.TWO_TWO)) {
				time = Time.FOUR_FOUR;
			}
			prevMsr = currMsr;
			prevTime = time;
			eventsPerMeasure = prevTime.beats * eventsPerBeat;
		}
		if (prevTime == null) {
			return null;
		}
		measure = prevMsr + ((mXML1EventNo - currEventNo) / eventsPerMeasure);
		beat = ((mXML1EventNo - currEventNo) % eventsPerMeasure) / (1.0 * eventsPerBeat);
		
		return new Pair<Integer,Double>(measure,beat);
	}

	Map<Integer, List<ParsedMusicXMLObject.MusicXMLAlignmentEvent>> alignmentEvents = new HashMap<Integer, List<ParsedMusicXMLObject.MusicXMLAlignmentEvent>>();
	public Map<Character, List<Pair<Integer, Double>>> rhymeMatches;
	private Map<Character, List<Pair<Pair<Integer, Double>, Pair<Integer, Double>>>> lyricMatches;
	private Map<Character, List<Pair<Pair<Integer, Double>, Pair<Integer, Double>>>> pitchMatches;
	private Map<Character, List<Pair<Pair<Integer, Double>, Pair<Integer, Double>>>> rhythmMatches;
	private Map<Character, List<Pair<Pair<Integer, Double>, Pair<Integer, Double>>>> harmonyMatches;
	private Map<Character, List<Pair<Pair<Integer, Double>, Pair<Integer, Double>>>> chorusMatches;
	private Map<Character, List<Pair<Pair<Integer, Double>, Pair<Integer, Double>>>> verseMatches;

	 // the measure and beat are mapped to an index value for the rhymeMatches data structure where other matches can be looked up
	private Map<Integer, Map<Double, Set<String>>> rhymeMatchGroupsByPosition = new HashMap<Integer, Map<Double, Set<String>>>();
	private Map<Integer, Map<Double, Set<String>>> lyricMatchGroupsByPosition = new HashMap<Integer, Map<Double, Set<String>>>();
	private Map<Integer, Map<Double, Set<String>>> pitchMatchGroupsByPosition = new HashMap<Integer, Map<Double, Set<String>>>();
	private Map<Integer, Map<Double, Set<String>>> rhythmMatchGroupsByPosition = new HashMap<Integer, Map<Double, Set<String>>>();
	private Map<Integer, Map<Double, Set<String>>> harmonyMatchGroupsByPosition = new HashMap<Integer, Map<Double, Set<String>>>();
	private Map<Integer, Map<Double, Set<String>>> chorusMatchGroupsByPosition = new HashMap<Integer, Map<Double, Set<String>>>();
	private Map<Integer, Map<Double, Set<String>>> verseMatchGroupsByPosition = new HashMap<Integer, Map<Double, Set<String>>>();
	
	private Map<Integer, Map<Double, Set<String>>> harPitRhyLyrMatchGroupsByPosition = new HashMap<Integer, Map<Double, Set<String>>>();

	public List<ParsedMusicXMLObject.MusicXMLAlignmentEvent> getAlignmentEvents(int eventsPerBeat) {
		List<ParsedMusicXMLObject.MusicXMLAlignmentEvent> events = alignmentEvents.get(eventsPerBeat);
		if (events == null) {
			events = computeAlignmentEvents(eventsPerBeat);
			alignmentEvents.put(eventsPerBeat, events);
		}
		
		return events;
	}
	
	/**
	 * 
	 * @param quarterNoteSubdivisions integer representing the smallest note type (independent of time signature). 1=quarter, 2=eighth, 4 = 16th
	 * @return
	 */
	public List<ParsedMusicXMLObject.MusicXMLAlignmentEvent> computeAlignmentEvents(int quarterNoteSubdivisions) {
		int nextNoteIdx = 0;
		Triple<Integer, Integer, Note> nextNote = notesByPlayedMeasure.get(nextNoteIdx++);
		Note currNote = null;
		boolean noteOnset;
		int nextHarmonyIdx = 0;
		Triple<Integer, Integer, Harmony> nextHarmony = nextHarmonyIdx < unoverlappingHarmonyByPlayedMeasure.size() ? unoverlappingHarmonyByPlayedMeasure.get(nextHarmonyIdx++) : null;
		Harmony currHarmony = null;
		boolean harmonyOnset;
		NoteLyric currLyric = null;
		boolean lyricOnset;
		Iterator<Entry<Integer, Triple<SegmentType, Integer, Double>>> globalStructureIterator = globalStructureByFormStart == null ? null : globalStructureByFormStart.entrySet().iterator();
		Entry<Integer, Triple<SegmentType, Integer, Double>> nextSegment = globalStructureIterator == null ? null : globalStructureIterator.next();
		int nextSegmentMeasureStart = nextSegment == null ? -1 : (nextSegment.getKey() + nextSegment.getValue().getSecond());
		SegmentType currSegment = null;
		int measureOffsetIntoSegment = -1;
		
		List<ParsedMusicXMLObject.MusicXMLAlignmentEvent> events = new ArrayList<ParsedMusicXMLObject.MusicXMLAlignmentEvent>();
		
		double currDivs = 0;
		double currBeat = 0.0;
		Time currTime = null;
		int currDivsPerQuarter = -1;
		double currDivsPerEvent = -1.;
		int currEventsPerMeasure = -1;
		int currDivsPerBeat = -1;
		double currBeatsPerEvent = -1;
		int currNoteStartingDivs = 0;
		
		Object[] allMatchGroupsByPosition = new Object[]{rhymeMatchGroupsByPosition, lyricMatchGroupsByPosition, pitchMatchGroupsByPosition, rhythmMatchGroupsByPosition, harmonyMatchGroupsByPosition, chorusMatchGroupsByPosition, verseMatchGroupsByPosition};
		Map<Integer, Map<Double, Set<String>>> matchGroupsByPosition;
		
		Set<String> activeRhymeGroups = new HashSet<String>();
		Set<String> activeLyricGroups = new HashSet<String>();
		Set<String> activePitchGroups = new HashSet<String>(); 
		Set<String> activeRhythmGroups = new HashSet<String>();
		Set<String> activeHarmonyGroups = new HashSet<String>();
		Set<String> activeChorusGroups = new HashSet<String>();
		Set<String> activeVerseGroups = new HashSet<String>();
		
		Object[] allActiveGroups = new Object[]{activeRhymeGroups, activeLyricGroups, activePitchGroups, activeRhythmGroups, activeHarmonyGroups, activeChorusGroups, activeVerseGroups};
		Set<String> activeGroup;
		
		Map<Double, Set<String>> groupsForMeasure;
		Set<String> groupsForMeasureAndBeat;
		
		int playedMeasureCount = getMeasureCount();
		int currNoteMeasure;
		double currNoteBeat;
		double currBeatsSinceOnset;
		int measureOfLastOnset = 0;
		double beatOfLastOnset = 0.0;
		// for each measure
		for(int playedMeasure = 0; playedMeasure < playedMeasureCount; playedMeasure++) {
//			System.out.println("Measure number " + (playedMeasure+1));
			// get relevant time signature and divs per beat info
			int absoluteMeasure = playedToAbsoluteMeasureNumberMap.get(playedMeasure);
			if (timeByAbsoluteMeasure.containsKey(absoluteMeasure))
				currTime = timeByAbsoluteMeasure.get(absoluteMeasure);
			if (divsPerQuarterByAbsoluteMeasure.containsKey(absoluteMeasure))
				currDivsPerQuarter = divsPerQuarterByAbsoluteMeasure.get(absoluteMeasure);
			currDivsPerBeat = (int) (currDivsPerQuarter * (4.0/currTime.beatType));
			currDivsPerEvent = 1.0*currDivsPerQuarter / quarterNoteSubdivisions; // calculate the number of divs per event
			currEventsPerMeasure = (int) ((4.0*currTime.beats/currTime.beatType) * quarterNoteSubdivisions); // and the number of events per measure 
			currBeatsPerEvent = 1.0*currDivsPerEvent/currDivsPerBeat;
			currDivs = 0.0;
			currBeat = 0.0;
			
			// for each event in the measure
			for (int i = 0; i < currEventsPerMeasure; i++) {
//				System.out.println("\tEvent number " + (i+1));
				// if there is a next segment to consider AND (this measure marks the start of that next segment AND 
				// the current beat has advanced to or beyond the start of that next segment OR the measure is past the start of the next segment) 
				if (nextSegment != null && (playedMeasure == nextSegmentMeasureStart && currBeat >= nextSegment.getValue().getThird() || playedMeasure > nextSegmentMeasureStart)) {
					currSegment = nextSegment.getValue().getFirst();
					measureOffsetIntoSegment = 0;
					nextSegment = globalStructureIterator.hasNext() ? globalStructureIterator.next() : null;
					nextSegmentMeasureStart = nextSegment == null ? -1 : (nextSegment.getKey() + nextSegment.getValue().getSecond());
				}
					
				// while there is a next note to consider AND this measure is the measure of that note AND the current divs (which is the target divs) is greater than the start of that note
				while (nextNote != null && (playedMeasure == nextNote.getFirst() && currDivs >= nextNote.getSecond() || playedMeasure > nextNote.getFirst())) {
					currNote = nextNote.getThird();
					currNoteMeasure = nextNote.getFirst();
					currNoteStartingDivs = nextNote.getSecond();
					currNoteBeat = currNoteStartingDivs / (1.0*currDivsPerBeat);
					
					if (currNote.isOnset()) {
						measureOfLastOnset = currNoteMeasure;
						beatOfLastOnset = currNoteBeat;
					}
					
					for (int j = 0; j < allMatchGroupsByPosition.length; j++) {
						matchGroupsByPosition = (Map<Integer, Map<Double, Set<String>>>) allMatchGroupsByPosition[j];
						activeGroup = (Set<String>) allActiveGroups[j];
						groupsForMeasure = matchGroupsByPosition.get(currNoteMeasure);
						if (groupsForMeasure == null) continue;
						groupsForMeasureAndBeat = groupsForMeasure.get(currNoteBeat);
						if (groupsForMeasureAndBeat == null) continue;
						for (String groupLabel : groupsForMeasureAndBeat) {
							if (activeGroup.contains(groupLabel)) {
								// this label is already active, so it should be marked for removal
								activeGroup.remove(groupLabel);
							} else {
								activeGroup.add(groupLabel);
							}
						}
					}
					
					nextNote = nextNoteIdx < notesByPlayedMeasure.size() ? notesByPlayedMeasure.get(nextNoteIdx++) : null;
				} 

				noteOnset = (currDivs == currNoteStartingDivs) && currNote.isPlayedNoteOnset();
				// if it's a rest
				if (currNote.pitch == Note.REST) {
					currLyric = null; // it's a new "null" lyric
					lyricOnset = noteOnset; // whether or not it's an onset depends on whether or not the note was an onset
				} else { // if it's not a rest
					NoteLyric noteLyric = currNote.getLyric(currSegment != null && currSegment.mustHaveDifferentLyricsOnRepeats());
					if (noteLyric != null) { // if it's got a new lyric
						currLyric = noteLyric; // set the new lyric
						lyricOnset = noteOnset; // it's an onset if the note is an onset
					} else {
						lyricOnset = false; // it hasn't got a new lyric, which means it keeps the same lyric from previous notes, thus not an onset.
					}
				}
				
				//if curr lyr 
				
				harmonyOnset = false;
				while (nextHarmony != null && (playedMeasure == nextHarmony.getFirst() && currDivs >= nextHarmony.getSecond() || playedMeasure > nextHarmony.getFirst())) {
					currHarmony = nextHarmony.getThird();
					harmonyOnset = (currDivs == nextHarmony.getSecond());
					nextHarmony = nextHarmonyIdx < unoverlappingHarmonyByPlayedMeasure.size() ? unoverlappingHarmonyByPlayedMeasure.get(nextHarmonyIdx++) : null;
				}

				currBeatsSinceOnset = currBeat - beatOfLastOnset;
				
				for(int m = measureOfLastOnset; m < playedMeasure; m++) {
					currBeatsSinceOnset += this.getTimeForAbsoluteMeasure(playedToAbsoluteMeasureNumberMap.get(m)).beats;
				}

//				System.out.println("\t\tcurrNote:"+currNote+"\n\t\tnoteOnset:"+noteOnset+"\n\t\tcurrBeatsSinceOnset:"+currBeatsSinceOnset+"\n\t\tcurrHarmony:"+currHarmony+
//						"\n\t\tharmonyOnset:"+harmonyOnset+"\n\t\tcurrLyric:"+currLyric+"\n\t\tlyricOnset:"+lyricOnset+"\n\t\tcurrBeat:"+currBeat+"\n\t\tcurrBeatsToDownBeat:"+(currTime.beats-currBeat)+
//						"\n\t\tcurrSegment:"+currSegment+"\n\t\tmeasureOffsetIntoSegment:"+measureOffsetIntoSegment + 
//						"\n\t\tactiveLyricGroups:" + activeLyricGroups +
//						"\n\t\tactivePitchGroups:" + activePitchGroups +
//						"\n\t\tactiveRhythmGroups:" + activeRhythmGroups +
//						"\n\t\tactiveHarmonyGroups:" + activeHarmonyGroups +
//						"\n\t\tactiveRhymeGroups:" + activeRhymeGroups
//						);
				
				events.add(new ParsedMusicXMLObject.MusicXMLAlignmentEvent(currNote, noteOnset, currBeatsSinceOnset, currHarmony, harmonyOnset, currLyric, lyricOnset, playedMeasure, currBeat, currTime.beats-currBeat, currSegment, measureOffsetIntoSegment, 
						new HashSet<String>(activeLyricGroups), new HashSet<String>(activePitchGroups), new HashSet<String>(activeRhythmGroups), 
						new HashSet<String>(activeHarmonyGroups), new HashSet<String>(activeChorusGroups), new HashSet<String>(activeVerseGroups), new HashSet<String>(activeRhymeGroups)));
				currDivs += currDivsPerEvent;
				currBeat += currBeatsPerEvent;
				activeRhymeGroups.clear();
			}
			measureOffsetIntoSegment++;
		}
		
		annotateTiedNoteDurations(events);
		
		return events;
	}

	private void annotateTiedNoteDurations(List<MusicXMLAlignmentEvent> events) {
		double durationSum = 0.0;
		int beginningEventIdxOfDuration = -1;
		Note currNote = null;
		for (int i = 0; i < events.size(); i++) {
			MusicXMLAlignmentEvent event = events.get(i);
			if (event.currBeatsSinceOnset == 0) {
				if (beginningEventIdxOfDuration != -1) {
					for (int j = beginningEventIdxOfDuration; j < i; j++) {
						events.get(j).tiedDurationOfCurrentNote = durationSum;
					}
				}
				durationSum = 0.0;
				beginningEventIdxOfDuration = i;
			}
			if (event.note != currNote) {
				currNote = event.note;
				durationSum += currNote.duration;
			}
		}
		if (durationSum != 0.0) {
			for (int j = beginningEventIdxOfDuration; j < events.size(); j++) {
				events.get(j).tiedDurationOfCurrentNote = durationSum;
			}
		}
	}

	public void setMatches(Map<Character, List<Pair<Integer, Double>>> rhymeMatches, 
			Map<Character, List<Pair<Pair<Integer, Double>, Pair<Integer, Double>>>> lyricMatches, 
			Map<Character, List<Pair<Pair<Integer, Double>, Pair<Integer, Double>>>> pitchMatches, 
			Map<Character, List<Pair<Pair<Integer, Double>, Pair<Integer, Double>>>> rhythmMatches, 
			Map<Character, List<Pair<Pair<Integer, Double>, Pair<Integer, Double>>>> harmonyMatches,
			Map<Character, List<Pair<Pair<Integer, Double>, Pair<Integer, Double>>>> chorusMatches,
			Map<Character, List<Pair<Pair<Integer, Double>, Pair<Integer, Double>>>> verseMatches) {
		this.rhymeMatches = rhymeMatches;		
		this.lyricMatches = lyricMatches;		
		this.pitchMatches = pitchMatches;		
		this.rhythmMatches = rhythmMatches;		
		this.harmonyMatches = harmonyMatches;
		this.chorusMatches = chorusMatches;
		this.verseMatches = verseMatches;
		
		List<Pair<Integer, Double>> rhymeMatchGroup;
		Map<Double, Set<String>> rhymeMatchGroupsByMeasurePosition;
		Set<String> rhymeMatchGroupsByMeasureBeat;
		Integer measure;
		Double beat;
		for (Character rhymeGroupLabel : rhymeMatches.keySet()) {
			rhymeMatchGroup = rhymeMatches.get(rhymeGroupLabel);
			for (int i = 0; i < rhymeMatchGroup.size(); i++) {
				Pair<Integer, Double> rhymePosition = rhymeMatchGroup.get(i);
				measure = rhymePosition.getFirst();
				beat = rhymePosition.getSecond();
				rhymeMatchGroupsByMeasurePosition = rhymeMatchGroupsByPosition.get(measure);
				if (rhymeMatchGroupsByMeasurePosition == null) {
					rhymeMatchGroupsByMeasurePosition = new TreeMap<Double, Set<String>>();
					rhymeMatchGroupsByPosition.put(measure,rhymeMatchGroupsByMeasurePosition);
				}
				rhymeMatchGroupsByMeasureBeat = rhymeMatchGroupsByMeasurePosition.get(beat);
				if (rhymeMatchGroupsByMeasureBeat == null) {
					rhymeMatchGroupsByMeasureBeat = new HashSet<String>();
					rhymeMatchGroupsByMeasurePosition.put(beat, rhymeMatchGroupsByMeasureBeat);
				}
				rhymeMatchGroupsByMeasureBeat.add("" + rhymeGroupLabel + String.format("%04d", i+1));
			}
		}
		
		Object[] allMatches = new Object[]{lyricMatches, pitchMatches, rhythmMatches, harmonyMatches, chorusMatches, verseMatches};
		Map<Character, List<Pair<Pair<Integer, Double>, Pair<Integer, Double>>>> matches;
		Object[] allMatchGroupsByPosition = new Object[]{lyricMatchGroupsByPosition, pitchMatchGroupsByPosition, rhythmMatchGroupsByPosition, harmonyMatchGroupsByPosition, chorusMatchGroupsByPosition, verseMatchGroupsByPosition};
		Map<Integer, Map<Double, Set<String>>> matchGroupsByPosition;
		Pair<Integer, Double> startPosition, endPosition;
		Map<Double, Set<String>> matchGroupsByMeasurePosition;
		Set<String> matchGroupsByMeasureBeat;
		Pair<Pair<Integer, Double>, Pair<Integer, Double>> groupForLabel;
		// for each viewpoint
		for (int i = 0; i < allMatches.length; i++) {
			matches = (Map<Character, List<Pair<Pair<Integer, Double>, Pair<Integer, Double>>>>) allMatches[i];
			matchGroupsByPosition = (Map<Integer, Map<Double, Set<String>>>) allMatchGroupsByPosition[i];
			// for each group
			for (Character groupLabel : matches.keySet()) {
				List<Pair<Pair<Integer, Double>, Pair<Integer, Double>>> groupsForLabel = matches.get(groupLabel);
				// for each match in group
				for (int j = 0; j < groupsForLabel.size(); j++) {
					groupForLabel = groupsForLabel.get(j);
					startPosition = groupForLabel.getFirst();
					measure = startPosition.getFirst();
					beat = startPosition.getSecond();
					matchGroupsByMeasurePosition = matchGroupsByPosition.get(measure);
					if (matchGroupsByMeasurePosition == null) {
						matchGroupsByMeasurePosition = new TreeMap<Double, Set<String>>();
						matchGroupsByPosition.put(measure,matchGroupsByMeasurePosition);
					}
					matchGroupsByMeasureBeat = matchGroupsByMeasurePosition.get(beat);
					if (matchGroupsByMeasureBeat == null) {
						matchGroupsByMeasureBeat = new HashSet<String>();
						matchGroupsByMeasurePosition.put(beat, matchGroupsByMeasureBeat);
					}
					final String matchGroupLabel = "" + groupLabel + (j+1);
					matchGroupsByMeasureBeat.add(matchGroupLabel);
					
					endPosition = groupForLabel.getSecond();
					measure = endPosition.getFirst();
					beat = endPosition.getSecond();
					matchGroupsByMeasurePosition = matchGroupsByPosition.get(measure);
					if (matchGroupsByMeasurePosition == null) {
						matchGroupsByMeasurePosition = new TreeMap<Double, Set<String>>();
						matchGroupsByPosition.put(measure,matchGroupsByMeasurePosition);
					}
					matchGroupsByMeasureBeat = matchGroupsByMeasurePosition.get(beat);
					if (matchGroupsByMeasureBeat == null) {
						matchGroupsByMeasureBeat = new HashSet<String>();
						matchGroupsByMeasurePosition.put(beat, matchGroupsByMeasureBeat);
					}
					matchGroupsByMeasureBeat.add(matchGroupLabel);
				}
			}
		}
		
	}

	public Map<Character, List<Pair<Pair<Integer, Double>, Pair<Integer, Double>>>> getAllMatchingLyricGroups() {
		return lyricMatches;
	}
	
	public Map<Character, List<Pair<Pair<Integer, Double>, Pair<Integer, Double>>>> getAllMatchingPitchGroups() {
		return pitchMatches;
	}
	
	public Map<Character, List<Pair<Pair<Integer, Double>, Pair<Integer, Double>>>> getAllMatchingHarmonyGroups() {
		return harmonyMatches;
	}
	
	public Map<Character, List<Pair<Pair<Integer, Double>, Pair<Integer, Double>>>> getAllMatchingChorusGroups() {
		return chorusMatches;
	}
	
	public Map<Character, List<Pair<Pair<Integer, Double>, Pair<Integer, Double>>>> getAllMatchingVerseGroups() {
		return verseMatches;
	}
	
	public Map<Character, List<Pair<Pair<Integer, Double>, Pair<Integer, Double>>>> getAllMatchingRhythmGroups() {
		return rhythmMatches;
	}
	
	public Map<Character, List<Pair<Integer, Double>>> getAllMatchingRhymeGroups() {
		return rhymeMatches;
	}
	
}
