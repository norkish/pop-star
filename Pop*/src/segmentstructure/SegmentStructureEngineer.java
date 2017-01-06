package segmentstructure;

import java.util.List;

import composition.Measure;
import globalstructure.SegmentType;

public abstract class SegmentStructureEngineer {

	public abstract SegmentStructure defineSegmentStructure(SegmentType segmentType);

	public abstract List<Measure> instantiateSegmentStructure(SegmentType segmentType, SegmentStructure segmentStructure, boolean lastOfKind,
			boolean lastSegment);

}
