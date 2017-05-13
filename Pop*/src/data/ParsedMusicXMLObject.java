package data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import condition.ExactBinaryMatch;
import constraint.Constraint;
import data.MusicXMLParser.Harmony;
import data.MusicXMLParser.Key;
import data.MusicXMLParser.Note;
import data.MusicXMLParser.NoteLyric;
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
					+ ", " + beat + ", " + segmentType + ", " + measureOffsetInSegment;
		}

		public Note note;
		public boolean noteOnset;
		public Harmony harmony;
		public boolean harmonyOnset;
		public NoteLyric lyric;
		public String strippedLyricLCText;
		public boolean lyricOnset;
		double beat;
		public SegmentType segmentType;
		private int measureOffsetInSegment;
	
		public MusicXMLAlignmentEvent(Note note, boolean noteOnset, Harmony harmony, boolean harmonyOnset,
				NoteLyric lyric, boolean lyricOnset, double currBeat, SegmentType segmentType, int measureOffsetInSegment) {
			this.note = note;
			this.noteOnset = noteOnset;
			this.harmony = harmony;
			this.harmonyOnset = harmonyOnset;
			this.lyric = lyric;
			this.strippedLyricLCText = lyric == null? "":lyric.text.replaceAll("[^a-zA-Z ]", "").toLowerCase();
			this.lyricOnset = lyricOnset;
			this.beat = currBeat;
			this.segmentType = segmentType;
			this.measureOffsetInSegment = measureOffsetInSegment;
		}
	
		public boolean shouldAlignWith(MusicXMLAlignmentEvent that) {
	
			if (this.segmentType != SegmentType.CHORUS || this.segmentType != that.segmentType) {
				return false;
			}
			
			if (this.measureOffsetInSegment != that.measureOffsetInSegment) {
				return false;
			}
			
			if (this.beat != that.beat) {
				return false;
			}
			
			return true;
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
	public List<Pair<List<NoteLyric>, List<Triple<String, StressedPhone[], Integer>>>> lyricsWithDifferentSyllableCountThanAssociatedNotes 
		= new ArrayList<Pair<List<NoteLyric>, List<Triple<String, StressedPhone[], Integer>>>>();
	
	public int noteCount = -1; // needs to be set
	private SortedMap<Integer, Integer> divsPerQuarterByAbsoluteMeasure;
	
	/**
	 * Key is measure where segment Form starts (ignoring pickups or delays), value contains 
	 * 1) segment type of the form and 2) the delta from the form start of the measure of the
	 * first actual note belonging to the form and and 3) offset within that measure 
	 */
	private SortedMap<Integer, Triple<SegmentType, Integer, Double>> globalStructureByFormStart;
	public SortedMap<Integer, SortedMap<Double, List<Constraint<NoteLyric>>>> segmentStructure;
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

	public Time getTimeForMeasure(int measure) {
		Time time = Utils.valueForKeyBeforeOrEqualTo(measure, timeByAbsoluteMeasure);

		// We'll count anything in 2/2 as 4/4
		if (time.equals(Time.TWO_TWO)) {
			return Time.FOUR_FOUR;
		}
		
		return time;
	}

	public void setDivsPerQuarterByAbsoluteMeasure(SortedMap<Integer, Integer> divsPerQuarterByAbsoluteMeasure) {
		this.divsPerQuarterByAbsoluteMeasure = divsPerQuarterByAbsoluteMeasure;
	}
	
	public double divsToBeats(int divsOffset, int playedMeasure) {
		int absoluteMeasure = playedToAbsoluteMeasureNumberMap.get(playedMeasure);
		return (divsOffset*1.0/Utils.valueForKeyBeforeOrEqualTo(absoluteMeasure, divsPerQuarterByAbsoluteMeasure)) * (getTimeForMeasure(absoluteMeasure).beatType/4.0);
	}

	public Integer beatsToDivs(Double beatsOffset, Integer playedMeasure) {
		int absoluteMeasure = playedToAbsoluteMeasureNumberMap.get(playedMeasure);
		return (int) ((beatsOffset / (getTimeForMeasure(absoluteMeasure).beatType/4.0)) * Utils.valueForKeyBeforeOrEqualTo(absoluteMeasure, divsPerQuarterByAbsoluteMeasure));
	}
	
	public double getDivsPerQuarterForAbsoluteMeasure(int absoluteMeasureNumber) {
		return Utils.valueForKeyBeforeOrEqualTo(absoluteMeasureNumber, divsPerQuarterByAbsoluteMeasure);
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

	public List<ParsedMusicXMLObject.MusicXMLAlignmentEvent> getAlignmentEvents(int eventsPerBeat) {
		List<ParsedMusicXMLObject.MusicXMLAlignmentEvent> events = alignmentEvents.get(eventsPerBeat);
		if (events == null) {
			events = computeAlignmentEvents(eventsPerBeat);
			alignmentEvents.put(eventsPerBeat, events);
		}
		
		return events;
	}
	
	public List<ParsedMusicXMLObject.MusicXMLAlignmentEvent> computeAlignmentEvents(int eventsPerBeat) {
		int nextNoteIdx = 0;
		Triple<Integer, Integer, Note> nextNote = notesByPlayedMeasure.get(nextNoteIdx++);
		Note currNote = null;
		boolean noteOnset;
		int nextHarmonyIdx = 0;
		Triple<Integer, Integer, Harmony> nextHarmony = unoverlappingHarmonyByPlayedMeasure.get(nextHarmonyIdx++);
		Harmony currHarmony = null;
		boolean harmonyOnset;
		NoteLyric currLyric = null;
		boolean lyricOnset;
		Iterator<Entry<Integer, Triple<SegmentType, Integer, Double>>> globalStructureIterator = globalStructureByFormStart.entrySet().iterator();
		Entry<Integer, Triple<SegmentType, Integer, Double>> nextSegment = globalStructureIterator.next();
		int nextSegmentMeasureStart = (nextSegment.getKey() + nextSegment.getValue().getSecond());
		SegmentType currSegment = null;
		int measureOffsetIntoSegment = -1;
		
		List<ParsedMusicXMLObject.MusicXMLAlignmentEvent> events = new ArrayList<ParsedMusicXMLObject.MusicXMLAlignmentEvent>();
		
		int currDivs = 0;
		double currBeat = 0.0;
		Time currTime = null;
		int currDivsPerQuarter = -1;
		int currDivsPerEvent = -1;
		int currEventsPerMeasure = -1;
		int currDivsPerBeat = -1;
		double currBeatsPerEvent = -1;
		int currNoteStartingDivs = 0;
		
		int measureCount = getMeasureCount();
		// for each measure
		for(int measure = 0; measure < measureCount; measure++) {
//			System.out.println("Measure number " + measure);
			// get relevant time signature and divs per beat info
			if (timeByAbsoluteMeasure.containsKey(measure))
				currTime = timeByAbsoluteMeasure.get(measure);
			if (divsPerQuarterByAbsoluteMeasure.containsKey(measure))
				currDivsPerQuarter = divsPerQuarterByAbsoluteMeasure.get(measure);
			currDivsPerBeat = (int) (currDivsPerQuarter * (4.0/currTime.beatType));
			currDivsPerEvent = currDivsPerBeat / eventsPerBeat; // calculate the number of divs per event
			currEventsPerMeasure = currTime.beats * eventsPerBeat; // and the number of events per measure 
			currBeatsPerEvent = 1.0*currDivsPerEvent/currDivsPerBeat;
			currDivs = 0;
			currBeat = 0.0;
			
			// for each event in the measure
			for (int i = 0; i < currEventsPerMeasure; i++) {
//				System.out.println("\tEvent number " + i);
				// if there is a next segment to consider AND (this measure marks the start of that next segment AND 
				// the current beat has advanced to or beyond the start of that next segment OR the measure is past the start of the next segment) 
				if (nextSegment != null && (measure == nextSegmentMeasureStart && currBeat >= nextSegment.getValue().getThird() || measure > nextSegmentMeasureStart)) {
					currSegment = nextSegment.getValue().getFirst();
					measureOffsetIntoSegment = 0;
					nextSegment = globalStructureIterator.hasNext() ? globalStructureIterator.next() : null;
					nextSegmentMeasureStart = nextSegment == null ? -1 : (nextSegment.getKey() + nextSegment.getValue().getSecond());
				}
					
				// while there is a next note to consider AND this measure is the measure of that note AND the current divs (which is the target divs) is greater than the start of that note
				while (nextNote != null && (measure == nextNote.getFirst() && currDivs >= nextNote.getSecond() || measure > nextNote.getFirst())) {
					currNote = nextNote.getThird();
					currNoteStartingDivs = nextNote.getSecond();
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
				
				harmonyOnset = false;
				while (nextHarmony != null && (measure == nextHarmony.getFirst() && currDivs >= nextHarmony.getSecond() || measure > nextHarmony.getFirst())) {
					currHarmony = nextHarmony.getThird();
					harmonyOnset = (currDivs == nextHarmony.getSecond());
					nextHarmony = nextHarmonyIdx < unoverlappingHarmonyByPlayedMeasure.size() ? unoverlappingHarmonyByPlayedMeasure.get(nextHarmonyIdx++) : null;
				}

//				System.out.println("\t\tcurrNote:"+currNote+"\n\t\tnoteOnset:"+noteOnset+"\n\t\tcurrHarmony:"+currHarmony+
//						"\n\t\tharmonyOnset:"+harmonyOnset+"\n\t\tcurrLyric:"+currLyric+"\n\t\tlyricOnset:"+lyricOnset+"\n\t\tcurrBeat:"+currBeat+
//						"\n\t\tcurrSegment:"+currSegment+"\n\t\tmeasureOffsetIntoSegment:"+measureOffsetIntoSegment);
				events.add(new ParsedMusicXMLObject.MusicXMLAlignmentEvent(currNote, noteOnset, currHarmony, harmonyOnset, currLyric, lyricOnset, currBeat, currSegment, measureOffsetIntoSegment));
				currDivs += currDivsPerEvent;
				currBeat += currBeatsPerEvent;
			}
			measureOffsetIntoSegment++;
		}
		return events;
	}

	private boolean hasExactBinaryMatchMarkedAt(Integer measure, double beatOffset) {

		SortedMap<Double, List<Constraint<NoteLyric>>> constraintsForMeasure = segmentStructure.get(measure);
		if (constraintsForMeasure != null) {
			List<Constraint<NoteLyric>> constraintsForBeatOffset = constraintsForMeasure.get(beatOffset);
			if (constraintsForBeatOffset != null) {
				for (Constraint<NoteLyric> constraint : constraintsForBeatOffset) {
					if (constraint.getCondition() instanceof ExactBinaryMatch && constraint.getDesiredConditionState()) {
						return true;
					}
				}
			}
		}
		
		return false;
	}

}
