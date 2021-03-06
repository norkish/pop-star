package main;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.w3c.dom.Document;

import automaton.MatchIteratorBuilderDFS;
import automaton.RegularConstraintApplier.StateToken;
import automaton.RhymeComparator;
import composition.Composition;
import composition.Measure;
import composition.Score;
import data.MusicXMLParser;
import data.MusicXMLParser.Harmony;
import data.MusicXMLParser.Key;
import data.MusicXMLParser.Note;
import data.MusicXMLParser.NoteLyric;
import data.MusicXMLParser.NoteTie;
import data.MusicXMLParser.Quality;
import data.MusicXMLParser.Root;
import data.MusicXMLParser.Syllabic;
import data.MusicXMLParser.Time;
import data.MusicXMLSummaryGenerator;
import data.ParsedMusicXMLObject;
import data.ParsedMusicXMLObject.MusicXMLAlignmentEvent;
import dbtb.constraint.AbsoluteStressConstraint;
import dbtb.constraint.ConditionedConstraint;
import dbtb.constraint.EndOfWordConstraint;
import dbtb.constraint.PartsOfSpeechConstraint;
import dbtb.constraint.StartOfWordConstraint;
import dbtb.constraint.StateConstraint;
import dbtb.data.DataLoader;
import dbtb.data.SyllableToken;
import dbtb.linguistic.paul.Phonetecizer;
import dbtb.linguistic.paul.StressedPhone;
import dbtb.linguistic.syntactic.Pos;
import dbtb.markov.BidirectionalVariableOrderPrefixIDMap;
import dbtb.markov.SparseVariableOrderMarkovModel;
import dbtb.markov.Token;
import dbtb.utils.Triple;
import globalstructure.StructureExtractor;
import globalstructureinference.GeneralizedGlobalStructureInferer;
import globalstructureinference.GeneralizedGlobalStructureInferer.GeneralizedGlobalStructureAlignmentParameterization;
import melody.MelodyEngineer;
import orchestrate.CompingMusicXMLOrchestrator;
import orchestrate.Orchestrator;
import pitch.Pitch;
import tabcomplete.main.TabDriver;
import utils.Utils;

public class AlnNHMMSongGeneratorNoNHMM {

	public static class PitchConstraint<T extends Token> implements StateConstraint<T> {
		
		int pitch;
		
		public PitchConstraint(int pitch) {
			this.pitch = pitch;
		}
		
		@Override
		public boolean isSatisfiedBy(LinkedList<T> state, int i) {
//			System.out.println("Checking if " + state.get(i).token.normalizedPitch + " in " + chordNotes);
			T t = state.get(i);
			if (t instanceof StateToken) {
//				if (!chordNotes.contains(((StateToken<PitchToken>)state.get(i)).token.normalizedPitch%12)) {
//					System.out.println("StateToken " + state.get(i) + " didn't satisfy " + this);
//				}
				return (pitch == Note.REST && ((StateToken<PitchToken>)state.get(i)).token.normalizedPitch == Note.REST) || (pitch != Note.REST && pitch == (((StateToken<PitchToken>)state.get(i)).token.normalizedPitch%12));
			}
			else
				return (pitch == Note.REST && ((PitchToken)state.get(i)).normalizedPitch == Note.REST) || (pitch != Note.REST && pitch == (((PitchToken)state.get(i)).normalizedPitch%12));
		}
		
		@Override
		public String toString() {
			return "" + pitch;
		}
	}

	public static class PitchInChordConstraint<T extends Token> implements StateConstraint<T> {

		Set<Integer> chordNotes = new HashSet<Integer>();
		
		public PitchInChordConstraint(Harmony harmony) {
			int root = (harmony.root.rootStep + 9)%12;
			chordNotes.add(root);
			final boolean[] pitchesOn = harmony.quality.getPitches();
			for (int i = 0; i < pitchesOn.length; i++) {
				if (pitchesOn[i]) chordNotes.add((root+i+1)%12);
			}
//			System.out.println("Harmony " + harmony.toShortString() + " with root " + root + " and pitches " + Arrays.toString(pitchesOn) + " -> " + chordNotes);
		}

		@Override
		public boolean isSatisfiedBy(LinkedList<T> state, int i) {
//			System.out.println("Checking if " + state.get(i).token.normalizedPitch + " in " + chordNotes);
			T t = state.get(i);
			if (t instanceof StateToken) {
//				if (!chordNotes.contains(((StateToken<PitchToken>)state.get(i)).token.normalizedPitch%12)) {
//					System.out.println("StateToken " + state.get(i) + " didn't satisfy " + this);
//				}
				return chordNotes.contains(((StateToken<PitchToken>)state.get(i)).token.normalizedPitch%12);
			}
			else
				return chordNotes.contains(((PitchToken)state.get(i)).normalizedPitch%12);
		}

		@Override
		public String toString() {
			return "" + chordNotes;
		}
	}

	public static class RhythmToken extends Token{

		private double durationInQuarterNotes;
		private double durationInQuarterNotesOfTiedNotes;
		private double beatsSinceOnsetInQuarterNotes;
		private double measureOffsetInQuarterNotes;
		private boolean isRest;

		public RhythmToken(double durationInQuarterNotes, double durationInQuarterNotesOfTiedNotes, double beatsSinceOnsetInQuarterNotes, double measureOffsetInQuarterNotes, boolean b) {
			this.durationInQuarterNotes = durationInQuarterNotes;
			this.durationInQuarterNotesOfTiedNotes = durationInQuarterNotesOfTiedNotes;
			this.beatsSinceOnsetInQuarterNotes = beatsSinceOnsetInQuarterNotes;
			this.measureOffsetInQuarterNotes = measureOffsetInQuarterNotes;
			this.isRest = b;
		}

		public boolean isOnset() {
			return beatsSinceOnsetInQuarterNotes == 0.0;
		}

		@Override
		public String toString() {
			return durationInQuarterNotes + ", " + durationInQuarterNotesOfTiedNotes + ", " + beatsSinceOnsetInQuarterNotes + ", " + measureOffsetInQuarterNotes + ", " + isRest;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			long temp;
			temp = Double.doubleToLongBits(measureOffsetInQuarterNotes);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			temp = Double.doubleToLongBits(beatsSinceOnsetInQuarterNotes);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			temp = Double.doubleToLongBits(durationInQuarterNotesOfTiedNotes);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			temp = Double.doubleToLongBits(durationInQuarterNotes);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			result = prime * result + (isRest ? 1231 : 1237);
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			RhythmToken other = (RhythmToken) obj;
			if (Double.doubleToLongBits(measureOffsetInQuarterNotes) != Double.doubleToLongBits(other.measureOffsetInQuarterNotes))
				return false;
			if (Double.doubleToLongBits(beatsSinceOnsetInQuarterNotes) != Double.doubleToLongBits(other.beatsSinceOnsetInQuarterNotes))
				return false;
			if (Double.doubleToLongBits(durationInQuarterNotesOfTiedNotes) != Double.doubleToLongBits(other.durationInQuarterNotesOfTiedNotes))
				return false;
			if (Double.doubleToLongBits(durationInQuarterNotes) != Double.doubleToLongBits(other.durationInQuarterNotes))
				return false;
			if (isRest != other.isRest)
				return false;
			return true;
		}
	}

	public static class HarmonyConstraint implements StateConstraint<HarmonyToken> {

		Harmony constrainedHarmony;
		
		public HarmonyConstraint(Harmony finalHarmony) {
			constrainedHarmony = finalHarmony;
		}

		@Override
		public boolean isSatisfiedBy(LinkedList<HarmonyToken> state, int i) {
			return state.get(i).harmony.equals(constrainedHarmony);
		}

	}
	
	public static class ChordsConstraint<T extends Token> implements StateConstraint<T> {
		
		Harmony[] harmonies;
		
		public ChordsConstraint(Harmony[] harmonies) {
			this.harmonies = harmonies;
		}
		
