package composition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import constraint.Constraint;
import data.MusicXML.Harmony;
import data.MusicXML.Key;
import data.MusicXML.KeyMode;
import data.MusicXML.Note;
import data.MusicXML.Time;
import globalstructure.SegmentType;

public class Measure {

	public int divisions = -1;
	public Key key = null;
	public Time time = null;
	// harmony
	// notes
	// lyrics
	SegmentType segmentType; // SegmentType
	int offsetWithinSegment;// MeasureOffsetWithinSegment
	private TreeMap<Double, List<Constraint>> constraints = new TreeMap<Double, List<Constraint>>();
	private TreeMap<Double, Harmony> harmonies = new TreeMap<Double, Harmony>();
	private TreeMap<Double, Note> notes = new TreeMap<Double, Note>();
	
	public void setDivions(int divisions) {
		this.divisions = divisions;
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

	public String toXML(int indentationLevel) {
		StringBuilder str = new StringBuilder();
		
		// TODO: spread staves, make chords bigger
		
		// do all harmonies
		for (Entry<Double, Harmony> offsetHarmony : harmonies.entrySet()) {
			str.append(offsetHarmony.getValue().toXML(indentationLevel, (int) (1 * offsetHarmony.getKey())));
		}
		
		// do all notes
		for (Note note : notes.values()) {
			// this assumes note durations are sufficient to calculate note onsets (no gaps)
			str.append(note.toXML(indentationLevel));
		}
		// TODO: constraints
		
		return str.toString();
	}

}
