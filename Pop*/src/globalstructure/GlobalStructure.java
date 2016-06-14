package globalstructure;

public class GlobalStructure {

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

	public int length() {
		return structure.length;
	}

	public SegmentType get(int nextSegmentIdx) {
		return structure[nextSegmentIdx];
	}
}