		@Override
		public boolean isSatisfiedBy(LinkedList<T> state, int i) {
			final T t2 = state.get(i);
			Token t;
			if (t2 instanceof StateToken)
				t = ((StateToken) t2).token;
			else
				t = t2;
			
			Harmony other = ((HarmonyToken) t).harmony;
			if (other == null) {
				return (harmonies == null);
			} else if (harmonies == null) {
				return false;
			} else {
				for (Harmony harmony : harmonies) {
					if (!other.root.equals(harmony.root)) {
						continue;
					} else {
						boolean[] pitches = harmony.quality.getPitches();
						boolean[] otherPitches = other.quality.getPitches();
						
						boolean failure = false;
						for (int j = 0; j < pitches.length; j++) {
							if (pitches[j] && !otherPitches[j]) {
								failure = true;
								break;
							}
						}
						if (failure) continue;
						
						if (harmony.bass == null) 
							if (other.bass == null)
								return true;
							else
								continue;
						else 
							if (harmony.bass.equals(other.bass))
								return true;
							else
								continue;
					}
				}
				return false;
			}
		}
	}

	public static class ChordConstraint<T extends Token> implements StateConstraint<T> {
		
		Harmony harmony;
		
		public ChordConstraint(Harmony harmony) {
			this.harmony = harmony;
		}
		
		@Override
		public boolean isSatisfiedBy(LinkedList<T> state, int i) {
			final T t2 = state.get(i);
			Token t;
			if (t2 instanceof StateToken)
				t = ((StateToken) t2).token;
			else
				t = t2;
			
			Harmony other = ((HarmonyToken) t).harmony;
			if (other == null) {
				return (harmony == null);
			} else if (harmony == null) {
				return false;
			} else if (!other.root.equals(harmony.root)) {
				return false;
			} else {
				boolean[] pitches = harmony.quality.getPitches();
				boolean[] otherPitches = other.quality.getPitches();
				
				for (int j = 0; j < pitches.length; j++) {
					if (pitches[j] && !otherPitches[j]) {
						return false;
					}
				}
				 
				if (harmony.bass == null) 
					return other.bass == null;
				else 
					return harmony.bass.equals(other.bass);
			}
		}
	}
	
	public static class RhythmConstraint<T extends Token> implements StateConstraint<T> {
		
		Double durationInQuarterNoteBeats;
		
		public RhythmConstraint(Double durationInBeats) {
			this.durationInQuarterNoteBeats = durationInBeats;
		}
		
		@Override
		public boolean isSatisfiedBy(LinkedList<T> state, int i) {
			final T t2 = state.get(i);
			RhythmToken t;
			if (t2 instanceof StateToken)
				t = ((StateToken<RhythmToken>) t2).token;
			else
				t = (RhythmToken) t2;
			
			return t.isOnset() && t.durationInQuarterNotes == durationInQuarterNoteBeats;
		}
		
	}
	
	public static class RhythmOnsetConstraint<T extends Token> implements StateConstraint<T> {
		
		Double durationInQuarterNoteBeats;
		
		public RhythmOnsetConstraint() {}
		
		@Override
		public boolean isSatisfiedBy(LinkedList<T> state, int i) {
			final T t2 = state.get(i);
			RhythmToken t;
			if (t2 instanceof StateToken)
				t = ((StateToken<RhythmToken>) t2).token;
			else
				t = (RhythmToken) t2;
			
			return t.isOnset();
		}
		
	}

	public static class BeatConstraint<T extends Token> implements StateConstraint<T> {
		
		Double offset;
		
		public BeatConstraint(Double offset) {
			this.offset = offset;
		}
		
		@Override
		public boolean isSatisfiedBy(LinkedList<T> state, int i) {
			final T t2 = state.get(i);
			Token t;
			if (t2 instanceof StateToken)
				t = ((StateToken) t2).token;
			else
				t = t2;
			
			if (t instanceof HarmonyToken) {
				return ((HarmonyToken) t).beat == offset;
			} else if (t instanceof PitchToken) {
				return ((PitchToken) t).beat == offset;
			} else if (t instanceof RhythmToken) {
				return ((RhythmToken) t).measureOffsetInQuarterNotes == offset;
			} else {
				throw new RuntimeException("Unreachable");
			}
		}
		
	}

	public static class HarmonyToken extends Token{
		// Tokens include beat information and have normalized pitch
		Harmony harmony;
		double beat;

		public HarmonyToken(Harmony harmony, double beat) {
			this.harmony = harmony;
			this.beat = beat;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			long temp;
			temp = Double.doubleToLongBits(beat);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			result = prime * result + ((harmony == null) ? 0 : harmony.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			HarmonyToken other = (HarmonyToken) obj;
			if (Double.doubleToLongBits(beat) != Double.doubleToLongBits(other.beat))
				return false;
			if (harmony == null) {
				if (other.harmony != null)
					return false;
			} else if (!harmony.equals(other.harmony))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return harmony + ", " + beat;
		}
	}

	public static class PitchToken extends Token{
		final static int MAX_TRACKED_DIST = 0;
		final static boolean NORMALIZE_BY_CHORD = true;

		// Tokens include beat information and have normalized pitch
		int normalizedPitch;
		double beat=0;
//		int distFromPhraseBeginningInMeasures, distFromPhraseEndingInMeasures;
//		Harmony harmony;
		
		public PitchToken(int normalizedPitch, Harmony harmony, double beat, int distFromPhraseBeginningInMeasures, int distFromPhraseEndingInMeasures) {
			this.normalizedPitch = normalizedPitch;
			this.beat = beat;
//			this.distFromPhraseBeginningInMeasures = Math.min(MAX_TRACKED_DIST, distFromPhraseBeginningInMeasures);
//			this.distFromPhraseEndingInMeasures = Math.min(MAX_TRACKED_DIST, distFromPhraseEndingInMeasures);
//			this.harmony = NORMALIZE_BY_CHORD ? null : harmony;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			long temp;
			temp = Double.doubleToLongBits(beat);
			result = prime * result + (int) (temp ^ (temp >>> 32));
//			result = prime * result + distFromPhraseBeginningInMeasures;
//			result = prime * result + distFromPhraseEndingInMeasures;
//			result = prime * result + ((harmony == null) ? 0 : harmony.hashCode());
			result = prime * result + normalizedPitch;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			PitchToken other = (PitchToken) obj;
			if (Double.doubleToLongBits(beat) != Double.doubleToLongBits(other.beat))
				return false;
//			if (distFromPhraseBeginningInMeasures != other.distFromPhraseBeginningInMeasures)
//				return false;
//			if (distFromPhraseEndingInMeasures != other.distFromPhraseEndingInMeasures)
//				return false;
//			if (harmony == null) {
//				if (other.harmony != null)
//					return false;
//			} else if (!harmony.equals(other.harmony))
//				return false;
			if (normalizedPitch != other.normalizedPitch)
				return false;
			return true;
		}

		@Override
		public String toString() {
			return normalizedPitch + ", " + beat;// + ", " + distFromPhraseBeginningInMeasures + ", "
//					+ distFromPhraseEndingInMeasures + ", " + harmony;
		}
		
	}
	
	public static DecimalFormat df4 = new DecimalFormat("#.####");
	
	private final static String structureFileDir = TabDriver.dataDir + "/Wikifonia_edited_xmls"; 
	private final static String structureFileName = "Traditional - Twinkle, twinkle, little star.xml"; 
	private final static String structureFilePath = structureFileDir + "/" + structureFileName; 
	
	private final static Map<String, SparseVariableOrderMarkovModel> models = new HashMap<String, SparseVariableOrderMarkovModel>();
	static {
		models.put("Harmony", null);
		models.put("Pitch", null);
		models.put("Rhythm", null);
		models.put("Lyric", null);
	}
	
