package melody;

import pitch.Pitches;
import rhythm.Rhythms;

public class Melody {

	private Rhythms rhythms = null;
	private Pitches pitches = null;

	public void setRhythms(Rhythms rhythms) {
		this.rhythms  = rhythms;
	}

	public void setPitches(Pitches pitches) {
		this.pitches  = pitches;
	}
	
}
