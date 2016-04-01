package config;

import globalstructure.StructureSource;
import harmony.HarmonySource;
import inspiration.InspirationSource;
import lyrics.LyricalSource;
import pitch.PitchSource;
import rhythm.RhythmSource;
import substructure.SubstructureSource;

public class SongConfiguration {

	public InspirationSource inspirationSource = InspirationSource.UNSET;
	public StructureSource globalStructureSource = StructureSource.UNSET;
	public SubstructureSource substructureSource = SubstructureSource.UNSET;
	public LyricalSource lyricSource = LyricalSource.UNSET;
	public HarmonySource harmonySource = HarmonySource.UNSET;
	public RhythmSource rhythmSource = RhythmSource.UNSET;
	public PitchSource pitchSource = PitchSource.UNSET;

}
