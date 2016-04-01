package rhythm;

import inspiration.Inspiration;
import lyrics.Lyrics;
import structure.Structure;

public abstract class RhythmEngineer {

	public abstract Rhythms generateRhythm(Inspiration inspiration, Structure structure, Lyrics lyrics);

}
