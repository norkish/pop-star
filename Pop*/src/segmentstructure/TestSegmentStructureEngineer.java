package segmentstructure;

import java.util.List;

import composition.Measure;
import condition.ExactBinaryMatch;
import condition.ExactUnaryMatch;
import condition.Rhyme;
import condition.StrongResolution;
import constraint.Constraint;
import data.MusicXMLParser.KeyMode;
import data.MusicXMLParser.NoteLyric;
import globalstructure.SegmentType;
import harmony.Chord;
import lyrics.Lyric;
import pitch.Pitch;

public class TestSegmentStructureEngineer extends SegmentStructureEngineer {


	@Override
	public SegmentStructure defineSegmentStructure(SegmentType segmentType) {
		
		// TODO: rethink this
		// the segment structure should be easily used to create measures (perhaps even it should be measures)
		int measureCount = 17;
		
		SegmentStructure segmentStructure = new SegmentStructure(measureCount, SegmentType.VERSE);
		segmentStructure.addDivisionsPerQuarterNote(0,2);
		segmentStructure.addKey(0,0,KeyMode.MAJOR);
		segmentStructure.addTime(0,3,4);
		
		segmentStructure.addConstraint(0, 2.0, new Constraint<NoteLyric>(new ExactUnaryMatch<NoteLyric>(new NoteLyric[]{new NoteLyric(null, "It's", false, false)}), true));
		
		segmentStructure.addConstraint(5, 1.5, new Constraint<NoteLyric>(new ExactUnaryMatch<NoteLyric>(new NoteLyric[]{new NoteLyric(null, "The", false, false)}), true));
		segmentStructure.addConstraint(8, 0.0, new Constraint<Chord>(new StrongResolution<Chord>(), false));
		
		segmentStructure.addConstraint(8, 2.0, new Constraint<NoteLyric>(new ExactUnaryMatch<NoteLyric>(new NoteLyric[]{new NoteLyric(null, "There's", false, false)}), true));
		for (int i = 9; i < 13; i++) {
			segmentStructure.addConstraint(i, 0.0, new Constraint<Chord>(new ExactBinaryMatch<Chord>(0,i), true));
		}
		for (int i = 13; i < 17; i++) {
			segmentStructure.addConstraint(i, 0.0, new Constraint<Pitch>(new ExactBinaryMatch<Pitch>(0, i), true));
		}
		
		segmentStructure.addConstraint(12, 2.0, new Constraint<NoteLyric>(new ExactUnaryMatch<NoteLyric>(new NoteLyric[]{new NoteLyric(null, "Making", false, false)}), true));
		segmentStructure.addConstraint(15, 0.0, new Constraint<NoteLyric>(new Rhyme<NoteLyric>(1, Constraint.FINAL_POSITION), true));
		segmentStructure.addConstraint(16, 0.0, new Constraint<Chord>(new StrongResolution<Chord>(), true));
		
		return segmentStructure;
	}

	public List<Measure> instantiateSegmentStructure(SegmentType segmentType, SegmentStructure segmentStructure, 
			boolean lastOfKind, boolean lastSegment) {
		return instantiateExactSegmentStructure(segmentType, segmentStructure, lastOfKind, lastSegment);
	}

}
