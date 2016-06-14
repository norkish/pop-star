package config;

import globalstructure.StructureSource;
import harmony.HarmonySource;
import inspiration.InspirationSource;
import lyrics.LyricalSource;
import pitch.PitchSource;
import rhythm.RhythmSource;
import substructure.SubstructureSource;

public class SongConfigurationLoader {

	public static SongConfiguration loadConfigurationFromFile() {
		throw new UnsupportedOperationException("Not implemented");
	}

	public static SongConfiguration loadRandomConfiguration() {
		SongConfiguration config = new SongConfiguration();
		
		config.inspirationSource = InspirationSource.randomInspirationSource();
		config.globalStructureSource = StructureSource.randomStructureSource();
		config.substructureSource = SubstructureSource.randomSubstructureSource();
		config.lyricSource = LyricalSource.randomLyricalSource();
		config.harmonySource = HarmonySource.randomHarmonySource();
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
		config.substructureSource = SubstructureSource.FIXED;
		config.lyricSource = LyricalSource.TEMPLATE;
		config.harmonySource = HarmonySource.MONOCHORD;
		config.rhythmSource = RhythmSource.RANDOM;
		config.pitchSource = PitchSource.RANDOM;
		
		return config;
	}
	
	public static SongConfiguration loadTestConfiguration() {
		SongConfiguration config = new SongConfiguration();
		
		config.inspirationSource = InspirationSource.RANDOM;
		config.globalStructureSource = StructureSource.TEST;
		config.substructureSource = SubstructureSource.TEST;
		config.lyricSource = LyricalSource.TEST;
		config.harmonySource = HarmonySource.TEST;
		config.rhythmSource = RhythmSource.TEST;
		config.pitchSource = PitchSource.TEST;
		
		return config;
	}

	public static SongConfiguration loadDistributionalConfiguration() {
		SongConfiguration config = new SongConfiguration();
		
		config.inspirationSource = InspirationSource.RANDOM; // TODO
		config.globalStructureSource = StructureSource.DISTRIBUTION;
		config.substructureSource = SubstructureSource.DISTRIBUTION;
		config.lyricSource = LyricalSource.LYRICAL_NGRAM;
		config.harmonySource = HarmonySource.SEGMENTSPECIFIC_HMM;
		config.rhythmSource = RhythmSource.TEST; // TODO
		config.pitchSource = PitchSource.TEST; // TODO
		
		return config;
	}
}
