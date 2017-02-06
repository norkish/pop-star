package segmentstructure;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import composition.Measure;
import constraint.Constraint;
import data.MusicXMLParser.Key;
import data.MusicXMLParser.NoteLyric;
import data.MusicXMLParser.Time;
import globalstructure.SegmentType;
import utils.Utils;

public abstract class SegmentStructureEngineer {

	public abstract SegmentStructure defineSegmentStructure(SegmentType segmentType);

	public abstract List<Measure> instantiateSegmentStructure(SegmentType segmentType, SegmentStructure segmentStructure, boolean lastOfKind,
			boolean lastSegment);
	
	protected List<Measure> instantiateExactSegmentStructure(SegmentType segmentType, SegmentStructure segmentStructure, 
			boolean lastOfKind, boolean lastSegment) {
		List<Measure> segmentMeasures = new ArrayList<Measure>();

		int currDivsPerQuarter = -1;
		Key currKey = null;
		Time currTime = null;
		
		System.out.println("Instantiating segment of type " + segmentType + " with " + segmentStructure.getMeasureCount() + " measures");
		for (int i = 0; i < segmentStructure.getMeasureCount(); i++) {
			Measure measureStructure = segmentStructure.measures.get(i);
			Measure instantiatedMeasure = new Measure(measureStructure.segmentType,measureStructure.offsetWithinSegment);
			
			if (measureStructure.divisionsPerQuarterNote != -1)
				currDivsPerQuarter = measureStructure.divisionsPerQuarterNote;
			if (measureStructure.key != null)
				currKey = measureStructure.key;
			if (measureStructure.time != null)
				currTime = measureStructure.time;

			instantiatedMeasure.divisionsPerQuarterNote = currDivsPerQuarter;
			instantiatedMeasure.key = currKey;
			instantiatedMeasure.time = currTime;

			for (Entry<Double, List<Constraint>> offsetConstraints: measureStructure.getConstraints().entrySet()) {
				List<Constraint> constraints = offsetConstraints.getValue();
				System.out.println("\tAt measure " + i + ", beat " + offsetConstraints.getKey() + ", " + constraints);
				for (Constraint constraint : constraints) {
					Constraint<NoteLyric> deepCopiedConstraint = (Constraint<NoteLyric>) Utils.deepCopy(constraint);
					instantiatedMeasure.addConstraint(offsetConstraints.getKey(), deepCopiedConstraint);
				}
			}
			
			segmentMeasures.add(instantiatedMeasure);
		}		
		
		return segmentMeasures;
	}

}
