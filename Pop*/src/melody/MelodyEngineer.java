package melody;

import harmony.Harmony;
import inspiration.Inspiration;
import lyrics.Lyrics;
import pitch.PitchEngineer;
import pitch.Pitches;
import rhythm.RhythmEngineer;
import rhythm.Rhythms;
import structure.Structure;

public class MelodyEngineer {

	private RhythmEngineer rhythmEngineer = null;
	private PitchEngineer pitchEngineer = null;
	
	public void setRhythmEngineer(RhythmEngineer rhythmEngineer) {
		this.rhythmEngineer = rhythmEngineer;
	}

	public void setPitchEngineer(PitchEngineer pitchEngineer) {
		this.pitchEngineer = pitchEngineer;
	}

	public Melody generateMelody(Inspiration inspiration, Structure structure, Lyrics lyrics, Harmony harmony) {
		Melody melody = new Melody();
		
		Rhythms rhythms = rhythmEngineer.generateRhythm(inspiration, structure, lyrics);
		melody.setRhythms(rhythms);
		
		Pitches pitches = pitchEngineer.generatePitch(inspiration, structure, lyrics, harmony);
		melody.setPitches(pitches);
		
		return melody;
	}

}
