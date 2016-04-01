package structure;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import globalstructure.GlobalStructure;
import globalstructure.SegmentType;
import substructure.SegmentSubstructure;
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
			 nextSegmentIdx++;
			return new Pair<SegmentType, Integer>(type, typeCt);
		}

	}
	
	public class SegmentIterator<T> implements Iterator<Triple<SegmentType, Integer, SegmentSubstructure>> {

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
		public Triple<SegmentType, Integer, SegmentSubstructure> next() {
			SegmentType type = globalStructure.get(nextSegmentIdx);
			int typeCt = currIdx.get(type);
			SegmentSubstructure substruct = substructure.get(type)[typeCt];
			currIdx.put(type, typeCt+1);
			nextSegmentIdx++;
			return new Triple<SegmentType, Integer, SegmentSubstructure>(type, typeCt,substruct);
		}

	}

	private GlobalStructure globalStructure = null;
	private Map<SegmentType, SegmentSubstructure[]> substructure = null;

	public void setGlobalStructure(GlobalStructure globalStructure) {
		this.globalStructure = globalStructure;
	}

	public void setSubstructure(Map<SegmentType, SegmentSubstructure[]> substructure) {
		this.substructure = substructure;
	}

	public Map<SegmentType, SegmentSubstructure[]> getSubstructure() {
		return substructure;
	}
}
