package structure;

import java.util.Iterator;
import java.util.Map;

import globalstructure.GlobalStructure;
import globalstructure.SegmentType;
import substructure.Substructure;
import utils.Pair;

public class Structure {

	public class SegmentIterator<T> implements Iterator<Pair<SegmentType, Integer>> {

		//TODO!!!
		
		@Override
		public boolean hasNext() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public Pair<SegmentType, Integer> next() {
			// TODO Auto-generated method stub
			return null;
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
