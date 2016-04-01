package lyrics;

import java.util.List;

public class LyricSegment {

	private List<List<Lyric>> lyricLines;

	public LyricSegment(List<List<Lyric>> lyricLines) {
		this.lyricLines = lyricLines;
	}

	public List<Lyric> getLine(int lineNum) {
		return lyricLines.get(lineNum);
	}
}
