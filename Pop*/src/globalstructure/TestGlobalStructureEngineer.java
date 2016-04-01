package globalstructure;

public class TestGlobalStructureEngineer extends GlobalStructureEngineer {

	@Override
	public GlobalStructure generateStructure() {
		return new GlobalStructure(new SegmentType[]{
				SegmentType.VERSE
		});
	}

}
