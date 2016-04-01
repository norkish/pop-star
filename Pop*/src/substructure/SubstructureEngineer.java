package substructure;

import java.util.HashMap;
import java.util.Map;

import globalstructure.GlobalStructure;
import globalstructure.SegmentType;
import utils.Utils;

public abstract class SubstructureEngineer {

	public Map<SegmentType, Substructure[]> defineSubstructure(GlobalStructure structure)
	{
		//This data structure contains, for each type of segment in the structure, a list of substructures
		// representing the repetitions of that segment type (e.g., a list of verse substructures for the 
		// segment type verse. This is to allow for order-dependent variation between verses, choruses, etc. 
		Map<SegmentType, Substructure[]> substructures = initSubstructures(structure);
		
		for (SegmentType segmentKey : substructures.keySet()) {
			Substructure[] segmentStructures = substructures.get(segmentKey);
			segmentStructures[0] = defineSubstructure(segmentKey);
			for (int i = 1; i < segmentStructures.length; i++) {
				segmentStructures[i] = (Substructure) Utils.deepCopy(segmentStructures[i-1]);
				applyVariation(segmentStructures[i], segmentKey, i == (segmentStructures.length-1));
			} 
		}
		
		return substructures;
	}

	protected abstract void applyVariation(Substructure substructure, SegmentType segmentType, boolean isLast);

	protected abstract Substructure defineSubstructure(SegmentType segmentType);

	private Map<SegmentType, Substructure[]> initSubstructures(GlobalStructure structure) {
		Map<SegmentType, Integer> segmentCountByType = new HashMap<SegmentType, Integer>();
		
		for (SegmentType segment : structure.getGlobalStructure()) {
			Integer count = segmentCountByType.get(segment);
			if (count != null) {
				segmentCountByType.put(segment, count+1);
			}
			else
			{
				segmentCountByType.put(segment, 1);
			}
		}
		
		Map<SegmentType, Substructure[]> substructures = new HashMap<SegmentType, Substructure[]>();
		for (SegmentType segment : segmentCountByType.keySet()) {
			substructures.put(segment, new Substructure[segmentCountByType.get(segment)]);
		}
		
		return substructures;
	}
}
