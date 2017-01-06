package manager;

import config.SongConfiguration;
import config.SongConfigurationLoader;
import globalstructure.DistributionalGlobalStructureEngineer;
import globalstructure.FixedGlobalStructureEngineer;
import globalstructure.GlobalStructureEngineer;
import globalstructure.MarkovGlobalStructureEngineer;
import globalstructure.TestGlobalStructureEngineer;
import harmony.HarmonyEngineer;
import harmony.MonochordHarmonyEngineer;
import harmony.PhraseDictHarmonyEngineer;
import harmony.SegmentSpecificHarmonyEngineer;
import harmony.TestHarmonyEngineer;
import inspiration.EnvironmentalInspirationEngineer;
import inspiration.InspirationEngineer;
import inspiration.RandomInspirationEngineer;
import inspiration.UserInspirationEngineer;
import lyrics.LyricTemplateEngineer;
import lyrics.LyricalEngineer;
import lyrics.NGramLyricEngineer;
import lyrics.NGramNonLyricEngineer;
import lyrics.TestLyricEngineer;
import main.ProgramArgs;
import melody.MelodyEngineer;
import melody.RandomMelodyEngineer;
import melody.TestMelodyEngineer;
import pitch.HMMPitchEngineer;
import pitch.IdiomaticPitchEngineer;
import pitch.PitchEngineer;
import pitch.RandomPitchEngineer;
import pitch.TestPitchEngineer;
import rhythm.LyricalStressRhythmEngineer;
import rhythm.PhraseDictRhythmEngineer;
import rhythm.RandomRhythmEngineer;
import rhythm.RhythmEngineer;
import rhythm.TestRhythmEngineer;
import segmentstructure.DistributionalSegmentStructureEngineer;
import segmentstructure.FixedSegmentStructureEngineer;
import segmentstructure.HierarchicalSegmentStructureEngineer;
import segmentstructure.SegmentStructureEngineer;
import segmentstructure.TestSegmentStructureEngineer;

public class Manager {

	private SongConfiguration config = null;

	public Manager() {
		switch (ProgramArgs.configurationSetting) {
		case FROM_FILE:
			config = SongConfigurationLoader.loadConfigurationFromFile();
			break;
		case RANDOM:
			config = SongConfigurationLoader.loadRandomConfiguration();
			break;
		case FROM_COMMANDLINE:
			config = SongConfigurationLoader.loadConfigurationFromCommandline();
			break;
		case SIMPLE:
			config = SongConfigurationLoader.loadSimpleConfiguration();
			break;
		case TEST:
			config = SongConfigurationLoader.loadTestConfiguration();
			break;
		case DISTRIBUTIONAL:
			config = SongConfigurationLoader.loadDistributionalConfiguration();
			break;
		default:
			throw new RuntimeException("Invalid configuration setting: " + config.globalStructureSource);
		}
	}

	public InspirationEngineer getInspirationEngineer() {
		InspirationEngineer engineer = null;

		switch (config.inspirationSource) {
		case RANDOM:
			engineer = new RandomInspirationEngineer();
			break;
		case USER:
			engineer = new UserInspirationEngineer();
			break;
		case ENVIRONMENT:
			engineer = new EnvironmentalInspirationEngineer();
			break;
		default:
			throw new RuntimeException("Invalid inspiration configuration: " + config.inspirationSource);
		}

		return engineer;
	}
	
	public GlobalStructureEngineer getGlobalStructureEngineer() {
		GlobalStructureEngineer engineer = null;

		switch (config.globalStructureSource) {
		case FIXED:
			engineer = new FixedGlobalStructureEngineer();
			break;
		case DISTRIBUTION:
			engineer = new DistributionalGlobalStructureEngineer();
			break;
		case MARKOV:
			engineer = new MarkovGlobalStructureEngineer();
			break;
		case TEST:
			engineer = new TestGlobalStructureEngineer();
			break;
		default:
			throw new RuntimeException("Invalid structure configuration: " + config.globalStructureSource);
		}

		return engineer;
	}