	final private static int harmonyMarkovOrder = 5; // ideal here is probably 4-5 (eighth note intervals)
	final private static int pitchMarkovOrder = 3; // ideal here is probably 3-4 (eighth note intervals)
	final private static int rhythmMarkovOrder = 3; // ideal here is probably 3-4 (eighth note intervals)
	final private static int lyricMarkovOrder = 4; // ideal here is probably 4-5 (syllables)
	final private static int INSPIRING_FILE_COUNT_WIKIFONIA = 75;
	final private static int INSPIRING_FILE_COUNT_LYRICS_DB = 2500;
	final private static boolean useWikifoniaLyrics = false;
	final private static boolean useExternalLyrics = true;
	public static void main(String[] args) throws Exception {
		
		dbtb.main.Main.setRootPath("/Users/norkish/Archive/2017_BYU/ComputationalCreativity/");
		
		// MUSE
		Muse muse = new Muse();
		System.out.println("Muse's inspiration: " + muse.getInspiringEmotion());
		String dirName;
		
		if ((dirName = createDirectoryForTweet(muse.getTweet(), muse.getEmpathSummary())) == null) {
			System.exit(-1);
		}
		
		files = muse.findInspiringWikifoniaFiles(INSPIRING_FILE_COUNT_WIKIFONIA);
		
		// 0. STRUCTURE
		// Load a song and structure from file
		ParsedMusicXMLObject structureSong = loadSong(structureFilePath);
		
		
		// 0.5 LOAD TRAINING FROM WIKIFONIA
		trainModels(muse);

		// Tell GeneralizedGlobalStructureInferer which parameterizations to load from
		GeneralizedGlobalStructureInferer.setPopulationFile("None");
		
		// 1. HARMONY
		// For each harmony group
		// Order can be smaller
		
		// do an alignment for each song in the training set using the parameterization
		GeneralizedGlobalStructureAlignmentParameterization globalStructureAlignmentParameterization = GeneralizedGlobalStructureInferer.loadInitialPopulationFromFile("harmony",false).get(0).getSecond();
		Object[] matchingSegments = inferMatchingSegments(structureSong,globalStructureAlignmentParameterization);
		double[][] pathsTaken = (double[][]) matchingSegments[0];
		char[][] ptrMatrix = (char[][]) matchingSegments[1];
		Utils.normalizeByMaxVal(pathsTaken);
		
		List<SortedSet<Integer>> matchingPosesByPos = findMatchingPosesByPos(pathsTaken, ptrMatrix, globalStructureAlignmentParameterization);
		int[][] harmonyMatchConstraintList = new int[2][];
		harmonyMatchConstraintList[0] = createMatchConstraintList(matchingPosesByPos);
		harmonyMatchConstraintList[1] = new int[harmonyMatchConstraintList[0].length];
		Arrays.fill(harmonyMatchConstraintList[1], -1);
		harmonyMatchConstraintList[1][31] = 64;
		boolean[][] harmonyMatchConstraintOutcomeList = new boolean[2][harmonyMatchConstraintList[0].length];
		Arrays.fill(harmonyMatchConstraintOutcomeList[0], true);
		Arrays.fill(harmonyMatchConstraintOutcomeList[1], false);
		
		// create the NHMM n of length as long as the song with low markov order d
		// Train NHMM on Wikifonia
		int harmonyMarkovLength = matchingPosesByPos.size();
		SparseVariableOrderMarkovModel<HarmonyToken> harmonyMarkovModel = models.get("Harmony");
		List<List<ConditionedConstraint<HarmonyToken>>> harmonyConstraints = new ArrayList<List<ConditionedConstraint<HarmonyToken>>>();
		for (int j = 0; j < harmonyMarkovLength; j++) {
			harmonyConstraints.add(new ArrayList<ConditionedConstraint<HarmonyToken>>());
		}
		for (int i = 0; i < matchingPosesByPos.size(); i++) {
			harmonyConstraints.get(i).add(new ConditionedConstraint<>(new BeatConstraint<>(0.0), (i%(EVENTS_PER_BEAT*4)==0)));
		}
//		harmonyConstraints.get(0).add(new ConditionedConstraint<>(new ChordConstraint<>(new Harmony(new Root(3), new Quality(), null))));
//		harmonyConstraints.get(28).add(new ConditionedConstraint<>(new ChordConstraint<>(new Harmony(new Root(3), new Quality(), null)))); // has to end with a C chord
		harmonyConstraints.get(0).add(new ConditionedConstraint<>(new ChordsConstraint<>(new Harmony[] {new Harmony(new Root(3), new Quality(), null), new Harmony(new Root(0), new Quality("min"), null)}))); // has to end with a C chord
		
//		System.out.println("Building Harmony Solution Iterator");
		
		//TODO: add all control constraints to DFS
		Iterator<List<HarmonyToken>> harmonyIterator = MatchIteratorBuilderDFS.buildEfficiently(harmonyMatchConstraintList, harmonyMatchConstraintOutcomeList, null, harmonyMarkovModel, harmonyConstraints, 10000);
		
		// 3. RHYTHM (must go before lyrics to inform lyrics to maintain rhythmic patterns across verses)
		System.out.println("Loading rhythm constraints from file");
		globalStructureAlignmentParameterization = GeneralizedGlobalStructureInferer.loadInitialPopulationFromFile("rhythm",false).get(0).getSecond();
		matchingSegments = inferMatchingSegments(structureSong,globalStructureAlignmentParameterization);
		pathsTaken = (double[][]) matchingSegments[0];
		ptrMatrix = (char[][]) matchingSegments[1];
		Utils.normalizeByMaxVal(pathsTaken);
		
		matchingPosesByPos = findMatchingPosesByPos(pathsTaken, ptrMatrix, globalStructureAlignmentParameterization);
		int[] rhythmMatchConstraintList = createMatchConstraintList(matchingPosesByPos);
		
		boolean[] rhythmMatchConstraintOutcomeList = new boolean[rhythmMatchConstraintList.length];
		Arrays.fill(rhythmMatchConstraintOutcomeList, true);
		
		// create the NHMM n of length as long as the song with low markov order d
		// Train NHMM on Wikifonia
		int rhythmMarkovLength = matchingPosesByPos.size();
		SparseVariableOrderMarkovModel<RhythmToken> rhythmMarkovModel = models.get("Rhythm");
		List<List<ConditionedConstraint<RhythmToken>>> rhythmConstraints = new ArrayList<List<ConditionedConstraint<RhythmToken>>>();
		for (int j = 0; j < rhythmMarkovLength; j++) {
			rhythmConstraints.add(new ArrayList<ConditionedConstraint<RhythmToken>>());
			rhythmConstraints.get(j).add(new ConditionedConstraint<RhythmToken>(new BeatConstraint<>(0.0), (j%(EVENTS_PER_BEAT*4)==0)));
		}
		rhythmConstraints.get(0).add(new ConditionedConstraint<RhythmToken>(new RhythmOnsetConstraint<>()));
		rhythmConstraints.get(28).add(new ConditionedConstraint<RhythmToken>(new RhythmConstraint<>(2.0)));
		
//		
		// 4. LYRICS
		System.out.println("Computing Abstract Lyric Matching constraints from file");
		globalStructureAlignmentParameterization = GeneralizedGlobalStructureInferer.loadInitialPopulationFromFile("lyric",false).get(0).getSecond();
		matchingSegments = inferMatchingSegments(structureSong,globalStructureAlignmentParameterization);
		pathsTaken = (double[][]) matchingSegments[0];
		ptrMatrix = (char[][]) matchingSegments[1];
		Utils.normalizeByMaxVal(pathsTaken);
		
		List<SortedSet<Integer>> lyricMatchingPosesByPos = findMatchingPosesByPos(pathsTaken, ptrMatrix, globalStructureAlignmentParameterization);

		//Create comparators for lyric match constraints
		List<Comparator<SyllableToken>> equivalenceRelations = new ArrayList<Comparator<SyllableToken>>(){{
			add(null);
			add(new RhymeComparator());
			add(null);
			add(null);
		}};
		
		System.out.println("Computing Pitch Matching constraints");
		//**PITCH
		// do an alignment for each song in the training set using the parameterization
		globalStructureAlignmentParameterization = GeneralizedGlobalStructureInferer.loadInitialPopulationFromFile("pitch",false).get(0).getSecond();
		matchingSegments = inferMatchingSegments(structureSong,globalStructureAlignmentParameterization);
		pathsTaken = (double[][]) matchingSegments[0];
		ptrMatrix = (char[][]) matchingSegments[1];
		Utils.normalizeByMaxVal(pathsTaken);
		matchingSegments = null;
		
		
		matchingPosesByPos = findMatchingPosesByPos(pathsTaken, ptrMatrix, globalStructureAlignmentParameterization);
		int[] pitchMatchConstraintList = createMatchConstraintList(matchingPosesByPos);
		pathsTaken = null;
		ptrMatrix = null;
		
		boolean[] pitchMatchConstraintOutcomeList = new boolean[pitchMatchConstraintList.length];
		Arrays.fill(pitchMatchConstraintOutcomeList, true);
		
		
		// create the NHMM n of length as long as the song with low markov order d
		// Train NHMM on Wikifonia
		int pitchMarkovLength = matchingPosesByPos.size();
		matchingPosesByPos = null;
		SparseVariableOrderMarkovModel<PitchToken> pitchMarkovModel = models.get("Pitch");

		int fileSuffix = 1;
		Iterator<List<PitchToken>> pitchIterator = null;
		while (harmonyIterator.hasNext()) {
			List<HarmonyToken> harmonyGenerate = harmonyIterator.next();
			System.out.println("TRYING HARMONY:" + printSummary(harmonyGenerate,2.0));
			try {
				List<List<ConditionedConstraint<PitchToken>>> pitchConstraints = new ArrayList<List<ConditionedConstraint<PitchToken>>>();
				for (int j = 0; j < pitchMarkovLength; j++) {
					final ArrayList<ConditionedConstraint<PitchToken>> pitchConstraintsAtJ = new ArrayList<ConditionedConstraint<PitchToken>>();
					pitchConstraints.add(pitchConstraintsAtJ);
					pitchConstraintsAtJ.add(new ConditionedConstraint<PitchToken>(new PitchConstraint<PitchToken>(Note.REST),false)); // rests are determined from rhythm model, not pitch model
					if (j == 28) {
						pitchConstraintsAtJ.add(new ConditionedConstraint<PitchToken>(new PitchConstraint<PitchToken>((harmonyGenerate.get(j).harmony.root.rootStep + 9)%12)));
					} else if (j%(2*EVENTS_PER_BEAT) == 0){
						pitchConstraintsAtJ.add(new ConditionedConstraint<PitchToken>(new PitchInChordConstraint<PitchToken>(harmonyGenerate.get(j).harmony)));
					}
				}
				
//				System.out.println("Building Pitch Iterator");
				pitchIterator = MatchIteratorBuilderDFS.buildEfficiently(pitchMatchConstraintList, pitchMatchConstraintOutcomeList, pitchMarkovModel, pitchConstraints, 10000);
			} catch (Exception e) {
//				System.out.println(e.getMessage());
				continue;
			}
			
			List<PitchToken> pitchGenerate = pitchIterator.next();
			System.out.println("TRYING PITCH:" + printSummary(pitchGenerate,1.0));

			// add measures to score according to measures from structureSong
			List<Measure> instantiatedMeasures = new ArrayList<Measure>();
			List<Integer> playedToAbsoluteMeasureNumberMap = structureSong.playedToAbsoluteMeasureNumberMap;
			SortedMap<Integer, Key> normalizedKeyByAbsoluteMeasure = structureSong.normalizedKeyByAbsoluteMeasure;
			for (Integer absoluteMeasure : playedToAbsoluteMeasureNumberMap) {
				final Measure newMeasure = new Measure(null, 0);
				Time timeForAbsoluteMeasure = structureSong.getTimeForAbsoluteMeasure(absoluteMeasure);
				newMeasure.setTime(timeForAbsoluteMeasure.beats, timeForAbsoluteMeasure.beatType);
				Key valueForKeyBeforeOrEqualTo = Utils.valueForKeyBeforeOrEqualTo(absoluteMeasure, normalizedKeyByAbsoluteMeasure);
				newMeasure.setKey(valueForKeyBeforeOrEqualTo.fifths, valueForKeyBeforeOrEqualTo.mode);
				newMeasure.setDivisions((int) (EVENTS_PER_BEAT * timeForAbsoluteMeasure.beatType / 4.0));
				instantiatedMeasures.add(newMeasure);
			}
			
			List<SyllableToken> lyricGenerate = null;
			boolean foundLyricsRhythmMatch = false;
			
//			System.out.println("Building Rhythm Iterator");
			
			Iterator<List<RhythmToken>> rhythmIterator = MatchIteratorBuilderDFS.buildEfficiently(rhythmMatchConstraintList, rhythmMatchConstraintOutcomeList, rhythmMarkovModel, rhythmConstraints, 5000);
			
			while (rhythmIterator.hasNext()) {
				List<RhythmToken> rhythmGenerate = rhythmIterator.next();
				System.out.println("TRYING RHYTHM:" + printSummary(rhythmGenerate, 0.5));

				// 4. LYRICS
				// Length (in syllables) is determined from the rhythm
				int[][] lyricMatchConstraintLists;
				try {
					lyricMatchConstraintLists = createLyricMatchConstraintLists(lyricMatchingPosesByPos, rhythmGenerate, pitchGenerate);
				} catch (Exception e) {
//					System.out.println(e.getMessage());
					continue;
				}
				
				boolean[][] lyricMatchConstraintOutcomeList = new boolean[4][lyricMatchConstraintLists[0].length];
				for (int i = 0; i < lyricMatchConstraintOutcomeList.length; i++) {
					boolean[] bs = lyricMatchConstraintOutcomeList[i];
					Arrays.fill(bs, i<2);
				}
				
//				for (int j = 0; j < lyricMatchConstraintLists[0].length; j++) {
//					System.out.println((j+1) + ":" + lyricMatchConstraintLists[0][j] + "\t" + (j+1) + ":" + lyricMatchConstraintLists[1][j]);
//				}
				
				// create the NHMM n of length as long as the song with low markov order d
				// Train NHMM on Wikifonia
				int lyricMarkovLength = lyricMatchConstraintLists[0].length;
				SparseVariableOrderMarkovModel<SyllableToken> lyricMarkovModel = models.get("Lyric");
				List<List<ConditionedConstraint<SyllableToken>>> lyricConstraints = new ArrayList<List<ConditionedConstraint<SyllableToken>>>();
				for (int j = 0; j < lyricMarkovLength; j++) {
					lyricConstraints.add(new ArrayList<ConditionedConstraint<SyllableToken>>());
				}

				lyricConstraints.get(0).add(new ConditionedConstraint<>(new StartOfWordConstraint<>()));

				int numNotes = 0;
				double durationSoFar = 0.0;
				double nextPhraseEnding = 8.0;
				// Make sure stressed syllables don't land on offbeats
				for (RhythmToken stateToken : rhythmGenerate) {
					if (stateToken.isOnset()) { // if it's a note
						if (!stateToken.isRest && durationSoFar % 1.0 != 0.0) { // if it's not a rest and it's offbeat
							lyricConstraints.get(numNotes).add(new ConditionedConstraint<>(new AbsoluteStressConstraint<>(0)));
						}
						durationSoFar += stateToken.durationInQuarterNotesOfTiedNotes;
						if (!stateToken.isRest) numNotes++;
					}
					
					if (durationSoFar >= nextPhraseEnding) {
						lyricConstraints.get(numNotes-1).add(new ConditionedConstraint<>(new EndOfWordConstraint<>()));
						// can't end phrase with a dumb word
						lyricConstraints.get(numNotes-1).add(new ConditionedConstraint<>(new PartsOfSpeechConstraint<>(new HashSet<>(Arrays.asList(Pos.DT, Pos.PDT, Pos.PRP$, Pos.TO, Pos.UH, Pos.WDT, Pos.WP, Pos.WP$, Pos.WRB))), false));
						nextPhraseEnding += 8.0;
					}
				}
				
				try {
//					System.out.println("Building Lyric Iterator");
					
					Iterator<List<SyllableToken>> lyricIterator = MatchIteratorBuilderDFS.buildEfficiently(lyricMatchConstraintLists, lyricMatchConstraintOutcomeList, equivalenceRelations, lyricMarkovModel, lyricConstraints,10000);
					lyricGenerate = lyricIterator.next();
					System.out.println("KEEPING:" + printSummary(lyricGenerate, 0.5));
				
				} catch (Exception e) {
//					System.out.println(e.getMessage());
					continue;
				}
				foundLyricsRhythmMatch = true;
				
				Score newScore = new Score();
				newScore.addMeasures(instantiatedMeasures);
				
				//add harmony to score
				int measure = -1;
				Harmony prevHarmony = null, currHarmony;
				for (HarmonyToken hToken : harmonyGenerate) {
					currHarmony = hToken.harmony;
					if (hToken.beat == 0.0) {
						measure++;
					}
					if (!currHarmony.equals(prevHarmony) || hToken.beat == 0.0) {
						newScore.addHarmony(measure, hToken.beat, hToken.harmony);
						prevHarmony = currHarmony;
					}
				}
				
				measure = 0;
				int nextLyricIdx = 0;
				final List<Measure> measures = newScore.getMeasures();
				int minNote = Integer.MAX_VALUE;
				int maxNote = Integer.MIN_VALUE;
				StringBuilder titleBuilder = new StringBuilder();
				boolean addTitle = true;
				
				for (int j = 0; j < rhythmGenerate.size(); j++) { // for every rhythm token (onset or not)
					PitchToken pitchToken = pitchGenerate.get(j); // get the corresponding pitch token
					RhythmToken rhythmToken = rhythmGenerate.get(j); // here's the rhythm token
//					System.out.println("Considering note:" + rhythmToken + " with pitch:" + pitchToken);
//					if (rhythmToken.measureOffsetInQuarterNotes == 0.0) { // we assume any rhythm token whose measure offset in quarternotes is 0 is a measure start.
//						measure++;
//					}
					if (rhythmToken.isOnset()) { //add a note if it *IS* and onset
						int pitch = rhythmToken.isRest ? Note.REST : pitchToken.normalizedPitch; // get MIDI pitch value (or rest)
						
						double currMeasureOffsetInQuarterNotes = rhythmToken.measureOffsetInQuarterNotes;
						boolean lyricAdded = false;
						
						List<Note> createTiedNoteWithDuration = new ArrayList<Note>();
						int i = 0;
						double remainingDuration = rhythmToken.durationInQuarterNotesOfTiedNotes;
						do {
							double durationForCurrMeasure = Math.min(remainingDuration, (i==0 ? 4.0 - currMeasureOffsetInQuarterNotes : 4.0));
							remainingDuration -= durationForCurrMeasure;
							
							final List<Note> currNote = MelodyEngineer.createTiedNoteWithDuration(measures.get(measure+i).beatsToDivs(durationForCurrMeasure), pitch, measures.get(measure+i).divisionsPerQuarterNote);
							currNote.get(0).tie = NoteTie.NONE;
							currNote.get(currNote.size()-1).tie = NoteTie.NONE;
							createTiedNoteWithDuration.addAll(currNote); 
							if (!rhythmToken.isRest && !lyricAdded) {
								final SyllableToken sToken = lyricGenerate.get(nextLyricIdx++);
								final NoteLyric newNoteLyric = new NoteLyric(createNoteLyric(sToken));
								createTiedNoteWithDuration.get(0).setLyric(newNoteLyric, true);
								if (addTitle && measure > 1 && (newNoteLyric.syllabic == Syllabic.BEGIN || newNoteLyric.syllabic == Syllabic.SINGLE)) addTitle = false;
								if (addTitle && sToken.getPositionInContext() == 0) titleBuilder.append(sToken.getStringRepresentation() + " ");
							}
							lyricAdded = true;
						} while(remainingDuration > 0.0);
						
						if (createTiedNoteWithDuration.size() > 1) {
							createTiedNoteWithDuration.get(0).tie = NoteTie.START;
							createTiedNoteWithDuration.get(createTiedNoteWithDuration.size()-1).tie = NoteTie.STOP;
						}
						
						for (Note note : createTiedNoteWithDuration) {
//							System.out.println("Adding note:" + note + " to msr " + measure + " at beat " + currMeasureOffsetInQuarterNotes);
							
							newScore.addNote(measure, currMeasureOffsetInQuarterNotes, note);
							
							if (note.pitch != Note.REST && note.pitch < minNote) {
								minNote = note.pitch;
							}
							
							if (note.pitch != Note.REST && note.pitch > maxNote) {
								maxNote = note.pitch;
							}
							
							currMeasureOffsetInQuarterNotes += 1.0 * note.duration / measures.get(measure).divisionsPerQuarterNote;
							if (currMeasureOffsetInQuarterNotes >= 4.0) {
								measure++;
								currMeasureOffsetInQuarterNotes %= 4.0;
							}
						}
					}
				}
				
				String title = titleBuilder.toString().trim();
				System.out.println("Saving composition as ./compositions/" + dirName + "/" + title.replaceAll("\\W+", "_") + "." + fileSuffix + ".xml...");
				
				Composition composition = new Composition(newScore);
				composition.setMuse(muse);
				composition.setTitle(title);
				Files.write(Paths.get("./compositions/" + dirName + "/"  + title.replaceAll("\\W+", "_") + "." + fileSuffix + ".xml"), composition.toString().getBytes());
				
				Orchestrator orchestrator = new CompingMusicXMLOrchestrator();
				orchestrator.orchestrate(composition);
				Files.write(Paths.get("./compositions/" + dirName + "/"  + title.replaceAll("\\W+", "_") + "." + fileSuffix + ".orchest.xml"), composition.toString().getBytes());

				int suggestedTransposition = 45 - minNote;
				
				if (suggestedTransposition > 0) {
					System.out.println("Transposing up " + suggestedTransposition + " half steps");
					composition.transpose(suggestedTransposition);
					Files.write(Paths.get("./compositions/" + dirName + "/"  + title.replaceAll("\\W+", "_") + "." + fileSuffix + ".orchest.transp.xml"), composition.toString().getBytes());
				}
				
				Files.write(Paths.get("./compositions/" + dirName + "/" + title.replaceAll("\\W+", "_") + "." + fileSuffix + ".descr.txt"), muse.composeDescription(muse.getEmpathVector(printSummary(lyricGenerate, 0.5)), printSummary(lyricGenerate, 0.5), title).getBytes());
				fileSuffix++;
				break;
			}
			if (!foundLyricsRhythmMatch) {
				System.out.println("Could not find lyrics to match any rhythm");
				System.exit(-1);
			}
		}
	}

