package lyrics;

import java.util.Map;

import globalstructure.SegmentType;

public class Lyrics {

	private Map<SegmentType, LyricSegment[]> lyricsBySegment = null;

	public void setLyricsBySegment(Map<SegmentType, LyricSegment[]> lyricsBySegment) {
		this.lyricsBySegment  = lyricsBySegment;
	}

	public Map<SegmentType, LyricSegment[]> getLyricsBySegment() {
		return lyricsBySegment;
	}

}
