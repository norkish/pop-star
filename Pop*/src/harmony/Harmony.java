package harmony;

import java.util.Map;

import globalstructure.SegmentType;

public class Harmony {

	private Map<SegmentType, ProgressionSegment[]> progressions = null;

	public Map<SegmentType, ProgressionSegment[]> getProgressions() {
		return progressions;
	}

	public void setProgressions(Map<SegmentType, ProgressionSegment[]> progressions) {
		this.progressions  = progressions;
	}

}