	private static String createDirectoryForTweet(Tweet tweet, String empathDescr) {
		if (tweet == null)
			System.out.println("Tweet is null.");

		StringBuilder dirNameBldr = new StringBuilder();
		
		dirNameBldr.append(tweet.username.replaceAll("[^\\W ]+", ""));
		dirNameBldr.append(" (");
		dirNameBldr.append(tweet.date.replace(":", ""));
		dirNameBldr.append(") ");
		dirNameBldr.append("- " + empathDescr);
		
		String dirName = dirNameBldr.toString();
		
		File f = new File("./compositions/"+dirName);
		if (f.exists() && f.isDirectory()) {
		   System.out.println("Songs have already been composed for this inspiration.");
		   return null;
		} else {
			f.mkdir();
		}
		
		return dirName;
	}

	private static String extractTitle(List<SyllableToken> lyricGenerate) {
		StringBuilder str = new StringBuilder();
		
		for (SyllableToken sToken : lyricGenerate) {
			if (sToken.getPositionInContext() == 0) {
				str.append(sToken.getStringRepresentation());
				str.append(" ");
			}
		}
		
		return str.toString().trim();
	}

	private static NoteLyric createNoteLyric(SyllableToken sToken) {
		Syllabic syllabic;
		final int sylCount = sToken.getCountOfSylsInContext();
		final int idx = sToken.getPositionInContext();
		if (sylCount == 1)
			syllabic = Syllabic.SINGLE;
		else {
			if (idx==0)
				syllabic = Syllabic.BEGIN;
			else if (idx == sylCount-1)
				syllabic = Syllabic.END;
			else
				syllabic = Syllabic.MIDDLE;
		}
		
		final String word = sToken.getStringRepresentation();
		List<StressedPhone[]> phones = Phonetecizer.getPhones(word, false);
		if (phones == null || phones.isEmpty()) throw new RuntimeException("No pronounciation for " + word);
		StressedPhone[] bestStressedPhones = null;
		int maxDiff = Integer.MAX_VALUE;
		for (StressedPhone[] stressedPhones : phones) {
			final int diff = Math.abs(stressedPhones.length-sylCount);
			if (diff < maxDiff) {
				bestStressedPhones = stressedPhones;
				maxDiff = diff;
				if (diff == 0) {
					break;
				}
			}
		}
		List<Triple<String, StressedPhone[], StressedPhone>> syllabifiedWord = dbtb.linguistic.paul.Syllabifier.syllabify(word, bestStressedPhones);
		
		String text = "";
		if (idx < syllabifiedWord.size())
			text = syllabifiedWord.get(idx).getFirst();
		
		return new NoteLyric(syllabic, text, false, false);
	}

