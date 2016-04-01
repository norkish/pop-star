package structure;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import globalstructure.GlobalStructure;
import globalstructure.SegmentType;
import substructure.Substructure;
import utils.Pair;
import utils.Triple;

public class Structure {

	public class SegmentTypeIterator<T> implements Iterator<Pair<SegmentType, Integer>> {

		Map<SegmentType, Integer> currIdx;
		int nextSegmentIdx;
		
		public SegmentTypeIterator()
		{
			currIdx = new HashMap<SegmentType, Integer>();
			for (SegmentType segment : substructure.keySet()) {
				currIdx.put(segment, 0);
			}
			nextSegmentIdx = 0;
		}
		
		@Override
		public boolean hasNext() {
			return (nextSegmentIdx < globalStructure.length());
		}

		@Override
		public Pair<SegmentType, Integer> next() {
			SegmentType type = globalStructure.get(nextSegmentIdx);
			int typeCt = currIdx.get(type);
			currIdx.put(type, typeCt+1);
			return new Pair<SegmentType, Integer>(type, typeCt);
		}

	}
	
	public class SegmentIterator<T> implements Iterator<Triple<SegmentType, Integer, Substructure>> {

		Map<SegmentType, Integer> currIdx;
		int nextSegmentIdx;
		
		public SegmentIterator()
		{
			currIdx = new HashMap<SegmentType, Integer>();
			for (SegmentType segment : substructure.keySet()) {
				currIdx.put(segment, 0);
			}
			nextSegmentIdx = 0;
		}
		
		@Override
		public boolean hasNext() {
			return (nextSegmentIdx < globalStructure.length());
		}

		@Override
		public Triple<SegmentType, Integer, Substructure> next() {
			SegmentType type = globalStructure.get(nextSegmentIdx);
			int typeCt = currIdx.get(type);
			Substructure substruct = substructure.get(type)[typeCt];
			currIdx.put(type, typeCt+1);
			return new Triple<SegmentType, Integer, Substructure>(type, typeCt,substruct);
		}

	}

	private GlobalStructure globalStructure = null;
	private Map<SegmentType, Substructure[]> substructure = null;

	public void setGlobalStructure(GlobalStructure globalStructure) {
		this.globalStructure = globalStructure;
	}

	public void setSubstructure(Map<SegmentType, Substructure[]> substructure) {
		this.substructure = substructure;
	}

	public Map<SegmentType, Substructure[]> getSubstructure() {
		return substructure;
	}
}
