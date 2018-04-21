package main;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.w3c.dom.Document;

import automaton.Automaton;
import automaton.MatchDFABuilderDFS;
import automaton.RegularConstraintApplier;
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
import dbtb.constraint.StartOfWordConstraint;
import dbtb.constraint.StateConstraint;
import dbtb.data.DataLoader;
import dbtb.data.SyllableToken;
import dbtb.linguistic.paul.Phonetecizer;
import dbtb.linguistic.paul.StressedPhone;
import dbtb.linguistic.syntactic.Pos;
import dbtb.markov.BidirectionalVariableOrderPrefixIDMap;
import dbtb.markov.SparseVariableOrderMarkovModel;
import dbtb.markov.SparseVariableOrderNHMMMultiThreaded;
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

public class AlnNHMMSongGenerator {

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
				return pitch == (((StateToken<PitchToken>)state.get(i)).token.normalizedPitch%12);
			}
			else
				return pitch == (((PitchToken)state.get(i)).normalizedPitch%12);
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

		private double durationInBeats;
		private double beatsSinceOnset;
		private double beat;
		private boolean isRest;

		public RhythmToken(double duration, double currBeatsSinceOnset, double beat, boolean b) {
			this.durationInBeats = duration;
			this.beatsSinceOnset = currBeatsSinceOnset;
			this.beat = beat;
			this.isRest = b;
		}

		public boolean isOnset() {
			return beatsSinceOnset == 0.0;
		}

		@Override
		public String toString() {
			return durationInBeats + ", " + beatsSinceOnset + ", " + beat + ", " + isRest;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			long temp;
			temp = Double.doubleToLongBits(beat);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			temp = Double.doubleToLongBits(beatsSinceOnset);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			temp = Double.doubleToLongBits(durationInBeats);
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
			if (Double.doubleToLongBits(beat) != Double.doubleToLongBits(other.beat))
				return false;
			if (Double.doubleToLongBits(beatsSinceOnset) != Double.doubleToLongBits(other.beatsSinceOnset))
				return false;
			if (Double.doubleToLongBits(durationInBeats) != Double.doubleToLongBits(other.durationInBeats))
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
		
		Double durationInBeats;
		
		public RhythmConstraint(Double durationInBeats) {
			this.durationInBeats = durationInBeats;
		}
		
		@Override
		public boolean isSatisfiedBy(LinkedList<T> state, int i) {
			final T t2 = state.get(i);
			RhythmToken t;
			if (t2 instanceof StateToken)
				t = ((StateToken<RhythmToken>) t2).token;
			else
				t = (RhythmToken) t2;
			
			return t.isOnset() && t.durationInBeats == durationInBeats;
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
				return ((RhythmToken) t).beat == offset;
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
	
	final private static int harmonyMarkovOrder = 1;
	final private static int pitchMarkovOrder = 1;
	final private static int rhythmMarkovOrder = 1;
	final private static int lyricMarkovOrder = 1;
	final private static int INSPIRING_FILE_COUNT = 10;
	public static void main(String[] args) throws Exception {
		
		dbtb.main.Main.setRootPath("/Users/norkish/Archive/2017_BYU/ComputationalCreativity/");
		
		// MUSE
		Muse muse = new Muse();
//		files = muse.findInspiringFiles(INSPIRING_FILE_COUNT);
		
		// 0. STRUCTURE
		// Load a song and structure from file
		ParsedMusicXMLObject structureSong = loadSong(structureFilePath);
		
		
		// 0.5 LOAD TRAINING FROM WIKIFONIA
		trainModels();

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
		
//		for (double[] row : pathsTaken) {
//			for (double col : row) {
//				System.out.print(GeneralizedGlobalStructureInferer.df2.format(col) + " ");
//			}
//			System.out.println();
//		}
		
		List<SortedSet<Integer>> matchingPosesByPos = findMatchingPosesByPos(pathsTaken, ptrMatrix, globalStructureAlignmentParameterization);
		int[][] harmonyMatchConstraintList = new int[2][];
		harmonyMatchConstraintList[0] = createMatchConstraintList(matchingPosesByPos);
		harmonyMatchConstraintList[1] = new int[harmonyMatchConstraintList[0].length];
		Arrays.fill(harmonyMatchConstraintList[1], -1);
		harmonyMatchConstraintList[1][31] = 64;
		boolean[][] harmonyMatchConstraintOutcomeList = new boolean[2][harmonyMatchConstraintList[0].length];
		Arrays.fill(harmonyMatchConstraintOutcomeList[0], true);
		Arrays.fill(harmonyMatchConstraintOutcomeList[1], false);
		
//		int i2 = 0;
//		for (Set<Integer> set : matchingPosesByPos) {
//			System.out.print((i2) +":" + harmonyMatchConstraintList[0][i2] + " -> ");
//			for (Integer integer : set) {
//				System.out.print((integer)+",");
//			}
//			i2++;
//			System.out.println();
//		}

		
		// create the NHMM n of length as long as the song with low markov order d
		// Train NHMM on Wikifonia
		int harmonyMarkovLength = matchingPosesByPos.size();
		SparseVariableOrderMarkovModel<HarmonyToken> harmonyMarkovModel = models.get("Harmony");
		List<List<ConditionedConstraint<HarmonyToken>>> harmonyConstraints = new ArrayList<List<ConditionedConstraint<HarmonyToken>>>();
		List<List<ConditionedConstraint<StateToken<HarmonyToken>>>> harmonyStateConstraints = new ArrayList<List<ConditionedConstraint<StateToken<HarmonyToken>>>>();
		for (int j = 0; j < harmonyMarkovLength; j++) {
			harmonyConstraints.add(new ArrayList<ConditionedConstraint<HarmonyToken>>());
			harmonyStateConstraints.add(new ArrayList<ConditionedConstraint<StateToken<HarmonyToken>>>());
		}
		harmonyConstraints.get(0).add(new ConditionedConstraint<>(new BeatConstraint<>(0.0)));
		harmonyStateConstraints.get(0).add(new ConditionedConstraint<>(new BeatConstraint<>(0.0)));
		harmonyConstraints.get(0).add(new ConditionedConstraint<>(new ChordConstraint<>(new Harmony(new Root(3), new Quality(), null))));
		harmonyStateConstraints.get(0).add(new ConditionedConstraint<>(new ChordConstraint<>(new Harmony(new Root(3), new Quality(), null))));
		harmonyConstraints.get(28).add(new ConditionedConstraint<>(new ChordConstraint<>(new Harmony(new Root(3), new Quality(), null))));
		harmonyStateConstraints.get(28).add(new ConditionedConstraint<>(new ChordConstraint<>(new Harmony(new Root(3), new Quality(), null))));
		
		System.out.println("Building Harmony Automaton");
		
		//TODO: add all control constraints to DFS
		Automaton<HarmonyToken> harmonyAutomaton = MatchDFABuilderDFS.buildEfficiently(harmonyMatchConstraintList, harmonyMatchConstraintOutcomeList, null, harmonyMarkovModel, harmonyConstraints);
		System.out.println("\nBuilding Harmony NHMM");
		
		SparseVariableOrderNHMMMultiThreaded<StateToken<HarmonyToken>> NHMM = RegularConstraintApplier.combineAutomataWithMarkov(harmonyMarkovModel, harmonyAutomaton, harmonyMarkovLength, harmonyStateConstraints);
		System.out.println("Sampling Harmony");
		Map<String,Double> counts = new HashMap<String, Double>();
		Map<String,Double> probs = new HashMap<String, Double>();
		Set<List<StateToken<HarmonyToken>>> harmonyGenerates = new HashSet<List<StateToken<HarmonyToken>>>();
		String printSummary = null;
		int SAMPLE_COUNT = 10000;
		for (int j = 0; j < SAMPLE_COUNT; j++) {
			List<StateToken<HarmonyToken>> harmonyGenerate = NHMM.generate(harmonyMarkovLength);
			printSummary = printSummary(harmonyGenerate, 2.0);
			Utils.incrementDoubleForKey(counts, printSummary);
			probs.put(printSummary, NHMM.probabilityOfSequence(harmonyGenerate.toArray(new Token[0])));
			harmonyGenerates.add(harmonyGenerate);
		}
		harmonyAutomaton = null;
		NHMM = null;
		
		for (String generate : counts.keySet()) {
			System.out.println("\t\tProb:" + probs.get(generate) + ", XProb:" + (counts.get(generate)/SAMPLE_COUNT) + "\t" + generate);
		}
		
		// 3. RHYTHM (must go before lyrics to inform lyrics to maintain rhythmic patterns across verses)
		globalStructureAlignmentParameterization = GeneralizedGlobalStructureInferer.loadInitialPopulationFromFile("rhythm",false).get(0).getSecond();
		matchingSegments = inferMatchingSegments(structureSong,globalStructureAlignmentParameterization);
		pathsTaken = (double[][]) matchingSegments[0];
		ptrMatrix = (char[][]) matchingSegments[1];
		Utils.normalizeByMaxVal(pathsTaken);
		
//		for (double[] row : pathsTaken) {
//			for (double col : row) {
//				System.out.print(GeneralizedGlobalStructureInferer.df2.format(col) + " ");
//			}
//			System.out.println();
//		}
		
		matchingPosesByPos = findMatchingPosesByPos(pathsTaken, ptrMatrix, globalStructureAlignmentParameterization);
		int[] matchConstraintList = createMatchConstraintList(matchingPosesByPos);
		
		boolean[] matchConstraintOutcomeList = new boolean[matchConstraintList.length];
		Arrays.fill(matchConstraintOutcomeList, true);
		
//		i = 0;
//		for (Set<Integer> set : matchingPosesByPos) {
//			System.out.print((i+1) +":" + matchConstraintList[i] + " -> ");
//			for (Integer integer : set) {
//				System.out.print((integer+1)+",");
//			}
//			i++;
//			System.out.println();
//		}
		
		
		// create the NHMM n of length as long as the song with low markov order d
		// Train NHMM on Wikifonia
		int rhythmMarkovLength = matchingPosesByPos.size();
		SparseVariableOrderMarkovModel<RhythmToken> rhythmMarkovModel = models.get("Rhythm");
		List<List<ConditionedConstraint<StateToken<RhythmToken>>>> rhythmStateConstraints = new ArrayList<List<ConditionedConstraint<StateToken<RhythmToken>>>>();
		List<List<ConditionedConstraint<RhythmToken>>> rhythmConstraints = new ArrayList<List<ConditionedConstraint<RhythmToken>>>();
		for (int j = 0; j < rhythmMarkovLength; j++) {
			rhythmStateConstraints.add(new ArrayList<ConditionedConstraint<StateToken<RhythmToken>>>());
			rhythmConstraints.add(new ArrayList<ConditionedConstraint<RhythmToken>>());
		}
		rhythmStateConstraints.get(0).add(new ConditionedConstraint<StateToken<RhythmToken>>(new BeatConstraint<>(0.0)));
		rhythmConstraints.get(0).add(new ConditionedConstraint<RhythmToken>(new BeatConstraint<>(0.0)));
		rhythmStateConstraints.get(28).add(new ConditionedConstraint<StateToken<RhythmToken>>(new RhythmConstraint<>(2.0)));
		rhythmConstraints.get(28).add(new ConditionedConstraint<RhythmToken>(new RhythmConstraint<>(2.0)));
		
		System.out.println("Building Rhythm Automaton");
		
		Automaton<RhythmToken> rhythmAutomaton = MatchDFABuilderDFS.buildEfficiently(matchConstraintList, matchConstraintOutcomeList, rhythmMarkovModel, rhythmConstraints);
		System.out.println("\nBuilding Rhythm NHMM");
		
		SparseVariableOrderNHMMMultiThreaded<StateToken<RhythmToken>> rhythmNHMM = RegularConstraintApplier.combineAutomataWithMarkov(rhythmMarkovModel, rhythmAutomaton, rhythmMarkovLength, rhythmStateConstraints);
		
		// 4. LYRICS
		System.out.println("Computing Abstract Lyric Matching constraints");
		globalStructureAlignmentParameterization = GeneralizedGlobalStructureInferer.loadInitialPopulationFromFile("lyric",false).get(0).getSecond();
		matchingSegments = inferMatchingSegments(structureSong,globalStructureAlignmentParameterization);
		pathsTaken = (double[][]) matchingSegments[0];
		ptrMatrix = (char[][]) matchingSegments[1];
		Utils.normalizeByMaxVal(pathsTaken);
		
//				for (double[] row : pathsTaken) {
//					for (double col : row) {
//						System.out.print(GeneralizedGlobalStructureInferer.df2.format(col) + " ");
//					}
//					System.out.println();
		
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
		
//		for (double[] row : pathsTaken) {
//			for (double col : row) {
//				System.out.print(GeneralizedGlobalStructureInferer.df2.format(col) + " ");
//			}
//			System.out.println();
//		}
		
		matchingPosesByPos = findMatchingPosesByPos(pathsTaken, ptrMatrix, globalStructureAlignmentParameterization);
		matchConstraintList = createMatchConstraintList(matchingPosesByPos);
		
		matchConstraintOutcomeList = new boolean[matchConstraintList.length];
		Arrays.fill(matchConstraintOutcomeList, true);
		
//		i = 0;
//		for (Set<Integer> set : matchingPosesByPos) {
//			System.out.print((i+1) +":" + matchConstraintList[i] + " -> ");
//			for (Integer integer : set) {
//				System.out.print((integer+1)+",");
//			}
//			i++;
//			System.out.println();
//		}

		
		// create the NHMM n of length as long as the song with low markov order d
		// Train NHMM on Wikifonia
		int pitchMarkovLength = matchingPosesByPos.size();
		SparseVariableOrderMarkovModel<PitchToken> pitchMarkovModel = models.get("Pitch");

		Set<List<StateToken<RhythmToken>>> rhythmGenerates = new HashSet<List<StateToken<RhythmToken>>>();
		System.out.println("Sampling Rhythm");
		counts = new HashMap<String, Double>();
		probs = new HashMap<String, Double>();
		for (int j = 0; j < 10*SAMPLE_COUNT; j++) {
			List<StateToken<RhythmToken>> rhythmGenerate = rhythmNHMM.generate(rhythmMarkovLength);
			rhythmGenerates.add(rhythmGenerate);
			printSummary = printSummary(rhythmGenerate, 0.5);
			Utils.incrementDoubleForKey(counts, printSummary);
			probs.put(printSummary, rhythmNHMM.probabilityOfSequence(rhythmGenerate.toArray(new Token[0])));
		}
		rhythmAutomaton = null;
		rhythmNHMM = null;
		
		for (String generate : counts.keySet()) {
			System.out.println("\t\tProb:" + probs.get(generate) + ", XProb:" + (counts.get(generate)/SAMPLE_COUNT) + "\t" + generate);
		}
		
		
		int fileSuffix = 1;
		Automaton<PitchToken> pitchAutomaton = null;
		List<List<ConditionedConstraint<StateToken<PitchToken>>>> pitchStateConstraints = null;
		System.out.println("Trying " + harmonyGenerates.size() + " harmonic progressions");
		for (List<StateToken<HarmonyToken>> harmonyGenerate : harmonyGenerates) {
			System.out.println("TRYING HARMONY:" + printSummary(harmonyGenerate,2.0));
			try {
				pitchStateConstraints = new ArrayList<List<ConditionedConstraint<StateToken<PitchToken>>>>();
				List<List<ConditionedConstraint<PitchToken>>> pitchConstraints = new ArrayList<List<ConditionedConstraint<PitchToken>>>();
				for (int j = 0; j < pitchMarkovLength; j++) {
					final ArrayList<ConditionedConstraint<StateToken<PitchToken>>> pitchStateConstraintsAtJ = new ArrayList<ConditionedConstraint<StateToken<PitchToken>>>();
					pitchStateConstraints.add(pitchStateConstraintsAtJ);
					final ArrayList<ConditionedConstraint<PitchToken>> pitchConstraintsAtJ = new ArrayList<ConditionedConstraint<PitchToken>>();
					pitchConstraints.add(pitchConstraintsAtJ);
					if (j == 28) {
						pitchStateConstraintsAtJ.add(new ConditionedConstraint<StateToken<PitchToken>>(new PitchConstraint<StateToken<PitchToken>>((harmonyGenerate.get(j).token.harmony.root.rootStep + 9)%12)));
						pitchConstraintsAtJ.add(new ConditionedConstraint<PitchToken>(new PitchConstraint<PitchToken>((harmonyGenerate.get(j).token.harmony.root.rootStep + 9)%12)));
					} else {
//					if (j%(2*EVENTS_PER_BEAT) == 0){
						pitchStateConstraintsAtJ.add(new ConditionedConstraint<StateToken<PitchToken>>(new PitchInChordConstraint<StateToken<PitchToken>>(harmonyGenerate.get(j).token.harmony)));
						pitchConstraintsAtJ.add(new ConditionedConstraint<PitchToken>(new PitchInChordConstraint<PitchToken>(harmonyGenerate.get(j).token.harmony)));
					}
				}
	//			pitchConstraints.get(0).add(new ConditionedConstraint<StateToken<PitchToken>>(new BeatConstraint<>(0.0)));
				
				System.out.println("Building Pitch Automaton");
				pitchAutomaton = MatchDFABuilderDFS.buildEfficiently(matchConstraintList, matchConstraintOutcomeList, pitchMarkovModel, pitchConstraints);
	
			} catch (Exception e) {
				System.err.println(e.getMessage());
				continue;
			}
			
			Score newScore = new Score();
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
			newScore.addMeasures(instantiatedMeasures);
			
			System.out.println("\nBuilding Pitch NHMM");
			SparseVariableOrderNHMMMultiThreaded<StateToken<PitchToken>> pitchNHMM = RegularConstraintApplier.combineAutomataWithMarkov(pitchMarkovModel, pitchAutomaton, pitchMarkovLength, pitchStateConstraints);
			//add harmony to score
			int measureNumber = -1;
			Harmony prevHarmony = null, currHarmony;
			for (StateToken<HarmonyToken> stateToken : harmonyGenerate) {
				HarmonyToken hToken = stateToken.token;
				currHarmony = hToken.harmony;
				if (hToken.beat == 0.0) {
					measureNumber++;
				}
				if (currHarmony != prevHarmony || hToken.beat == 0.0) {
					newScore.addHarmony(measureNumber, hToken.beat, hToken.harmony);
					prevHarmony = currHarmony;
				}
			}
			
			System.out.println("Sampling Pitch");
			counts = new HashMap<String, Double>();
			probs = new HashMap<String, Double>();
			List<StateToken<PitchToken>> pitchGenerate = null;
			for (int j = 0; j < SAMPLE_COUNT; j++) {
				pitchGenerate = pitchNHMM.generate(pitchMarkovLength);
				printSummary = printSummary(pitchGenerate, 1.0);
				Utils.incrementDoubleForKey(counts, printSummary);
				probs.put(printSummary, pitchNHMM.probabilityOfSequence(pitchGenerate.toArray(new Token[0])));
			}
			
			
			//TODO:add to score when lyrics and note durations have been sampled
			
			for (String generate : counts.keySet()) {
				System.out.println("\t\tProb:" + probs.get(generate) + ", XProb:" + (counts.get(generate)/SAMPLE_COUNT) + "\t" + generate);
			}
			System.out.println("KEEPING:" + printSummary);
			
			List<StateToken<SyllableToken>> lyricGenerate = null;
			boolean foundLyricsRhythmMatch = false;
			
			List<List<StateToken<RhythmToken>>> rhythmGeneratesList = new ArrayList<List<StateToken<RhythmToken>>>(rhythmGenerates);
			Collections.shuffle(rhythmGeneratesList);
			for (List<StateToken<RhythmToken>> rhythmGenerate : rhythmGeneratesList) {
				System.out.println("TRYING RHYTHM:" + printSummary(rhythmGenerate, 0.5));

				// 4. LYRICS
				// Length (in syllables) is determined from the rhythm
				int[][] lyricMatchConstraintLists = createLyricMatchConstraintLists(lyricMatchingPosesByPos, rhythmGenerate);
				
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
				List<List<ConditionedConstraint<StateToken<SyllableToken>>>> lyricStateConstraints = new ArrayList<List<ConditionedConstraint<StateToken<SyllableToken>>>>();
				List<List<ConditionedConstraint<SyllableToken>>> lyricConstraints = new ArrayList<List<ConditionedConstraint<SyllableToken>>>();
				for (int j = 0; j < lyricMarkovLength; j++) {
					lyricStateConstraints.add(new ArrayList<ConditionedConstraint<StateToken<SyllableToken>>>());
					lyricConstraints.add(new ArrayList<ConditionedConstraint<SyllableToken>>());
				}

				lyricStateConstraints.get(0).add(new ConditionedConstraint<>(new StartOfWordConstraint<>()));
				lyricConstraints.get(0).add(new ConditionedConstraint<>(new StartOfWordConstraint<>()));

				int numNotes = 0;
				double durationSoFar = 0.0;
				double nextPhraseEnding = 8.0;
				// Make sure stressed syllables don't land on offbeats
				for (StateToken<RhythmToken> stateToken : rhythmGenerate) {
					if (stateToken.token.isOnset()) { // if it's a note
						if (!stateToken.token.isRest && durationSoFar % 1.0 != 0.0) { // if it's not a rest and it's offbeat
							lyricStateConstraints.get(numNotes).add(new ConditionedConstraint<>(new AbsoluteStressConstraint<>(0))); // make sure both phrases end 
							lyricConstraints.get(numNotes).add(new ConditionedConstraint<>(new AbsoluteStressConstraint<>(0)));
						}
						durationSoFar += stateToken.token.durationInBeats;
						if (!stateToken.token.isRest) numNotes++;
					}
					
					if (durationSoFar >= nextPhraseEnding) {
						lyricStateConstraints.get(numNotes-1).add(new ConditionedConstraint<>(new EndOfWordConstraint<>())); // make sure both phrases end 
						lyricConstraints.get(numNotes-1).add(new ConditionedConstraint<>(new EndOfWordConstraint<>()));
						nextPhraseEnding += 8.0;
					}
				}
				
				try {
					System.out.println("Building Lyric Automaton");
					
					Automaton<SyllableToken> lyricAutomaton = MatchDFABuilderDFS.buildEfficiently(lyricMatchConstraintLists, lyricMatchConstraintOutcomeList, equivalenceRelations, lyricMarkovModel, lyricConstraints);
					System.out.println("Building Lyric NHMM");
					
					SparseVariableOrderNHMMMultiThreaded<StateToken<SyllableToken>> lyricNHMM = RegularConstraintApplier.combineAutomataWithMarkov(lyricMarkovModel, lyricAutomaton, lyricMarkovLength, lyricStateConstraints);
					
					System.out.println("Sampling Lyrics");
					counts = new HashMap<String, Double>();
					probs = new HashMap<String, Double>();
					for (int j = 0; j < SAMPLE_COUNT; j++) {
						lyricGenerate = lyricNHMM.generate(lyricMarkovLength);
						printSummary = printSummary(lyricGenerate, 0.5);
						Utils.incrementDoubleForKey(counts, printSummary);
						probs.put(printSummary, lyricNHMM.probabilityOfSequence(lyricGenerate.toArray(new Token[0])));
					}
					
					for (String generate : counts.keySet()) {
						System.out.println("\t\tProb:" + probs.get(generate) + ", XProb:" + (counts.get(generate)/SAMPLE_COUNT) + "\t" + generate);
					}
					lyricGenerate = lyricNHMM.generate(lyricMarkovLength);
					printSummary = printSummary(lyricGenerate, 0.5);
					System.out.println("KEEPING:" + printSummary);
				
				} catch (Exception e) {
					System.err.println(e.getMessage());
					continue;
				}
				
				int measure = -1;
				int nextLyricIdx = 0;
				System.out.println("Saving composition...");
				for (int j = 0; j < rhythmGenerate.size(); j++) {
					PitchToken pitchToken = pitchGenerate.get(j).token;
					RhythmToken rhythmToken = rhythmGenerate.get(j).token;
//					System.out.println("Considering note:" + rhythmToken + " with pitch:" + pitchToken);
//					System.out.println("Measure:" + measure);
					if (rhythmToken.beat == 0.0) {
						measure++;
					}
					if (rhythmToken.isOnset()) { //adding a note
						int pitch = rhythmToken.isRest ? Note.REST : pitchToken.normalizedPitch;
						final Measure measure2 = newScore.getMeasures().get(measure);
						List<Note> createTiedNoteWithDuration = MelodyEngineer.createTiedNoteWithDuration(measure2.beatsToDivs((double) rhythmToken.durationInBeats), pitch, measure2.divisionsPerQuarterNote);
						if (!rhythmToken.isRest)
							createTiedNoteWithDuration.get(0).setLyric(new NoteLyric(createNoteLyric(lyricGenerate.get(nextLyricIdx++))), true);
						double currBeat = rhythmToken.beat;
						int currMeasure = measure;
						for (Note note : createTiedNoteWithDuration) {
//							System.out.println("Adding note:" + note + " to msr " + currMeasure + " at beat " + currBeat);
							
							newScore.addNote(currMeasure, currBeat, note);
							currBeat += note.duration / measure2.divisionsPerQuarterNote;
							if (currBeat >= 4.0) {
								currMeasure++;
								currBeat %= 4.0;
							}
						}
					}
				}
				
				Composition composition = new Composition(newScore);
				Files.write(Paths.get("./compositions/newSong" + fileSuffix + ".xml"), composition.toString().getBytes());
				
				Orchestrator orchestrator = new CompingMusicXMLOrchestrator();
				orchestrator.orchestrate(composition);
				Files.write(Paths.get("./compositions/newSong" + fileSuffix + "Orchestrated.xml"), composition.toString().getBytes());
				fileSuffix++;
				break;
			}
			if (!foundLyricsRhythmMatch) {
				System.err.println("Could not find lyrics to match any rhythm");
			}
		}
	}

	private static NoteLyric createNoteLyric(StateToken<SyllableToken> stateToken) {
		SyllableToken sToken = stateToken.token;
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

	private static <T extends Token> String printSummary(List<StateToken<T>> generate, double printInterval) {
		StringBuilder str = new StringBuilder();
		
		for (StateToken<T> stateToken : generate) {
			T token = stateToken.token;
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
				if (rToken.beat % printInterval == 0 && rToken.beatsSinceOnset == 0.0) {
					if (rToken.isRest)
						str.append("(");
					str.append(rToken.beat+1);
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
		
		return str.toString();
	}

	/**
	 * First list is matching lyrics positions
	 * Second list is matching rhyme positions
	 * @param matchingPosesByPos
	 * @param rhythmGenerate
	 * @return
	 */
	private static int[][] createLyricMatchConstraintLists(List<SortedSet<Integer>> matchingPosesByPos, List<StateToken<RhythmToken>> rhythmGenerate) {

		SortedMap<Integer, Integer> oldToNewIdx = new TreeMap<Integer, Integer>();
		
		for (int i = 0; i < rhythmGenerate.size(); i++) {
			RhythmToken rToken = rhythmGenerate.get(i).token;
			if (rToken.isOnset() && !rToken.isRest) {
				// then i represents an index for our Match ConstraintLists
//				System.out.println(i + " is now " + oldToNewIdx.size());
				oldToNewIdx.put(i, oldToNewIdx.size());
			}
		}
		int[][] matchConstraintLists = new int[4][oldToNewIdx.size()];
		
		// create constraints for required non-matches
		matchConstraintLists[2][oldToNewIdx.get(oldToNewIdx.headMap(16).lastKey())] = oldToNewIdx.get(oldToNewIdx.headMap(32).lastKey()) + 1;
		matchConstraintLists[2][oldToNewIdx.get(oldToNewIdx.headMap(32).lastKey())] = oldToNewIdx.get(oldToNewIdx.headMap(48).lastKey()) + 1;
		matchConstraintLists[3][oldToNewIdx.get(oldToNewIdx.headMap(16).lastKey())] = oldToNewIdx.get(oldToNewIdx.headMap(48).lastKey()) + 1;
		matchConstraintLists[3][oldToNewIdx.get(oldToNewIdx.headMap(48).lastKey())] = oldToNewIdx.get(oldToNewIdx.headMap(64).lastKey()) + 1;
		
		// create rhyme constraints (SPECIFIC TO TWINKLE, TWINKLE, LITTLE STAR)
		matchConstraintLists[1][oldToNewIdx.get(oldToNewIdx.headMap(16).lastKey())] = oldToNewIdx.get(oldToNewIdx.headMap(32).lastKey()) + 1;
		matchConstraintLists[1][oldToNewIdx.get(oldToNewIdx.headMap(48).lastKey())] = oldToNewIdx.get(oldToNewIdx.headMap(64).lastKey()) + 1;
		matchConstraintLists[1][oldToNewIdx.get(oldToNewIdx.headMap(80).lastKey())] = oldToNewIdx.get(oldToNewIdx.headMap(96).lastKey()) + 1;
		for (int i = 0; i < matchConstraintLists[1].length; i++) {
			if (matchConstraintLists[1][i] == 0) matchConstraintLists[1][i] = -1;
		}
		
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
	private static void trainModels() {
		
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
				if (harmonyStatesByIndex != null && file.getName().contains("")) {
					harmonyPrefix.addLast(new HarmonyToken(alignmentEvent.harmony,alignmentEvent.beat));
					if (i >= (harmonyMarkovOrder-1)) {
						nextHarmonyPrefixID = harmonyStatesByIndex.addPrefix(harmonyPrefix);
						if (prevHarmonyPrefixID == -1 || ALL_STATES_INITIAL_STATES) {
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
				if (rhythmStatesByIndex != null && file.getName().contains("")) {
					rhythmPrefix.addLast(new RhythmToken(alignmentEvent.note.duration/musicXML.getDivsPerQuarterForAbsoluteMeasure(alignmentEvent.measure), alignmentEvent.currBeatsSinceOnset, alignmentEvent.beat, alignmentEvent.note.pitch == Note.REST));
					if (i >= (rhythmMarkovOrder-1)) {
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
				if (lyricStatesByIndex != null && (file.getName().contains("")) && alignmentEvent.lyricOnset) { //  && alignmentEvent.lyric.syllabic == Syllabic.BEGIN?
					str.append(alignmentEvent.lyric.text);
					if (alignmentEvent.lyric.syllabic == Syllabic.END || alignmentEvent.lyric.syllabic == Syllabic.SINGLE)
						str.append(' ');
				}
			}
			
			if (lyricStatesByIndex != null){
	
				String[] trainingSentences = str.toString().split("(?<=[;.!?:]+) ");

				Integer toTokenID;
				Integer fromTokenID = null;
				for (String trainingSentence : trainingSentences) {
					System.out.println("LYR TRAIN:" + trainingSentence);
					final List<List<SyllableToken>> convertToSyllableTokens = DataLoader.convertToSyllableTokens(dl.cleanSentence(trainingSentence));
					if (convertToSyllableTokens == null) continue;
					List<SyllableToken> trainingSentenceTokens = convertToSyllableTokens.get(0);
					for (SyllableToken syllableToken : trainingSentenceTokens) {
						syllableToken.setPos(Pos.NN); // ignore POS for now
//						syllableToken.setStringRepresentation(syllableToken.getStringRepresentation().replaceAll("[^a-zA-Z']+", ""));
					}
					LinkedList<SyllableToken> prefix = new LinkedList<SyllableToken>(trainingSentenceTokens.subList(0, lyricMarkovOrder));
					toTokenID = lyricStatesByIndex.addPrefix(prefix);
					if (fromTokenID != null) 
						Utils.incrementDoubleForKeys(lyricTransitions, fromTokenID, toTokenID, 1.0);
					fromTokenID = toTokenID;
					for (int j = lyricMarkovOrder; j < trainingSentenceTokens.size(); j++ ) {
						prefix.removeFirst();
						prefix.addLast(trainingSentenceTokens.get(j));
						
						toTokenID = lyricStatesByIndex.addPrefix(prefix);
						Utils.incrementDoubleForKeys(lyricTransitions, fromTokenID, toTokenID, 1.0);
						Utils.incrementDoubleForKey(lyricPriors, fromTokenID, 1.0); // we do this for every token 
						
						fromTokenID = toTokenID;
					}
					Utils.incrementDoubleForKey(lyricPriors, fromTokenID, 1.0); // we do this for every token 
				}
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
