package rhythm;

import java.util.Map;

import globalstructure.SegmentType;

public class Rhythms {

	Map<SegmentType, RhythmSegment[]> rhythmBySegment;
	
	public void setRhythmsByLine(Map<SegmentType, RhythmSegment[]> rhythmsByLine) {
		this.rhythmBySegment = rhythmsByLine;
	}

	public Map<SegmentType, RhythmSegment[]> getRhythmBySegment() {
		return rhythmBySegment;
	}

}
