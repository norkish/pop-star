package harmony;

import java.util.List;

public class ProgressionSegment {

	private List<List<Chord>> chordLines;

	public ProgressionSegment(List<List<Chord>> lyricLines) {
		this.chordLines = lyricLines;
	}

	public List<Chord> getLine(int lineNum) {
		return chordLines.get(lineNum);
	}
}
