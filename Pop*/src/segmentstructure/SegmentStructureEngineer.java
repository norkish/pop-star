package segmentstructure;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import composition.Measure;
import constraint.Constraint;
import data.MusicXMLParser.Key;
import data.MusicXMLParser.Time;
import globalstructure.SegmentType;

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

			for (Entry<Double, List<Constraint>> constraint: measureStructure.getConstraints().entrySet()) {
				instantiatedMeasure.addAllConstraints(constraint.getKey(), constraint.getValue());
			}
			
			segmentMeasures.add(instantiatedMeasure);
		}		
		
		return segmentMeasures;
	}

}