	public SegmentStructureEngineer getSegmentStructureEngineer() {
		SegmentStructureEngineer engineer = null;

		switch (config.substructureSource) {
		case FIXED:
			engineer = new FixedSegmentStructureEngineer();
			break;
		case DISTRIBUTION:
			engineer = new DistributionalSegmentStructureEngineer();
			break;
		case HIERARCHICAL:
			engineer = new HierarchicalSegmentStructureEngineer();
			break;
		case TEST:
			engineer = new TestSegmentStructureEngineer();
			break;
		default:
			throw new RuntimeException("Invalid substructure configuration: " + config.substructureSource);
		}

		return engineer;
	}

	public LyricalEngineer getLyricalEngineer() {
		LyricalEngineer engineer = null;

		switch (config.lyricSource) {
		case TEMPLATE:
			engineer = new LyricTemplateEngineer();
			break;
		case LYRICAL_NGRAM:
			engineer = new NGramLyricEngineer();
			break;
		case NON_LYRICAL_NGRAM:
			engineer = new NGramNonLyricEngineer();
			break;
		case TEST:
			engineer = new TestLyricEngineer();
			break;
		default:
			throw new RuntimeException("Invalid lyric configuration: " + config.lyricSource);
		}

		return engineer;
	}

	public HarmonyEngineer getHarmonyEngineer() {
		HarmonyEngineer engineer = null;

		switch (config.harmonySource) {
		case MONOCHORD:
			engineer = new MonochordHarmonyEngineer();
			break;
		case PHRASE_DICT:
			engineer = new PhraseDictHarmonyEngineer();
			break;
		case SEGMENTSPECIFIC_HMM:
			engineer = new SegmentSpecificHarmonyEngineer();
			break;
		case TEST:
			engineer = new TestHarmonyEngineer();
			break;
		default:
			throw new RuntimeException("Invalid harmony configuration: " + config.harmonySource);
		}

		return engineer;
	}

	public MelodyEngineer getMelodyEngineer() {
		MelodyEngineer melodyEngineer = null;
		
		switch (config.melodySource) {
		case RANDOM:
			melodyEngineer = new RandomMelodyEngineer();
			break;
		case TEST:
			melodyEngineer = new TestMelodyEngineer();
			break;
		default:
			throw new RuntimeException("Invalid harmony configuration: " + config.pitchSource);
		}
		
		return melodyEngineer;
	}

	public PitchEngineer getPitchEngineer() {
		PitchEngineer pitchEngineer = null;
		
		switch (config.pitchSource) {
		case RANDOM:
			pitchEngineer = new RandomPitchEngineer();
			break;
		case HMM:
			pitchEngineer = new HMMPitchEngineer();
			break;
		case IDIOMS:
			pitchEngineer = new IdiomaticPitchEngineer();
			break;
		case TEST:
			pitchEngineer = new TestPitchEngineer();
			break;
		default:
			throw new RuntimeException("Invalid harmony configuration: " + config.pitchSource);
		}
		return pitchEngineer;
	}

	public RhythmEngineer getRhythmEngineer() {
		RhythmEngineer rhythmEngineer = null;

		switch (config.rhythmSource) {
		case RANDOM:
			rhythmEngineer = new RandomRhythmEngineer();
			break;
		case PHRASE_DICT:
			rhythmEngineer = new PhraseDictRhythmEngineer();
			break;
		case LYRICAL_STRESS_PATTERNS:
			rhythmEngineer = new LyricalStressRhythmEngineer();
			break;
		case TEST:
			rhythmEngineer = new TestRhythmEngineer();
			break;
		default:
			throw new RuntimeException("Invalid harmony configuration: " + config.rhythmSource);
		}
		return rhythmEngineer;
	}
}