	private static <T extends Token> String printSummary(List<T> generate, double printInterval) {
		StringBuilder str = new StringBuilder();
		
		for (T token : generate) {
			if (token instanceof HarmonyToken) {
				HarmonyToken hToken = (HarmonyToken) token;
				if (hToken.beat % printInterval == 0) { 
					str.append(hToken.harmony.toShortString());
					str.append(" ");
				}
			} else if (token instanceof PitchToken) {
				PitchToken pToken = (PitchToken) token;
				if (pToken.beat % printInterval == 0) {
					str.append(Pitch.getPitchName((pToken.normalizedPitch+3)%12) + (pToken.normalizedPitch/12));
					str.append(" ");
				}
			} 
			else if (token instanceof RhythmToken) {
				RhythmToken rToken = (RhythmToken) token;
				if (rToken.measureOffsetInQuarterNotes % printInterval == 0 && rToken.beatsSinceOnsetInQuarterNotes == 0.0) {
					if (rToken.isRest)
						str.append("(");
					str.append(rToken.measureOffsetInQuarterNotes+1);
					if (rToken.isRest)
						str.append(") ");
					else
						str.append(" ");
				}
			}
			else if (token instanceof SyllableToken) {
				SyllableToken sToken = (SyllableToken) token;
				if (sToken.getPositionInContext() == 0) {
					str.append(sToken.getStringRepresentation());
					str.append(" ");
				}
			}
		}
		
		return str.toString().trim();
	}

