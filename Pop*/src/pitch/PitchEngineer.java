package pitch;

import harmony.Harmony;
import inspiration.Inspiration;
import lyrics.Lyrics;
import structure.Structure;

public abstract class PitchEngineer {

	public abstract Pitches generatePitch(Inspiration inspiration, Structure structure, Lyrics lyrics, Harmony harmony);

}
