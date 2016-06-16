package harmony;

import java.io.Serializable;
import java.util.List;

public class ProgressionSegment implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private List<List<Chord>> chordLines;

	public ProgressionSegment(List<List<Chord>> lyricLines) {
		this.chordLines = lyricLines;
	}

	public List<Chord> getLine(int lineNum) {
		return chordLines.get(lineNum);
	}
}
