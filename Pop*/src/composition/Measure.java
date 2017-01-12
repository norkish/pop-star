package composition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import constraint.Constraint;
import data.MusicXMLParser.Bass;
import data.MusicXMLParser.Harmony;
import data.MusicXMLParser.Key;
import data.MusicXMLParser.KeyMode;
import data.MusicXMLParser.Note;
import data.MusicXMLParser.NoteLyric;
import data.MusicXMLParser.Quality;
import data.MusicXMLParser.Time;
import globalstructure.SegmentType;
import melody.MelodyEngineer;

public class Measure {

	public int divisionsPerQuarterNote = -1;
	public Key key = null;
	public Time time = null;
	public SegmentType segmentType = null; // SegmentType
	public int offsetWithinSegment = -1;// MeasureOffsetWithinSegment

	private TreeMap<Double, List<Constraint>> constraints = new TreeMap<Double, List<Constraint>>();
	private TreeMap<Double, Harmony> harmonies = new TreeMap<Double, Harmony>();
	private TreeMap<Double, Note> notes = new TreeMap<Double, Note>();
	private TreeMap<Double, List<Note>> orchestration = null;
	private TreeMap<Double, List<Note>> bassOrchestration;
	
	public Measure(SegmentType segType, int offset) {
		this.segmentType = segType;
		this.offsetWithinSegment = offset;
	}

	public void setDivions(int divisions) {
		this.divisionsPerQuarterNote = divisions;
	}
	
	public void setKey(int fifths, KeyMode mode) {
		this.key = new Key(fifths, mode);
	}
	
	public void setTime(int beats, int beatsType) {
		this.time = new Time(beats, beatsType);
	}
	
	public void addConstraint(Double offset, Constraint constraint) {
		List<Constraint> constraintsAtOffset = constraints.get(offset);
		if (constraintsAtOffset == null) {
			constraintsAtOffset = new ArrayList<Constraint>();
			constraints.put(offset, constraintsAtOffset);
		}
		constraintsAtOffset.add(constraint);
	}

	public void addAllConstraints(Double offset, List<Constraint> newConstraintsAtOffset) {
		List<Constraint> currConstraintsAtOffset = constraints.get(offset);
		if (currConstraintsAtOffset == null) {
			constraints.put(offset, newConstraintsAtOffset);
		} else {
			currConstraintsAtOffset.addAll(newConstraintsAtOffset);
		}
	}

	public void addHarmony(Double offset, Harmony harmony) {
		harmonies.put(offset, harmony);
	}

	public void addNote(double offset, Note note) {
		notes.put(offset, note);
	}

	public TreeMap<Double, Note> getNotes() {
		return notes;
	}

	public TreeMap<Double, List<Constraint>> getConstraints() {
		return constraints;
	}

	public String leadToXML(int indentationLevel) {
		StringBuilder str = new StringBuilder();
		
		// TODO: spread staves, make chords bigger
		
		// do all harmonies
		for (Entry<Double, Harmony> offsetHarmony : harmonies.entrySet()) {
			str.append(offsetHarmony.getValue().toXML(indentationLevel, (int) (1 * offsetHarmony.getKey())));
		}
		
		// do all notes
		for (Note note : notes.values()) {
			// this assumes note durations are sufficient to calculate note onsets (no gaps)
			str.append(note.toXML(indentationLevel, true));
		}
		// TODO: constraints
		
		return str.toString();
	}

	public String orchestrationToXML(int indentationLevel, char part) {
		StringBuilder str = new StringBuilder();
		
		TreeMap<Double, List<Note>> partNotes = part == 'p' ? orchestration : bassOrchestration;
		
		// do chords, assigning to staves 
		for (Entry<Double, List<Note>> offsetHarmony : partNotes.entrySet()) {
			for (Note note : offsetHarmony.getValue()) {
				str.append(note.toXML(indentationLevel, false));
			}
		}
		
		return str.toString();
	}
	
	public Note getClosestNote(double prevMeasureOffset) {
		// TODO Auto-generated method stub
		return null;
	}

	public NoteLyric getClosestNoteLyric(double prevMeasureOffset) {
		// TODO Auto-generated method stub
		return null;
	}

	public Harmony getClosestHarmony(double prevMeasureOffset) {
		// TODO Auto-generated method stub
		return null;
	}

	public void initOrchestration() {
		orchestration = new TreeMap<Double, List<Note>>();
		bassOrchestration = new TreeMap<Double, List<Note>>();
	}

	public void addChordForHarmonyAt(double currPos, double durationsInBeats) {
		Harmony harmonyAtCurrPos = tokenAtPos(currPos, harmonies);
		if (harmonyAtCurrPos == null) return;
		
		List<Note> orchestrationNotes = orchestration.get(currPos);
		if (orchestrationNotes == null) {
			orchestrationNotes = new ArrayList<Note>();
			orchestration.put(currPos, orchestrationNotes);
		}
		
		boolean[] pitches = harmonyAtCurrPos.quality.getPitches();
		int rootPitch = harmonyAtCurrPos.root.rootStep + 45;
		int durationInDivs = (int) (durationsInBeats * (4.0 / this.time.beatType) * this.divisionsPerQuarterNote);
		final List<Note> chordRootWithTies = MelodyEngineer.createTiedNoteWithDuration(durationInDivs, rootPitch, this.divisionsPerQuarterNote);
		assert chordRootWithTies.size() == 1: "Chord note requested that requires tied notes to accomplish desired duration";
		Note chordRoot = chordRootWithTies.get(0);
		orchestrationNotes.add(chordRoot);
		
		for (int i = 0; i < pitches.length; i++) {
			boolean intervalOn = pitches[i];
			if (intervalOn) {
				Note newNote = new Note(rootPitch + Quality.HARMONY_CONSTANT_INTERVALS[i], 
						chordRoot.duration, chordRoot.type, null, chordRoot.dots, chordRoot.tie, null, orchestrationNotes.size()>0);
				orchestrationNotes.add(newNote);
			}
		}
	}

	private <T> T tokenAtPos(double currPos, TreeMap<Double, T> tokens) {
		T currToken = null;
		for (Double pos : tokens.keySet()) {
			if (pos > currPos) {
				return currToken;
			}
			currToken = tokens.get(pos);
		}
		return currToken;
	}

	public void addBassNoteForHarmony(double currPos, double durationsInBeats) {
		Harmony harmonyAtCurrPos = tokenAtPos(currPos, harmonies);
		if (harmonyAtCurrPos == null) return;
		
		List<Note> bassOrchestrationNotes = bassOrchestration.get(currPos);
		if (bassOrchestrationNotes == null) {
			bassOrchestrationNotes = new ArrayList<Note>();
			bassOrchestration.put(currPos, bassOrchestrationNotes);
		}
		
		final Bass bass = harmonyAtCurrPos.bass;
		int rootPitch = (bass == null ? harmonyAtCurrPos.root.rootStep : bass.bassStep) + 33;
		int durationInDivs = (int) (durationsInBeats * (4.0 / this.time.beatType) * this.divisionsPerQuarterNote);
		final List<Note> chordBassWithTies = MelodyEngineer.createTiedNoteWithDuration(durationInDivs, rootPitch, this.divisionsPerQuarterNote);
		assert chordBassWithTies.size() == 1: "Chord Bass note requested that requires tied notes to accomplish desired duration";
		Note chordRoot = chordBassWithTies.get(0);
		bassOrchestrationNotes.add(chordRoot);
	}


}
