package substructure;

import java.util.ArrayList;
import java.util.List;

import constraint.Constraint;
import harmony.Chord;
import lyrics.Lyric;
import pitch.Pitch;
import pitch.PitchSegment;
import rhythm.RhythmSegment;

public class SegmentSubstructure {
	
	public List<List<Constraint<Lyric>>> lyricConstraints = null;
	public List<List<Constraint<Chord>>> chordConstraints = null;
	public List<List<Constraint<RhythmSegment>>> rhythmConstraints = null;
	public List<List<Constraint<Pitch>>> pitchConstraints = null;
	public int linesPerSegment;
	public int measuresPerLine;
	public int minWordsPerLine;
	public int maxWordsPerLine;
	public int substructureRepetitions;
	public boolean relativeMinorKey;

	public SegmentSubstructure(int linesPerSegment, int measuresPerLine, int minWordsPerLine, int maxWordsPerLine,
			int substructureRepetitions, boolean relativeMinorKey) {
		
		this.linesPerSegment = linesPerSegment;
		this.measuresPerLine = measuresPerLine;
		this.minWordsPerLine = minWordsPerLine;
		this.maxWordsPerLine = maxWordsPerLine;
		this.substructureRepetitions = substructureRepetitions;
		this.relativeMinorKey = relativeMinorKey;
		
		lyricConstraints = new ArrayList<List<Constraint<Lyric>>>();
		chordConstraints = new ArrayList<List<Constraint<Chord>>>();
		rhythmConstraints = new ArrayList<List<Constraint<RhythmSegment>>>();
		pitchConstraints = new ArrayList<List<Constraint<Pitch>>>();
		
		for (int i = 0; i < linesPerSegment; i++) {
			lyricConstraints.add(new ArrayList<Constraint<Lyric>>());
			chordConstraints.add(new ArrayList<Constraint<Chord>>());
			rhythmConstraints.add(new ArrayList<Constraint<RhythmSegment>>());
			pitchConstraints.add(new ArrayList<Constraint<Pitch>>());
		}
	}

	public void addLyricConstraint(int lineNum, Constraint<Lyric> constraint) {
		lyricConstraints.get(lineNum).add(constraint);
	}
	
	public void addChordConstraint(int lineNum, Constraint<Chord> constraint) {
		chordConstraints.get(lineNum).add(constraint);
	}
	
	public void addRhythmConstraint(int lineNum, Constraint<RhythmSegment> constraint) {
		rhythmConstraints.get(lineNum).add(constraint);
	}
	
	public void addPitchConstraint(int lineNum, Constraint<Pitch> constraint) {
		pitchConstraints.get(lineNum).add(constraint);
	}

	public String toString()
	{
		StringBuilder str = new StringBuilder();
		
		for (int i = 0; i < linesPerSegment; i++) {
			str.append("Line ");
			str.append(i+1);
			str.append(":\n");
			for (Constraint<Chord> constraint : chordConstraints.get(i)) {
				str.append("\tThe CHORD ");
				str.append(constraint);
				str.append('\n');
			}
			for (Constraint<Lyric> constraint : lyricConstraints.get(i)) {
				str.append("\tThe LYRIC ");
				str.append(constraint);
				str.append('\n');
			}
			for (Constraint<RhythmSegment> constraint : rhythmConstraints.get(i)) {
				str.append("\tThe RHYTHM ");
				str.append(constraint);
				str.append('\n');
			}
			for (Constraint<Pitch> constraint : pitchConstraints.get(i)) {
				str.append("\tThe PITCH ");
				str.append(constraint);
				str.append('\n');
			}
			str.append('\n');
		}
		
		return str.toString();
	}
	
}
