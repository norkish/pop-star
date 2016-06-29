package lyrics;

import java.io.Serializable;
import java.util.List;

public class LyricSegment implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private List<List<Lyric>> lyricLines;

	public LyricSegment(List<List<Lyric>> lyricLines) {
		this.lyricLines = lyricLines;
	}

	public List<Lyric> getLine(int lineNum) {
		return lyricLines.get(lineNum);
	}
}
