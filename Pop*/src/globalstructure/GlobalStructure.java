package globalstructure;

public class GlobalStructure {

	private SegmentType[] structure;
	
	public GlobalStructure(SegmentType[] structure) {
		this.structure = structure;
	}

	public SegmentType[] getGlobalStructure() {
		return structure;
	}
}
