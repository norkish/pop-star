package segmentstructure;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import composition.Measure;
import condition.ExactBinaryMatch;
import condition.ExactUnaryMatch;
import condition.Rhyme;
import condition.StrongResolution;
import constraint.Constraint;
import data.MusicXML.KeyMode;
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
		
		SegmentStructure segmentStructure = new SegmentStructure(measureCount);
		segmentStructure.addDivisionsPerQuarterNote(0,2);
		segmentStructure.addKey(0,0,KeyMode.MAJOR);
		segmentStructure.addTime(0,3,4);
		
		segmentStructure.addConstraint(0, 2.0, new Constraint<Lyric>(0, new ExactUnaryMatch<Lyric>(new Lyric[]{new Lyric("It's")}), true));
		
		segmentStructure.addConstraint(5, 1.5, new Constraint<Lyric>(0, new ExactUnaryMatch<Lyric>(new Lyric[]{new Lyric("The")}), true));
		segmentStructure.addConstraint(8, 0.0, new Constraint<Chord>(Constraint.FINAL_POSITION, new StrongResolution<Chord>(), false));
		
		segmentStructure.addConstraint(8, 2.0, new Constraint<Lyric>(0, new ExactUnaryMatch<Lyric>(new Lyric[]{new Lyric("There's")}), true));
		for (int i = 9; i < 13; i++) {
			segmentStructure.addConstraint(i, 0.0, new Constraint<Chord>(i, new ExactBinaryMatch<Chord>(0,i), true));
		}
		for (int i = 13; i < 17; i++) {
			segmentStructure.addConstraint(i, 0.0, new Constraint<Pitch>(i, new ExactBinaryMatch<Pitch>(0, i), true));
		}
		
		segmentStructure.addConstraint(12, 2.0, new Constraint<Lyric>(0, new ExactUnaryMatch<Lyric>(new Lyric[]{new Lyric("Making")}), true));
		segmentStructure.addConstraint(15, 0.0, new Constraint<Lyric>(Constraint.FINAL_POSITION, new Rhyme<Lyric>(1, Constraint.FINAL_POSITION), true));
		segmentStructure.addConstraint(16, 0.0, new Constraint<Chord>(Constraint.FINAL_POSITION, new StrongResolution<Chord>(), true));
		
		return segmentStructure;
	}

	@Override
	public List<Measure> instantiateSegmentStructure(SegmentType segmentType, SegmentStructure segmentStructure, 
			boolean lastOfKind, boolean lastSegment) {
		List<Measure> segmentMeasures = new ArrayList<Measure>();

		for (int i = 0; i < segmentStructure.getMeasureCount(); i++) {
			Measure instantiatedMeasure = new Measure();
			Measure measureStructure = segmentStructure.measures.get(i);
			
			for (Entry<Double, List<Constraint>> constraint: measureStructure.getConstraints().entrySet()) {
				instantiatedMeasure.divisions = measureStructure.divisions;
				instantiatedMeasure.key = measureStructure.key;
				instantiatedMeasure.time = measureStructure.time;
				instantiatedMeasure.addAllConstraints(constraint.getKey(), constraint.getValue());
			}
			
			segmentMeasures.add(instantiatedMeasure);
		}		
		
		return segmentMeasures;
	}

}
