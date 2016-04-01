package pitch;

import java.util.List;

public class PitchSegment {

	private List<List<Pitch>> pitchLines;

	public PitchSegment(List<List<Pitch>> lyricLines) {
		this.pitchLines = lyricLines;
	}

	public List<Pitch> getLine(int lineNum) {
		return pitchLines.get(lineNum);
	}
}
