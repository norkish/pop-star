package substructure;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import constraint.Constraint;
import constraint.ConstraintBlock;
import harmony.Chord;
import lyrics.Lyric;
import pitch.Pitch;
import rhythm.RhythmSegment;

public class SegmentSubstructure implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public ConstraintBlock<Lyric> lyricConstraints = null;
	public ConstraintBlock<Chord> chordConstraints = null;
	public ConstraintBlock<RhythmSegment> rhythmConstraints = null;
	public ConstraintBlock<Pitch> pitchConstraints = null;
	public int linesPerSegment;

	public SegmentSubstructure(int linesPerSegment) {
		
		this.linesPerSegment = linesPerSegment;
		
		lyricConstraints = new ConstraintBlock<Lyric>();
		chordConstraints = new ConstraintBlock<Chord>();
		rhythmConstraints = new ConstraintBlock<RhythmSegment>();
		pitchConstraints = new ConstraintBlock<Pitch>();
		
		for (int i = 0; i < linesPerSegment; i++) {
			lyricConstraints.addLineConstraints(new ArrayList<Constraint<Lyric>>());
			chordConstraints.addLineConstraints(new ArrayList<Constraint<Chord>>());
			rhythmConstraints.addLineConstraints(new ArrayList<Constraint<RhythmSegment>>());
			pitchConstraints.addLineConstraints(new ArrayList<Constraint<Pitch>>());
		}
	}

	public void addLyricConstraint(int lineNum, Constraint<Lyric> constraint) {
		lyricConstraints.addConstraint(lineNum, constraint);
	}
	
	public void addChordConstraint(int lineNum, Constraint<Chord> constraint) {
		chordConstraints.addConstraint(lineNum,constraint);
	}
	
	public void addRhythmConstraint(int lineNum, Constraint<RhythmSegment> constraint) {
		rhythmConstraints.addConstraint(lineNum,constraint);
	}
	
	public void addPitchConstraint(int lineNum, Constraint<Pitch> constraint) {
		pitchConstraints.addConstraint(lineNum,constraint);
	}

	public String toString()
	{
		StringBuilder str = new StringBuilder();
		
		for (int i = 0; i < linesPerSegment; i++) {
			str.append("Line ");
			str.append(i+1);
			str.append(":\n");
			for (Constraint<Chord> constraint : chordConstraints.getConstraintsForLine(i)) {
				str.append("\tThe CHORD ");
				str.append(constraint);
				str.append('\n');
			}
			for (Constraint<Lyric> constraint : lyricConstraints.getConstraintsForLine(i)) {
				str.append("\tThe LYRIC ");
				str.append(constraint);
				str.append('\n');
			}
			for (Constraint<RhythmSegment> constraint : rhythmConstraints.getConstraintsForLine(i)) {
				str.append("\tThe RHYTHM ");
				str.append(constraint);
				str.append('\n');
			}
			for (Constraint<Pitch> constraint : pitchConstraints.getConstraintsForLine(i)) {
				str.append("\tThe PITCH ");
				str.append(constraint);
				str.append('\n');
			}
			str.append('\n');
		}
		
		return str.toString();
	}

	public void addLyricConstraints(ConstraintBlock<Lyric> constraints) {
		lyricConstraints.merge(constraints);
	}

	public void addLyricLengthConstraints(List<Integer> lengthConstraints) {
		lyricConstraints.addLengthConstraints(lengthConstraints);
	}

	public void addChordConstraints(ConstraintBlock<Chord> chordConstraints) {
		chordConstraints.merge(chordConstraints);
	}

	public void addChordLengthConstraints(List<Integer> lengthConstraints) {
		chordConstraints.addLengthConstraints(lengthConstraints);
	}
	
}
