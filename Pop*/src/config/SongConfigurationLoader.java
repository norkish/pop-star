package config;

import globalstructure.StructureSource;
import harmony.HarmonySource;
import inspiration.InspirationSource;
import lyrics.LyricalSource;
import melody.MelodySource;
import pitch.PitchSource;
import rhythm.RhythmSource;
import segmentstructure.SegmentStructureSource;

public class SongConfigurationLoader {

	public static SongConfiguration loadConfigurationFromFile() {
		throw new UnsupportedOperationException("Not implemented");
	}

	public static SongConfiguration loadRandomConfiguration() {
		SongConfiguration config = new SongConfiguration();
		
		config.inspirationSource = InspirationSource.randomInspirationSource();
		config.globalStructureSource = StructureSource.randomStructureSource();
		config.substructureSource = SegmentStructureSource.randomSubstructureSource();
		config.lyricSource = LyricalSource.randomLyricalSource();
		config.harmonySource = HarmonySource.randomHarmonySource();
		config.melodySource = MelodySource.randomMelodySource();
		config.rhythmSource = RhythmSource.randomRhythmSource();
		config.pitchSource = PitchSource.randomPitchSource();
		
		return config;
	}

	public static SongConfiguration loadConfigurationFromCommandline() {
		throw new UnsupportedOperationException("Not implemented");
	}

	public static SongConfiguration loadSimpleConfiguration() {
		SongConfiguration config = new SongConfiguration();
		
		config.inspirationSource = InspirationSource.RANDOM;
		config.globalStructureSource = StructureSource.FIXED;
		config.substructureSource = SegmentStructureSource.FIXED;
		config.lyricSource = LyricalSource.TEMPLATE;
		config.harmonySource = HarmonySource.MONOCHORD;
		config.melodySource = MelodySource.RANDOM;
		config.rhythmSource = RhythmSource.RANDOM;
		config.pitchSource = PitchSource.RANDOM;
		
		return config;
	}
	
	public static SongConfiguration loadTestConfiguration() {
		SongConfiguration config = new SongConfiguration();
		
		config.inspirationSource = InspirationSource.RANDOM;
		config.globalStructureSource = StructureSource.TEST;
		config.substructureSource = SegmentStructureSource.TEST;
		config.lyricSource = LyricalSource.TEST;
		config.harmonySource = HarmonySource.TEST;
		config.melodySource = MelodySource.TEST;
		config.rhythmSource = RhythmSource.TEST;
		config.pitchSource = PitchSource.TEST;
		
		return config;
	}

	public static SongConfiguration loadDistributionalConfiguration() {
		SongConfiguration config = new SongConfiguration();
		
		config.inspirationSource = InspirationSource.RANDOM; // TODO
		config.globalStructureSource = StructureSource.FIXED; // TODO: distributional
		config.substructureSource = SegmentStructureSource.FIXED; // TODO: distributional
		config.lyricSource = LyricalSource.TEMPLATE; // TODO: Lyrical ngram
		config.harmonySource = HarmonySource.SEGMENTSPECIFIC_HMM;
		config.melodySource = MelodySource.RANDOM; // TODO
		config.rhythmSource = RhythmSource.RANDOM; // TODO
		config.pitchSource = PitchSource.RANDOM; // TODO
		
		return config;
	}
}
