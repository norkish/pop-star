package rhythm;

import java.util.List;

public class RhythmSegment {

	private List<List<Rhythm>> rhythmLines;

	public RhythmSegment(List<List<Rhythm>> lyricLines) {
		this.rhythmLines = lyricLines;
	}

	public List<Rhythm> getLine(int lineNum) {
		return rhythmLines.get(lineNum);
	}
}
