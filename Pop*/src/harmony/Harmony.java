package harmony;

import java.util.Map;

import globalstructure.SegmentType;

public class Harmony {

	private Map<SegmentType, Progression[]> progressions = null;

	public void setProgression(Map<SegmentType, Progression[]> progressions) {
		this.progressions  = progressions;
	}

}
