package segmentstructure;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import composition.Measure;
import constraint.Constraint;
import globalstructure.SegmentType;

public abstract class SegmentStructureEngineer {

	public abstract SegmentStructure defineSegmentStructure(SegmentType segmentType);

	public abstract List<Measure> instantiateSegmentStructure(SegmentType segmentType, SegmentStructure segmentStructure, boolean lastOfKind,
			boolean lastSegment);
	
	protected List<Measure> instantiateExactSegmentStructure(SegmentType segmentType, SegmentStructure segmentStructure, 
			boolean lastOfKind, boolean lastSegment) {
		List<Measure> segmentMeasures = new ArrayList<Measure>();

		for (int i = 0; i < segmentStructure.getMeasureCount(); i++) {
			Measure instantiatedMeasure = new Measure();
			Measure measureStructure = segmentStructure.measures.get(i);
			
			instantiatedMeasure.divisionsPerQuarterNote = measureStructure.divisionsPerQuarterNote;
			instantiatedMeasure.key = measureStructure.key;
			instantiatedMeasure.time = measureStructure.time;

			for (Entry<Double, List<Constraint>> constraint: measureStructure.getConstraints().entrySet()) {
				instantiatedMeasure.addAllConstraints(constraint.getKey(), constraint.getValue());
			}
			
			segmentMeasures.add(instantiatedMeasure);
		}		
		
		return segmentMeasures;
	}

}