	/**
	 * First list is matching lyrics positions
	 * Second list is matching rhyme positions
	 * @param matchingPosesByPos
	 * @param rhythmGenerate
	 * @param pitchGenerate 
	 * @return
	 */
	private static int[][] createLyricMatchConstraintLists(List<SortedSet<Integer>> matchingPosesByPos, List<RhythmToken> rhythmGenerate, List<PitchToken> pitchGenerate) throws NoSuchElementException {

		SortedMap<Integer, Integer> oldToNewIdx = new TreeMap<Integer, Integer>();
		
		for (int i = 0; i < rhythmGenerate.size(); i++) {
			RhythmToken rToken = rhythmGenerate.get(i);
			PitchToken pToken = pitchGenerate.get(i);
			if (rToken.isOnset() && !rToken.isRest && pToken.normalizedPitch != Note.REST) {
				// then i represents an index for our Match ConstraintLists
//				System.out.println(i + " is now " + oldToNewIdx.size());
				oldToNewIdx.put(i, oldToNewIdx.size());
			}
		}
		int[][] matchConstraintLists = new int[4][oldToNewIdx.size()];
		for (int i = 0; i < matchConstraintLists.length; i++) {
			Arrays.fill(matchConstraintLists[i], -1);
		}

		for (int i = 0; i < matchConstraintLists[2].length - 1; i++) {
			matchConstraintLists[2][i] = i + 2; // don't allow consecutive syllables to be the same (1-based)
		}
		
		// create constraints for required non-matches (some over-writing here)
		matchConstraintLists[2][oldToNewIdx.get(oldToNewIdx.headMap(16).lastKey())] = oldToNewIdx.get(oldToNewIdx.headMap(32).lastKey()) + 1;
		matchConstraintLists[2][oldToNewIdx.get(oldToNewIdx.headMap(32).lastKey())] = oldToNewIdx.get(oldToNewIdx.headMap(48).lastKey()) + 1;
		matchConstraintLists[3][oldToNewIdx.get(oldToNewIdx.headMap(16).lastKey())] = oldToNewIdx.get(oldToNewIdx.headMap(48).lastKey()) + 1;
		matchConstraintLists[3][oldToNewIdx.get(oldToNewIdx.headMap(48).lastKey())] = oldToNewIdx.get(oldToNewIdx.headMap(64).lastKey()) + 1;
		
		// create rhyme constraints (SPECIFIC TO TWINKLE, TWINKLE, LITTLE STAR)
		matchConstraintLists[1][oldToNewIdx.get(oldToNewIdx.headMap(16).lastKey())] = oldToNewIdx.get(oldToNewIdx.headMap(32).lastKey()) + 1;
		matchConstraintLists[1][oldToNewIdx.get(oldToNewIdx.headMap(48).lastKey())] = oldToNewIdx.get(oldToNewIdx.headMap(64).lastKey()) + 1;
		matchConstraintLists[1][oldToNewIdx.get(oldToNewIdx.headMap(80).lastKey())] = oldToNewIdx.get(oldToNewIdx.headMap(96).lastKey()) + 1;
		
		Integer newIdx,otherNewIndex;
		for (int i = 0; i < matchingPosesByPos.size(); i++) {
			SortedSet<Integer> matchingPosesAtI = matchingPosesByPos.get(i);
//			System.out.println("Matches at " + i + " was " + matchingPosesAtI);
			newIdx = oldToNewIdx.get(i);
			if (newIdx == null) continue;
			if (matchingPosesAtI.isEmpty()) {
				matchConstraintLists[0][newIdx] = -1;
			} else {
				final SortedSet<Integer> tailSet = matchingPosesAtI.tailSet(i+1);
				matchConstraintLists[0][newIdx] = -1;
				if (!tailSet.isEmpty()) {
					for (Integer integer : tailSet) {
						otherNewIndex = oldToNewIdx.get(integer);
						if (otherNewIndex == null) 
							continue;
						else {
							matchConstraintLists[0][newIdx] = otherNewIndex + 1;
							break;
						}
					}
				}
			}
//			System.out.println("Match at " + newIdx + " now " + matchConstraintLists[0][newIdx]);
		}
		
		return matchConstraintLists;
	}

	private static int[] createMatchConstraintList(List<SortedSet<Integer>> matchingPosesByPos) {
		int[] matchConstraintList = new int[matchingPosesByPos.size()];
		for (int i = 0; i < matchConstraintList.length; i++) {
			SortedSet<Integer> matchingPosesAtI = matchingPosesByPos.get(i);
			if (matchingPosesAtI.isEmpty()) {
				matchConstraintList[i] = -1;
			} else {
				final SortedSet<Integer> tailSet = matchingPosesAtI.tailSet(i+1);
				if (tailSet.isEmpty())
					matchConstraintList[i] = -1;
				else
					matchConstraintList[i] = tailSet.first() + 1;
			}
		}
		
		return matchConstraintList;
	}

