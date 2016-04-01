package substructure;

import java.util.ArrayList;
import java.util.List;

import constraint.Constraint;
import harmony.Chord;
import lyrics.Lyric;
import pitch.PitchSegment;
import rhythm.RhythmSegment;


public class Substructure {
	
	public List<List<Constraint<Lyric>>> lyricConstraints = null;
	public List<List<Constraint<Chord>>> chordConstraints = null;
	public List<List<Constraint<RhythmSegment>>> rhythmConstraints = null;
	public List<List<Constraint<PitchSegment>>> pitchConstraints = null;
	public int linesPerSegment;
	public int measuresPerLine;
	public int minWordsPerLine;
	public int maxWordsPerLine;
	public int substructureRepetitions;
	public boolean relativeMinorKey;

	public Substructure(int linesPerSegment, int measuresPerLine, int minWordsPerLine, int maxWordsPerLine,
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
		pitchConstraints = new ArrayList<List<Constraint<PitchSegment>>>();
		
		for (int i = 0; i < linesPerSegment; i++) {
			lyricConstraints.add(new ArrayList<Constraint<Lyric>>());
			chordConstraints.add(new ArrayList<Constraint<Chord>>());
			rhythmConstraints.add(new ArrayList<Constraint<RhythmSegment>>());
			pitchConstraints.add(new ArrayList<Constraint<PitchSegment>>());
		}
	}

	public void addLyricConstraint(int lineNum, Constraint<Lyric> delayedMatchConstraint) {
		lyricConstraints.get(lineNum).add(delayedMatchConstraint);
	}
	
	public void addChordConstraint(int lineNum, Constraint<Chord> delayedMatchConstraint) {
		chordConstraints.get(lineNum).add(delayedMatchConstraint);
	}
	
	public void addRhythmConstraint(int lineNum, Constraint<RhythmSegment> delayedMatchConstraint) {
		rhythmConstraints.get(lineNum).add(delayedMatchConstraint);
	}
	
	public void addPitchConstraint(int lineNum, Constraint<PitchSegment> delayedMatchConstraint) {
		pitchConstraints.get(lineNum).add(delayedMatchConstraint);
	}

}
