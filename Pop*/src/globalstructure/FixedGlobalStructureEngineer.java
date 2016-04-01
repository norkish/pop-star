package globalstructure;

public class FixedGlobalStructureEngineer extends GlobalStructureEngineer {

	@Override
	public GlobalStructure generateStructure() {
		return new GlobalStructure(new SegmentType[]{
				SegmentType.INTRO,
				SegmentType.VERSE,
				SegmentType.CHORUS,
				SegmentType.VERSE,
				SegmentType.CHORUS,
				SegmentType.BRIDGE,
				SegmentType.CHORUS,
				SegmentType.OUTRO
		});
	}

}