	private static List<SortedSet<Integer>> findMatchingPosesByPos(double[][] pathsTaken, char[][] ptrMatrix, GeneralizedGlobalStructureAlignmentParameterization globalStructureAlignmentParameterization) {
		List<SortedSet<Integer>> matchingPosesByPos = new ArrayList<SortedSet<Integer>>();
		SortedSet<Integer> matchingPosesForCurrPos;
		SortedSet<Integer> matchingPosesForMatchPos;
		
		for (int i = 1; i < pathsTaken.length; i++) {
			matchingPosesByPos.add(null);
		}
		
		// for each event a, mark a with a (new or previously assigned) match group it belongs to and match all other events b that aligns with with the same match group
		for (int currPos = 1; currPos < pathsTaken.length; currPos++) {
			matchingPosesForCurrPos = matchingPosesByPos.get(currPos-1);
			if (matchingPosesForCurrPos == null) {
				matchingPosesForCurrPos = new TreeSet<Integer>();
				matchingPosesByPos.set(currPos-1, matchingPosesForCurrPos);
				matchingPosesForCurrPos.add(currPos-1);
			}
			for (int matchPos = currPos + globalStructureAlignmentParameterization.distanceFromDiagonalInBeats; matchPos < pathsTaken[currPos].length; matchPos++) {
				if (pathsTaken[currPos][matchPos] == 1.0 && ptrMatrix[currPos][matchPos] == 'D') {
					matchingPosesForMatchPos = matchingPosesByPos.get(matchPos-1);
					if (matchingPosesForMatchPos != null) { // if the matching position is already assigned a group
						if (matchingPosesForCurrPos != matchingPosesForMatchPos) { // and it is not the current group
							for (Integer matchingPosForMatchPos : matchingPosesForMatchPos) {
								matchingPosesByPos.set(matchingPosForMatchPos, matchingPosesForCurrPos); // change all members of that group to match the current group
							}
							matchingPosesForCurrPos.addAll(matchingPosesForMatchPos);
						}
					} else { // otherwise it hasn't been assigned and we add it to this group
						matchingPosesForCurrPos.add(matchPos-1);
						matchingPosesByPos.set(matchPos-1, matchingPosesForCurrPos);
					}
				}
			}
		}
		
		// if at any point the current match group of a does not match the (previously assigned) match group of b, combine them into the same match group.
		
		return matchingPosesByPos;
	}

	private static Object[] inferMatchingSegments(ParsedMusicXMLObject structureSong, GeneralizedGlobalStructureAlignmentParameterization globalStructureAlignmentParameterization) throws Exception {		
		Object[] matrices = GeneralizedGlobalStructureInferer.align(structureSong, globalStructureAlignmentParameterization);
		double[][] alnMatrix = (double[][]) matrices[0];
		char[][] ptrMatrix = (char[][]) matrices[1];

		// use the alignment to infer the locations of the target segment type
		Object[] inferredSegments = GeneralizedGlobalStructureInferer.inferTargetSegmentLocations(alnMatrix, ptrMatrix, globalStructureAlignmentParameterization);
		return new Object[]{inferredSegments[1],ptrMatrix};
	}

	private static final boolean ALL_STATES_INITIAL_STATES = true; 
	private static File[] files = new File(TabDriver.dataDir + "/Wikifonia_edited_xmls").listFiles();
	private static final int EVENTS_PER_BEAT = 2;
	private static void trainModels(Muse muse) throws IOException {
		
		// declare all structures
		BidirectionalVariableOrderPrefixIDMap<HarmonyToken> harmonyStatesByIndex = null;
		Map<Integer, Double> harmonyPriors = null;
		Map<Integer, Map<Integer, Double>> harmonyTransitions = null;
		
		BidirectionalVariableOrderPrefixIDMap<PitchToken> pitchStatesByIndex = null;
		Map<Integer, Double> pitchPriors = null;
		Map<Integer, Map<Integer, Double>> pitchTransitions = null;
		
		BidirectionalVariableOrderPrefixIDMap<RhythmToken> rhythmStatesByIndex = null;
		Map<Integer, Double> rhythmPriors = null;
		Map<Integer, Map<Integer, Double>> rhythmTransitions = null;
		
		BidirectionalVariableOrderPrefixIDMap<SyllableToken> lyricStatesByIndex = null;
		Map<Integer, Double> lyricPriors = null;
		Map<Integer, Map<Integer, Double>> lyricTransitions = null;
		DataLoader dl = new DataLoader(lyricMarkovOrder);

		// initialize appropriate substructures
		
		// HARMONY
		if (models.containsKey("Harmony")) {
			harmonyStatesByIndex = new BidirectionalVariableOrderPrefixIDMap<HarmonyToken>(harmonyMarkovOrder);
			harmonyPriors = new HashMap<Integer, Double>();
			harmonyTransitions = new HashMap<Integer, Map<Integer, Double>>();
		}
		
		// PITCH
		if (models.containsKey("Pitch")) {
			pitchStatesByIndex = new BidirectionalVariableOrderPrefixIDMap<PitchToken>(pitchMarkovOrder);
			pitchPriors = new HashMap<Integer, Double>();
			pitchTransitions = new HashMap<Integer, Map<Integer, Double>>();
		}

		// RHYTHM
		if (models.containsKey("Rhythm")) {
			rhythmStatesByIndex = new BidirectionalVariableOrderPrefixIDMap<RhythmToken>(rhythmMarkovOrder);
			rhythmPriors = new HashMap<Integer, Double>();
			rhythmTransitions = new HashMap<Integer, Map<Integer, Double>>();
		}
		
		// LYRIC
		if (models.containsKey("Lyric")) {
			lyricStatesByIndex = new BidirectionalVariableOrderPrefixIDMap<SyllableToken>(lyricMarkovOrder);
			lyricPriors = new HashMap<Integer, Double>();
			lyricTransitions = new HashMap<Integer, Map<Integer, Double>>();
		}
		
		// train
		System.out.println("TRAINING ON " + Arrays.toString(files));
		for (File file : files) {
			if (file.getName().startsWith(".DS")) continue;
			System.out.println("Loading XML for " + file.getName());
			MusicXMLParser musicXMLParser = null;
			try {
				final Document xml = MusicXMLSummaryGenerator.parseXML(new FileInputStream(file));
				musicXMLParser = new MusicXMLParser(file.getName(), xml);
//				MusicXMLSummaryGenerator.printDocument(xml, System.out);
			} catch (Exception e) {
				e.printStackTrace();
			}
			ParsedMusicXMLObject musicXML = musicXMLParser.parse(true);
			if (musicXML == null) {
				System.err.println("musicXML was null for " + file.getName());
				continue;
			}
			
			List<MusicXMLAlignmentEvent> alignmentEvents = musicXML.getAlignmentEvents(EVENTS_PER_BEAT);
			
			LinkedList<HarmonyToken> harmonyPrefix = new LinkedList<HarmonyToken>();
			LinkedList<PitchToken> pitchPrefix = new LinkedList<PitchToken>();
			LinkedList<RhythmToken> rhythmPrefix = new LinkedList<RhythmToken>();
			int prevHarmonyPrefixID = -1, nextHarmonyPrefixID;
			int prevPitchPrefixID = -1, nextPitchPrefixID;
			int prevRhythmPrefixID = -1, nextRhythmPrefixID;
			StringBuilder str = new StringBuilder();
			for (int i = 0; i < alignmentEvents.size(); i++) {
				MusicXMLAlignmentEvent alignmentEvent = alignmentEvents.get(i);
				// TRAIN HARMONY
//				if (harmonyStatesByIndex != null && file.getName().contains("Rainbow")) {
				if (harmonyStatesByIndex != null && file.getName().contains("")) {
					harmonyPrefix.addLast(new HarmonyToken(alignmentEvent.harmony,alignmentEvent.beat));
					if (i >= (harmonyMarkovOrder-1)) {
						nextHarmonyPrefixID = harmonyStatesByIndex.addPrefix(harmonyPrefix);
						if (prevHarmonyPrefixID == -1) {// || ALL_STATES_INITIAL_STATES) {
							Utils.incrementDoubleForKey(harmonyPriors, nextHarmonyPrefixID);
						} 
						if (prevHarmonyPrefixID != -1) {
							Utils.incrementDoubleForKeys(harmonyTransitions, prevHarmonyPrefixID, nextHarmonyPrefixID);
						}
						prevHarmonyPrefixID = nextHarmonyPrefixID;
						harmonyPrefix.removeFirst();
					}
				}
				// TRAIN PITCH
//				if (pitchStatesByIndex != null && file.getName().contains("Rainbow")) {
				if (pitchStatesByIndex != null && file.getName().contains("")) {
					pitchPrefix.addLast(new PitchToken(alignmentEvent.note.pitch,alignmentEvent.harmony, alignmentEvent.beat, 0, 0));
					if (i >= (pitchMarkovOrder-1)) {
						nextPitchPrefixID = pitchStatesByIndex.addPrefix(pitchPrefix);
						if (prevPitchPrefixID == -1 || ALL_STATES_INITIAL_STATES) {
							Utils.incrementDoubleForKey(pitchPriors, nextPitchPrefixID);
						} 
						if (prevPitchPrefixID != -1){
							Utils.incrementDoubleForKeys(pitchTransitions, prevPitchPrefixID, nextPitchPrefixID);
						}
						prevPitchPrefixID = nextPitchPrefixID;
						pitchPrefix.removeFirst();
					}
				}
				// TRAIN RHYTHM
//				if (rhythmStatesByIndex != null && file.getName().contains("")) {
				if (rhythmStatesByIndex != null && file.getName().contains("")) {
					int quarterNotesPerBeat = 4/musicXML.getTimeForAbsoluteMeasure(alignmentEvent.measure).beatType;
					rhythmPrefix.addLast(new RhythmToken(alignmentEvent.note.duration/musicXML.getDivsPerQuarterForAbsoluteMeasure(alignmentEvent.measure), alignmentEvent.tiedDurationOfCurrentNote/musicXML.getDivsPerQuarterForAbsoluteMeasure(alignmentEvent.measure), alignmentEvent.currBeatsSinceOnset*quarterNotesPerBeat, alignmentEvent.beat*quarterNotesPerBeat, alignmentEvent.note.pitch == Note.REST));
					if (i >= (rhythmMarkovOrder-1)) {
//						System.out.println(rhythmPrefix.peekLast());
						nextRhythmPrefixID = rhythmStatesByIndex.addPrefix(rhythmPrefix);
						if (prevRhythmPrefixID == -1 || ALL_STATES_INITIAL_STATES) {
							Utils.incrementDoubleForKey(rhythmPriors, nextRhythmPrefixID);
						} 
						if (prevRhythmPrefixID != -1){
							Utils.incrementDoubleForKeys(rhythmTransitions, prevRhythmPrefixID, nextRhythmPrefixID);
						}
						prevRhythmPrefixID = nextRhythmPrefixID;
						rhythmPrefix.removeFirst();
					}
				}
				// TRAIN LYRIC
//				if (lyricStatesByIndex != null && (file.getName().contains("Lennon")) && alignmentEvent.lyricOnset) { //  && alignmentEvent.lyric.syllabic == Syllabic.BEGIN?
				if (useWikifoniaLyrics && lyricStatesByIndex != null && (file.getName().contains("")) && alignmentEvent.lyricOnset) { //  && alignmentEvent.lyric.syllabic == Syllabic.BEGIN?
					str.append(alignmentEvent.lyric.text);
					if (alignmentEvent.lyric.syllabic == Syllabic.END || alignmentEvent.lyric.syllabic == Syllabic.SINGLE)
						str.append(' ');
				}
			}
			
			if (useWikifoniaLyrics && lyricStatesByIndex != null){
				String[] trainingSentences = str.toString().split("(?<=[;.!?:]+) ");
				trainOnSentences(lyricStatesByIndex, lyricPriors, lyricTransitions, dl, trainingSentences);
			}
		}
		
		if (useExternalLyrics) {
			String[][] externalLyricsBySong = loadExternalLyricsForTraining(muse);
			for (String[] trainingSentences : externalLyricsBySong) {
				trainOnSentences(lyricStatesByIndex, lyricPriors, lyricTransitions, dl, trainingSentences);
			}
		}
		
		// 	initialize appropriate structures
		if (harmonyStatesByIndex != null) {
			dbtb.utils.Utils.normalize(harmonyPriors);
			dbtb.utils.Utils.normalizeByFirstDimension(harmonyTransitions);
			models.put("Harmony", new SparseVariableOrderMarkovModel<HarmonyToken>(harmonyStatesByIndex, harmonyPriors, harmonyTransitions));
		}
		if (pitchStatesByIndex != null) {
			dbtb.utils.Utils.normalize(pitchPriors);
			dbtb.utils.Utils.normalizeByFirstDimension(pitchTransitions);
			models.put("Pitch", new SparseVariableOrderMarkovModel<PitchToken>(pitchStatesByIndex, pitchPriors, pitchTransitions));
		}
		if (rhythmStatesByIndex != null) {
			dbtb.utils.Utils.normalize(rhythmPriors);
			dbtb.utils.Utils.normalizeByFirstDimension(rhythmTransitions);
			models.put("Rhythm", new SparseVariableOrderMarkovModel<RhythmToken>(rhythmStatesByIndex, rhythmPriors, rhythmTransitions));
		}
		if (lyricStatesByIndex != null) {
			dbtb.utils.Utils.normalize(lyricPriors);
			dbtb.utils.Utils.normalizeByFirstDimension(lyricTransitions);
			models.put("Lyric", new SparseVariableOrderMarkovModel<SyllableToken>(lyricStatesByIndex, lyricPriors, lyricTransitions));
		}
	}

