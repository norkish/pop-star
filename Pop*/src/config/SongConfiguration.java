package config;

import globalstructure.StructureSource;
import harmony.HarmonySource;
import inspiration.InspirationSource;
import lyrics.LyricalSource;
import melody.MelodySource;
import pitch.PitchSource;
import rhythm.RhythmSource;
import segmentstructure.SegmentStructureSource;

public class SongConfiguration {

	public InspirationSource inspirationSource = InspirationSource.UNSET;
	public StructureSource globalStructureSource = StructureSource.UNSET;
	public SegmentStructureSource substructureSource = SegmentStructureSource.UNSET;
	public LyricalSource lyricSource = LyricalSource.UNSET;
	public HarmonySource harmonySource = HarmonySource.UNSET;
	public RhythmSource rhythmSource = RhythmSource.UNSET;
	public PitchSource pitchSource = PitchSource.UNSET;
	public MelodySource melodySource = MelodySource.UNSET;
	
	public static long randSeed = 0;
}
