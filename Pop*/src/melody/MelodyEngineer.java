package melody;

import composition.Score;
import inspiration.Inspiration;

public abstract class MelodyEngineer {

	// Legacy Code
//	private RhythmEngineer rhythmEngineer = null;
//	private PitchEngineer pitchEngineer = null;
//	
//	public void setRhythmEngineer(RhythmEngineer rhythmEngineer) {
//		this.rhythmEngineer = rhythmEngineer;
//	}
//
//	public void setPitchEngineer(PitchEngineer pitchEngineer) {
//		this.pitchEngineer = pitchEngineer;
//	}

	public abstract void addMelody(Inspiration inspiration, Score score);

}