	private static String[][] loadExternalLyricsForTraining(Muse muse) throws IOException {
		return muse.findInspiringLyricDBMatches(INSPIRING_FILE_COUNT_LYRICS_DB);
	}

	private static void trainOnSentences(BidirectionalVariableOrderPrefixIDMap<SyllableToken> lyricStatesByIndex,
			Map<Integer, Double> lyricPriors, Map<Integer, Map<Integer, Double>> lyricTransitions, DataLoader dl,
			String[] trainingSentences) {
		Integer toTokenID;
		Integer fromTokenID = null;
		List<SyllableToken> trainingSentenceTokens = new ArrayList<SyllableToken>();
		System.out.print("LYR TRAIN: ");
		for (String trainingSentence : trainingSentences) {
			final List<List<SyllableToken>> convertToSyllableTokens = DataLoader.convertToSyllableTokens(dl.cleanSentence(trainingSentence));
			if (convertToSyllableTokens == null) continue;
			System.out.print(trainingSentence + " ");
			trainingSentenceTokens.add(null);
			trainingSentenceTokens.addAll(convertToSyllableTokens.get(0));
		}
		System.out.println();
		
		if (trainingSentenceTokens.size() < (lyricMarkovOrder + trainingSentences.length)) return;
		LinkedList<SyllableToken> prefix = new LinkedList<SyllableToken>(trainingSentenceTokens.subList(0, lyricMarkovOrder));
		fromTokenID = lyricStatesByIndex.addPrefix(prefix);
		for (int j = lyricMarkovOrder; j < trainingSentenceTokens.size(); j++ ) {
			final SyllableToken nextItem = trainingSentenceTokens.get(j);
			if (nextItem == null) {
				Utils.incrementDoubleForKey(lyricPriors, fromTokenID, 1.0); // we do this for every sentence-start token 
				continue;
			}
			prefix.removeFirst();
			prefix.addLast(nextItem);
			
			toTokenID = lyricStatesByIndex.addPrefix(prefix);
			Utils.incrementDoubleForKeys(lyricTransitions, fromTokenID, toTokenID, 1.0);
			
			fromTokenID = toTokenID;
		}
	}

	private static ParsedMusicXMLObject loadSong(String filePath) {
		File file = new File(filePath);
		
		System.out.println("Loading XML for " + file.getName());
		MusicXMLParser musicXMLParser = null;
		try {
			final Document xml = MusicXMLSummaryGenerator.parseXML(new FileInputStream(file));
			musicXMLParser = new MusicXMLParser(file.getName(), xml);
		} catch (Exception e) {
			e.printStackTrace();
		}
		ParsedMusicXMLObject musicXML = musicXMLParser.parse(true);
		if (musicXML == null) {
			throw new RuntimeException("musicXML was null for " + file.getName());
		}
		try {
			StructureExtractor.loadGeneralizedStructureAnnotations(musicXML);
		} catch (Exception e) {
			System.err.println("For " + file.getName() + ":\n");
			throw new RuntimeException(e);
		}
		
		return musicXML;
	}

}
