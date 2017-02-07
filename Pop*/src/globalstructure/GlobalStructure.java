package globalstructure;

import java.util.Arrays;
import java.util.Iterator;

public class GlobalStructure implements Iterable<SegmentType>{

	public static final String GLOBAL_STRUCTURE_ANNOTATIONS_DIR = "wikifonia_global_structure_annotations";

	private SegmentType[] structure;
	
	public GlobalStructure(SegmentType[] structure) {
		this.structure = structure;
	}

	public GlobalStructure(String structureStr) {
		this.structure = new SegmentType[structureStr.length()];
		for (int i = 0; i < structureStr.length(); i++) {
			structure[i] = SegmentType.valueOf(structureStr.charAt(i));
		}
	}

	public SegmentType[] getGlobalStructure() {
		return structure;
	}

	public int size() {
		return structure.length;
	}

	public SegmentType get(int nextSegmentIdx) {
		return structure[nextSegmentIdx];
	}

	@Override
	public Iterator<SegmentType> iterator() {
		return Arrays.asList(structure).iterator();
	}

	public int lastIndexOf(SegmentType segmentType) {
		int idx = structure.length-1;
		
		for (; idx >= 0; idx--) {
			if (structure[idx] == segmentType)
				return idx;
		}
		
		return idx;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(structure);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof GlobalStructure))
			return false;
		GlobalStructure other = (GlobalStructure) obj;
		if (!Arrays.equals(structure, other.structure))
			return false;
		return true;
	}
}
