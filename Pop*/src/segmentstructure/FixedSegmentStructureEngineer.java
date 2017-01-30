package segmentstructure;

import java.util.List;

import composition.Measure;
import condition.ExactBinaryMatch;
import condition.Rhyme;
import condition.StrongResolution;
import constraint.Constraint;
import data.MusicXMLParser.Harmony;
import data.MusicXMLParser.KeyMode;
import data.MusicXMLParser.Note;
import globalstructure.SegmentType;
import lyrics.Lyric;

public class FixedSegmentStructureEngineer extends SegmentStructureEngineer {

	@Override
	public SegmentStructure defineSegmentStructure(SegmentType segmentType) {
		switch(segmentType){
		case BRIDGE:
			return defineBridgeSegmentStructure();
		case CHORUS:
			return defineChorusSegmentStructure();
		case INTRO:
			return defineIntroSegmentStructure();
		case OUTRO:
			return defineOutroSegmentStructure();
		case VERSE:
			return defineVerseSegmentStructure();
		case INTERLUDE:
			return defineInterludeSegmentStructure();
		case PRECHORUS:
			assert false: "didn't define prechorus yet";
			break;
		default:
			break;
		}
		return null;
	}

	private SegmentStructure defineInterludeSegmentStructure() {
		int measureCount = 4;
		
		SegmentStructure segmentStructure = new SegmentStructure(measureCount, SegmentType.INTERLUDE);
		segmentStructure.addDivisionsPerQuarterNote(0,4);
		segmentStructure.addKey(0,0,KeyMode.MAJOR);
		segmentStructure.addTime(0,4,4);
		
		segmentStructure.addConstraint(2, Constraint.ALL_POSITIONS, new Constraint<Harmony>(2345, new ExactBinaryMatch<Harmony>(0, Constraint.ALL_POSITIONS), true));
		
		return segmentStructure;
	}

	private SegmentStructure defineVerseSegmentStructure() {
		int measureCount = 16;
		
		SegmentStructure segmentStructure = new SegmentStructure(measureCount, SegmentType.VERSE);
		segmentStructure.addDivisionsPerQuarterNote(0,4);
		segmentStructure.addKey(0,0,KeyMode.MAJOR);
		segmentStructure.addTime(0,4,4);

		// lyric
		segmentStructure.addConstraint(7, 0.0, new Constraint<Lyric>(Constraint.FINAL_POSITION, new Rhyme<Lyric>(3, 0.0), true));
		segmentStructure.addConstraint(15, 0.0, new Constraint<Lyric>(Constraint.FINAL_POSITION, new Rhyme<Lyric>(11, 0.0), true));
		
		// harmony and melody
		for (int i = 8; i < 14; i++) {
			segmentStructure.addConstraint(i, Constraint.ALL_POSITIONS, new Constraint<Harmony>(2345, new ExactBinaryMatch<Harmony>(i-8, Constraint.ALL_POSITIONS), true));
			segmentStructure.addConstraint(i, Constraint.ALL_POSITIONS, new Constraint<Note>(2345, new ExactBinaryMatch<Note>(i-8, Constraint.ALL_POSITIONS), true));
		}
		
		return segmentStructure;
	}

	private SegmentStructure defineOutroSegmentStructure() {
		int measureCount = 4;
		
		SegmentStructure segmentStructure = new SegmentStructure(measureCount, SegmentType.OUTRO);
		segmentStructure.addDivisionsPerQuarterNote(0,4);
		segmentStructure.addKey(0,0,KeyMode.MAJOR);
		segmentStructure.addTime(0,4,4);
		
		segmentStructure.addConstraint(2, Constraint.ALL_POSITIONS, new Constraint<Harmony>(2345, new ExactBinaryMatch<Harmony>(0, Constraint.ALL_POSITIONS), true));
		segmentStructure.addConstraint(3, Constraint.ALL_POSITIONS, new Constraint<Harmony>(2345, new StrongResolution<Harmony>(), true));
		// TODO:Extrasegmental matching ? Outro should match the intro somewhat
		
		return segmentStructure;
	}

	private SegmentStructure defineIntroSegmentStructure() {
		int measureCount = 4;
		
		
		SegmentStructure segmentStructure = new SegmentStructure(measureCount, SegmentType.INTRO);
		segmentStructure.addDivisionsPerQuarterNote(0,4);
		segmentStructure.addKey(0,0,KeyMode.MAJOR);
		segmentStructure.addTime(0,4,4);
		
		segmentStructure.addConstraint(2, Constraint.ALL_POSITIONS, new Constraint<Harmony>(2345, new ExactBinaryMatch<Harmony>(0, Constraint.ALL_POSITIONS), true));
		
		return segmentStructure;
	}

	private SegmentStructure defineChorusSegmentStructure() {
		int measureCount = 16;
		
		SegmentStructure segmentStructure = new SegmentStructure(measureCount, SegmentType.CHORUS);
		segmentStructure.addDivisionsPerQuarterNote(0,4);
		segmentStructure.addKey(0,0,KeyMode.MAJOR);
		segmentStructure.addTime(0,4,4);
		
		// lyric
		segmentStructure.addConstraint(7, 0.0, new Constraint<Lyric>(Constraint.FINAL_POSITION, new Rhyme<Lyric>(3, 0.0), true));
		segmentStructure.addConstraint(11, 0.0, new Constraint<Lyric>(Constraint.FINAL_POSITION, new Rhyme<Lyric>(7, 0.0), true));
		segmentStructure.addConstraint(15, 0.0, new Constraint<Lyric>(Constraint.FINAL_POSITION, new Rhyme<Lyric>(11, 0.0), true));
		
		// harmony and melody
		for (int i = 8; i < 12; i++) {
			segmentStructure.addConstraint(i, Constraint.ALL_POSITIONS, new Constraint<Harmony>(2345, new ExactBinaryMatch<Harmony>(i-8, Constraint.ALL_POSITIONS), true));
			segmentStructure.addConstraint(i, Constraint.ALL_POSITIONS, new Constraint<Note>(2345, new ExactBinaryMatch<Note>(i-8, Constraint.ALL_POSITIONS), true));
		}
		segmentStructure.addConstraint(15, Constraint.ALL_POSITIONS, new Constraint<Harmony>(2345, new StrongResolution<Harmony>(), true));
		
		return segmentStructure;
	}

	private SegmentStructure defineBridgeSegmentStructure() {
		int measureCount = 8;
		
		SegmentStructure segmentStructure = new SegmentStructure(measureCount, SegmentType.BRIDGE);
		segmentStructure.addDivisionsPerQuarterNote(0,4);
		segmentStructure.addKey(0,0,KeyMode.MAJOR);
		segmentStructure.addTime(0,4,4);
		
		segmentStructure.addConstraint(5, 0.0, new Constraint<Lyric>(Constraint.FINAL_POSITION, new Rhyme<Lyric>(1, 0.0), true));
		segmentStructure.addConstraint(7, 0.0, new Constraint<Lyric>(Constraint.FINAL_POSITION, new Rhyme<Lyric>(3, 0.0), true));
		
		// TODO: constrain melody shape over chords? e.g., Just the way you are doesn't have exact copy but shape is same for first and second halves
		
		return segmentStructure;
	}

	@Override
	public List<Measure> instantiateSegmentStructure(SegmentType segmentType, SegmentStructure segmentStructure,
			boolean lastOfKind, boolean lastSegment) {
		return instantiateExactSegmentStructure(segmentType, segmentStructure, lastOfKind, lastSegment);
	}


}
