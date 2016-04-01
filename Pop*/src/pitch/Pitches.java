package pitch;

import java.util.Map;

import globalstructure.SegmentType;

public class Pitches {

	Map<SegmentType, PitchSegment[]> pitchesByLine;

	public Map<SegmentType, PitchSegment[]> getPitchesBySegment() {
		return pitchesByLine;
	}

	public void setPitchesByLine(Map<SegmentType, PitchSegment[]> pitchesByLine) {
		this.pitchesByLine = pitchesByLine;
	}

}
