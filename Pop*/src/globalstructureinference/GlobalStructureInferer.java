package globalstructureinference;

import java.awt.Color;
import java.awt.Font;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.tc33.jheatchart.HeatChart;
import org.w3c.dom.Document;

import config.SongConfiguration;
import data.MusicXMLParser;
import data.MusicXMLParser.Note;
import data.MusicXMLSummaryGenerator;
import data.ParsedMusicXMLObject;
import globalstructure.SegmentType;
import globalstructure.StructureExtractor;
import tabcomplete.main.TabDriver;
import utils.Pair;
import utils.Triple;
import utils.Utils;

public class GlobalStructureInferer {

	final private static int DEBUG = 0;
	
	private static DecimalFormat df2 = new DecimalFormat(".##");

	public static class GlobalStructureAlignmentParameterization {

		// pitch weights
		public double pitchesEqual;
		public double bothRests;
		public double oneRests;
		public double neitherRests;
//		public double bothPitchesOnset;
//		public double bothPitchesNotOnset;
//		public double onePitchOnsetOneNot;
		public double pitchDifference;
//
//		// harmony weights
		public double harmonyEqual;
//		public double harmonyDifference;
//		public double chordMatchOnsetScore;
//		public double bothHarmoniesNotOnset;
//		public double oneHarmonyOnsetOneNot;
//
//		// lyric weights
		public double lyricsEqual;
		public double lyricsUnequal;
		public double bothLyricsNull;
//		public double bothLyricsOnset;
//		public double bothLyricsNotOnset;
//		public double oneLyricOnsetOneNot;
//		
//		// measure offset match score
		
		// gap scores
		public double gapOpenScore;
		public double gapExtendScore;
		
		// alignment non-scoring params
		public int distanceFromDiagonalInBeats = 8;
		public int eventsPerBeat = 4; //number of divisions into which the beat should be divided.
		
		public GlobalStructureAlignmentParameterization(String nextLine) {
			String[] nextTokens = nextLine.split(", ");
			int i = 0;
			this.pitchesEqual = Double.parseDouble(nextTokens[i++]);
			this.bothRests = Double.parseDouble(nextTokens[i++]);
			this.oneRests = Double.parseDouble(nextTokens[i++]);
			this.neitherRests = Double.parseDouble(nextTokens[i++]);
//			this.bothPitchesOnset = Double.parseDouble(nextTokens[i++]);
//			this.bothPitchesNotOnset = Double.parseDouble(nextTokens[i++]);
//			this.onePitchOnsetOneNot = Double.parseDouble(nextTokens[i++]);
			this.pitchDifference = Double.parseDouble(nextTokens[i++]);
			this.harmonyEqual = Double.parseDouble(nextTokens[i++]);
//			this.chordMatchOnsetScore = Double.parseDouble(nextTokens[i++]);
//			this.bothHarmoniesNotOnset = Double.parseDouble(nextTokens[i++]);
//			this.oneHarmonyOnsetOneNot = Double.parseDouble(nextTokens[i++]);
			this.lyricsEqual = Double.parseDouble(nextTokens[i++]);
			this.lyricsUnequal = Double.parseDouble(nextTokens[i++]);
			this.bothLyricsNull = Double.parseDouble(nextTokens[i++]);
//			this.bothLyricsOnset = Double.parseDouble(nextTokens[i++]);
//			this.bothLyricsNotOnset = Double.parseDouble(nextTokens[i++]);
//			this.oneLyricOnsetOneNot = Double.parseDouble(nextTokens[i++]);
			this.gapOpenScore = Double.parseDouble(nextTokens[i++]);
			this.gapExtendScore = Double.parseDouble(nextTokens[i++]);
			this.distanceFromDiagonalInBeats = Integer.parseInt(nextTokens[i++]);
			this.eventsPerBeat = Integer.parseInt(nextTokens[i++]);
		}

		public GlobalStructureAlignmentParameterization(GlobalStructureAlignmentParameterization p1,
				GlobalStructureAlignmentParameterization p2) {
			this.pitchesEqual = (rand.nextBoolean()?p1.pitchesEqual:p2.pitchesEqual); 
			this.bothRests = (rand.nextBoolean()?p1.bothRests:p2.bothRests);
			this.oneRests = (rand.nextBoolean()?p1.oneRests:p2.oneRests);
			this.neitherRests = (rand.nextBoolean()?p1.neitherRests:p2.neitherRests);
//			this.bothPitchesOnset = (rand.nextBoolean()?p1.bothPitchesOnset:p2.bothPitchesOnset);
//			this.bothPitchesNotOnset = (rand.nextBoolean()?p1.bothPitchesNotOnset:p2.bothPitchesNotOnset);
//			this.onePitchOnsetOneNot = (rand.nextBoolean()?p1.onePitchOnsetOneNot:p2.onePitchOnsetOneNot);
			this.pitchDifference = (rand.nextBoolean()?p1.pitchDifference:p2.pitchDifference);
//			this.harmonyEqual = (rand.nextBoolean()?p1.harmonyEqual:p2.harmonyEqual);
//			this.chordMatchOnsetScore = (rand.nextBoolean()?p1.chordMatchOnsetScore:p2.chordMatchOnsetScore);
//			this.bothHarmoniesNotOnset = (rand.nextBoolean()?p1.bothHarmoniesNotOnset:p2.bothHarmoniesNotOnset);
//			this.oneHarmonyOnsetOneNot = (rand.nextBoolean()?p1.oneHarmonyOnsetOneNot:p2.oneHarmonyOnsetOneNot);
			this.lyricsEqual = (rand.nextBoolean()?p1.lyricsEqual:p2.lyricsEqual);
			this.lyricsUnequal = (rand.nextBoolean()?p1.lyricsUnequal:p2.lyricsUnequal);
			this.bothLyricsNull = (rand.nextBoolean()?p1.bothLyricsNull:p2.bothLyricsNull);
//			this.bothLyricsOnset = (rand.nextBoolean()?p1.bothLyricsOnset:p2.bothLyricsOnset);
//			this.bothLyricsNotOnset = (rand.nextBoolean()?p1.bothLyricsNotOnset:p2.bothLyricsNotOnset);
//			this.oneLyricOnsetOneNot = (rand.nextBoolean()?p1.oneLyricOnsetOneNot:p2.oneLyricOnsetOneNot);
			this.gapOpenScore = (rand.nextBoolean()?p1.gapOpenScore:p2.gapOpenScore);
			this.gapExtendScore = (rand.nextBoolean()?p1.gapExtendScore:p2.gapExtendScore);
			this.distanceFromDiagonalInBeats = (rand.nextBoolean()?p1.distanceFromDiagonalInBeats:p2.distanceFromDiagonalInBeats);
			this.eventsPerBeat = (rand.nextBoolean()?p1.eventsPerBeat:p2.eventsPerBeat);
		}
		
		@Override
		public String toString() {
			return 
					df2.format(pitchesEqual) + ", " + 
					df2.format(bothRests) + ", " + 
					df2.format(oneRests) + ", " +
					df2.format(neitherRests) + ", " + 
//					df2.format(bothPitchesOnset) + ", " + 
//					df2.format(bothPitchesNotOnset) + ", " +
//					df2.format(onePitchOnsetOneNot) + ", " + 
					df2.format(pitchDifference) + ", " + 
					df2.format(harmonyEqual) + ", " + 
//					df2.format(chordMatchOnsetScore) + ", " + 
//					df2.format(bothHarmoniesNotOnset) + ", " +
//					df2.format(oneHarmonyOnsetOneNot) + ", " + 
					df2.format(lyricsEqual) + ", " + 
					df2.format(lyricsUnequal) + ", " + 
					df2.format(bothLyricsNull) + ", " + 
//					df2.format(bothLyricsOnset) + ", " + 
//					df2.format(bothLyricsNotOnset) + ", " + 
//					df2.format(oneLyricOnsetOneNot) 
					df2.format(gapOpenScore) + ", " + 
					df2.format(gapExtendScore) + ", "
					+ distanceFromDiagonalInBeats + ", " + 
					eventsPerBeat;
		}

		private static double MUTATION_RATE = 0.5;
		private static int MAX_MUTATION_STEP = 5;
		public void mutate() {
			if (rand.nextDouble() < MUTATION_RATE)
				this.pitchesEqual += (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2);
			if (rand.nextDouble() < MUTATION_RATE)
				this.bothRests = (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
			if (rand.nextDouble() < MUTATION_RATE)
				this.oneRests = (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
			if (rand.nextDouble() < MUTATION_RATE)
				this.neitherRests = (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
//			if (rand.nextDouble() < MUTATION_RATE)
//				this.bothPitchesOnset = (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
//			if (rand.nextDouble() < MUTATION_RATE)
//				this.bothPitchesNotOnset = (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
//			if (rand.nextDouble() < MUTATION_RATE)
//				this.onePitchOnsetOneNot = (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
			if (rand.nextDouble() < MUTATION_RATE)
				this.pitchDifference = (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
//			if (rand.nextDouble() < MUTATION_RATE)
//				this.harmonyEqual = (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
//			if (rand.nextDouble() < MUTATION_RATE)
//				this.chordMatchOnsetScore = (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
//			if (rand.nextDouble() < MUTATION_RATE)
//				this.bothHarmoniesNotOnset = (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
//			if (rand.nextDouble() < MUTATION_RATE)
//				this.oneHarmonyOnsetOneNot = (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
			if (rand.nextDouble() < MUTATION_RATE)
				this.lyricsEqual += (rand.nextBoolean()?1:-1) * rand.nextInt(MAX_MUTATION_STEP); 
//				this.lyricsEqual = (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
			if (rand.nextDouble() < MUTATION_RATE)
				this.lyricsUnequal += (rand.nextBoolean()?1:-1) * rand.nextInt(MAX_MUTATION_STEP); 
//				this.lyricsUnequal = (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
			if (rand.nextDouble() < MUTATION_RATE)
				this.bothLyricsNull += (rand.nextBoolean()?1:-1) * rand.nextInt(MAX_MUTATION_STEP);
//				this.bothLyricsNull = (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
//			if (rand.nextDouble() < MUTATION_RATE)
//				this.bothLyricsOnset = (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
//			if (rand.nextDouble() < MUTATION_RATE)
//				this.bothLyricsNotOnset = (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
//			if (rand.nextDouble() < MUTATION_RATE)
//				this.oneLyricOnsetOneNot = (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
			if (rand.nextDouble() < MUTATION_RATE)
				this.gapOpenScore += (rand.nextBoolean()?1:-1) * rand.nextInt(MAX_MUTATION_STEP);
//				this.gapOpenScore = (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
			if (rand.nextDouble() < MUTATION_RATE)
				this.gapExtendScore += (rand.nextBoolean()?1:-1) * rand.nextInt(MAX_MUTATION_STEP);
//				this.gapExtendScore = (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
			if (rand.nextDouble() < MUTATION_RATE) {
				this.distanceFromDiagonalInBeats += (rand.nextBoolean()?1:-1) * rand.nextInt(MAX_MUTATION_STEP);
				if (this.distanceFromDiagonalInBeats > 20) {
					this.distanceFromDiagonalInBeats = 20;
				} else if (this.distanceFromDiagonalInBeats < 0) {
					this.distanceFromDiagonalInBeats = 0;
				}
			}
//			if (rand.nextDouble() < MUTATION_RATE)
//				this.eventsPerBeat = (int) Math.pow(2,rand.nextInt(3));			
		}

		public Integer getSolutionID() {
			final String stringRep = this.toString();
			Integer solutionID = solutionIDMap.get(stringRep);
	    	if (solutionID == null) {
	    		solutionID = solutionIDMap.size();
	    		solutionIDMap.put(stringRep, solutionID);
	    	}
	    	return solutionID;
		}
	}
	
	static int populationSize = 15;
	static int generation = 1;
	private static final File[] files = new File(
			TabDriver.dataDir + "/Wikifonia_edited_xmls").listFiles();
	private static List<ParsedMusicXMLObject> trainingSongs;
	static {
//		PopDriver.annotateSysOutErrCalls();

		trainingSongs = new ArrayList<ParsedMusicXMLObject>();
		for (File file : files) {
//			if (!file.getName().startsWith("Micha")) continue;
			if (!StructureExtractor.annotationsExistForFile(file)) {
				continue;
			}
			System.out.println("Loading XML for " + file.getName());
			MusicXMLParser musicXMLParser = null;
			try {
//				final Document xml = MusicXMLSummaryGenerator.mxlToXML(file);
				final Document xml = MusicXMLSummaryGenerator.parseXML(new FileInputStream(file));
				musicXMLParser = new MusicXMLParser(file.getName(), xml);
//				MusicXMLSummaryGenerator.printDocument(xml, System.out);
			} catch (Exception e) {
				e.printStackTrace();
			}
//			WikifoniaCorrection.applyManualCorrections(musicXMLParser, file.getName());
			ParsedMusicXMLObject musicXML = musicXMLParser.parse(true);
			if (musicXML == null) {
				System.err.println("musicXML was null for " + file.getName());
				continue;
			}
			try {
				StructureExtractor.annotateStructure(musicXML);
			} catch (Exception e) {
				System.err.println("For " + file.getName() + ":\n");
				throw new RuntimeException(e);
			}
			trainingSongs.add(musicXML);
		}
	}

	private final static SegmentType TYPE = SegmentType.CHORUS;
	private final static String POPULATION_FILE = "global_alignment_inference/parameterization_pop_"+ StringUtils.lowerCase(TYPE.toString()) +".txt";
	private final static String HEATMAP_FILE_PREFIX = "global_alignment_inference/"+ StringUtils.lowerCase(TYPE.toString()) +"_visualizations/";
	private final static int TOTAL_GENERATIONS = 100000;
	private static double prevBestAccuracy = 0.0;
	public static void main(String[] args) {
		// create/load initial population of x parameterizations and their accuracy scores when used to
		List<Pair<Double, GlobalStructureAlignmentParameterization>> population = loadInitialPopulation(TYPE);
		System.out.println("Previous Best Accuracy: " + prevBestAccuracy);
		
		for (int i = 0; i < TOTAL_GENERATIONS; i++) {
			generation++;
			// cross-over and mutate the scores, possible modifying just one score at a time?
			List<GlobalStructureAlignmentParameterization> offSpring = generateNewPopulation(population);
			
			// score solutions 
			population.addAll(scoreParameterizations(offSpring, TYPE));
			
			// save/keep the top x parameterizations
			Collections.sort(population, new Comparator<Pair<Double, GlobalStructureAlignmentParameterization>>() {
				@Override
				public int compare(Pair<Double, GlobalStructureAlignmentParameterization> o1,
						Pair<Double, GlobalStructureAlignmentParameterization> o2) {
					final double d = o1.getFirst() - o2.getFirst();
					return d > 0 ? -1 : (d < 0 ? 1 : 0);
				}
			});
			population = population.subList(0, populationSize);
			final Pair<Double, GlobalStructureAlignmentParameterization> best = population.get(0);
			if (best.getFirst() > prevBestAccuracy) {
				prevBestAccuracy = best.getFirst();
//				scoreParameterization(best.getSecond(), TYPE, true); // save best heatmap
			}
			System.out.println(i + "\t" + prevBestAccuracy);

			// print top y parameterizations
			savePopulationToFile(population);
		}
		
		// when parameterizations have settled, test on a song not used in training
		
	}
	
	private static final int LITTER_SIZE = 10;
	private static List<GlobalStructureAlignmentParameterization> generateNewPopulation(
			List<Pair<Double, GlobalStructureAlignmentParameterization>> population) {
		List<GlobalStructureAlignmentParameterization> newPop = new ArrayList<GlobalStructureAlignmentParameterization>();
		
		for (int i = 0; i < LITTER_SIZE; i++) {
			// selection
			int parentIdx = rand.nextInt(population.size());
			GlobalStructureAlignmentParameterization parent1 = population.get(parentIdx).getSecond();
			parentIdx = rand.nextInt(population.size());
			GlobalStructureAlignmentParameterization parent2 = population.get(parentIdx).getSecond();
			
			// crossover
			GlobalStructureAlignmentParameterization newOffspring = new GlobalStructureAlignmentParameterization(parent1,parent2);
			
			// mutation
			newOffspring.mutate();
			
			newPop.add(newOffspring);
		}
		
		return newPop;
	}

	static Map<String, Integer> solutionIDMap = new HashMap<String, Integer>();
	
	private static void savePopulationToFile(List<Pair<Double, GlobalStructureAlignmentParameterization>> population) {
		try(FileWriter fw = new FileWriter(POPULATION_FILE, true);
				BufferedWriter bw = new BufferedWriter(fw);
				PrintWriter out = new PrintWriter(bw))
		{
		    out.println("Generation " + generation);
		    for (Pair<Double, GlobalStructureAlignmentParameterization> pair : population) {
		    	Integer solutionID = pair.getSecond().getSolutionID();
				out.println(solutionID + "\t" + pair.getFirst() + "\t" + pair.getSecond());
			}
		    out.println("*****\n");
		} catch (IOException e) {
		}
	}

	private static List<Pair<Double, GlobalStructureAlignmentParameterization>> scoreParameterizations(List<GlobalStructureAlignmentParameterization> offSpring, SegmentType targetSegment) {
		List<Pair<Double, GlobalStructureAlignmentParameterization>> offSpringPopulation = new ArrayList<Pair<Double, GlobalStructureAlignmentParameterization>>();
		boolean first = true;
		for (GlobalStructureAlignmentParameterization globalStructureAlignmentParameterization : offSpring) {
			double score = scoreParameterization(globalStructureAlignmentParameterization, targetSegment, false);
			first = false;
			offSpringPopulation.add(new Pair<>(score, globalStructureAlignmentParameterization));
		}
		return offSpringPopulation;
	}
	
	private static double scoreParameterization(GlobalStructureAlignmentParameterization globalStructureAlignmentParameterization, SegmentType targetSegment, boolean saveHeatmap) {
		double correct = 0.0;
		double total = 0.0;
		
		if (DEBUG > 0) System.out.println("Scoring parameterization");
		for (ParsedMusicXMLObject song : trainingSongs) {
			if (DEBUG > 0) System.out.println(song.filename);
			// do an alignment for each song in the training set using the parameterization
			Object[] matrices = align(song, globalStructureAlignmentParameterization);
			double[][] alnMatrix = (double[][]) matrices[0];
			char[][] ptrMatrix = (char[][]) matrices[1];
			Utils.normalizeByMaxVal(alnMatrix);
			
			// use the alignment to infer the locations of the target segment type
			Object[] inferredSegments = inferTargetSegmentLocations(alnMatrix, ptrMatrix, globalStructureAlignmentParameterization);
			boolean[] inferredLocationStarts = (boolean[]) inferredSegments[0];
			double[][] pathsTaken = (double[][]) inferredSegments[1];
			
			// Given the inferred locations of the target segment type and the actual global structure, compute the accuracy
			final double computedAccuracy = computeAccuracy(inferredLocationStarts, song.getAlignmentEvents(globalStructureAlignmentParameterization.eventsPerBeat), targetSegment);
			if (DEBUG > 0) System.out.println("Computed Accuracy: " + computedAccuracy);
			
//			if (total == 0.0 && generation % 100 == 0 && saveHeatmap) { // Always just print heatmap for first song
			if (saveHeatmap) { // Always just print heatmap for first song
				saveHeatmap(generation, computedAccuracy, pathsTaken, song.getGlobalStructureBySegmentTokenStart(), targetSegment, globalStructureAlignmentParameterization, song);
			}
			
			correct += computedAccuracy;
			total+=1;
		}
		
		return correct/total;
	}


	static int minEventsFromDiagonal = -1; // how many events apart do events need to be before they can be considered for equality (computed to be about 2 measures)
	
	/**
	 * Given a parameterization and song, do an alignment 
	 * @return 
	 */
	private static Object[] align(ParsedMusicXMLObject song,
			GlobalStructureAlignmentParameterization globalStructureAlignmentParameterization) {
		
//		String test = "IntroVERSEChorusverseChorusBridgeChorusOutro";
		List<ParsedMusicXMLObject.MusicXMLAlignmentEvent> events = song.getAlignmentEvents(globalStructureAlignmentParameterization.eventsPerBeat);
		minEventsFromDiagonal = globalStructureAlignmentParameterization.eventsPerBeat * globalStructureAlignmentParameterization.distanceFromDiagonalInBeats;
		
//		final int matrixDimension = test.length()+1;
		final int matrixDimension = events.size()+1;
		double[][] matrix = new double[matrixDimension][matrixDimension];
		char[][] backtrack = new char[matrixDimension][matrixDimension];

		for (int col = 0; col < matrixDimension; col++) {
			matrix[0][col] = 0; // set top row to 0
			matrix[col][col] = 0; // set diagonal to 0
		}
		
		double matchScore, diag, up, left;
		boolean lyricsEqual, lyricsBothEmpty;
		String mXML1Lyric, mXML2Lyric;
		int pitch1, pitch2;
		for (int row = 1; row < matrixDimension; row++) {
			double[] prevMatrixRow = matrix[row-1];
			double[] currMatrixRow = matrix[row];
			char[] prevBacktrackRow = backtrack[row-1];
			char[] currBackTrackRow = backtrack[row];
			for (int col = row+minEventsFromDiagonal; col < matrixDimension; col++) {
				ParsedMusicXMLObject.MusicXMLAlignmentEvent musicXML1AlignmentEvent = events.get(row-1);
				ParsedMusicXMLObject.MusicXMLAlignmentEvent musicXML2AlignmentEvent = events.get(col-1);
				
				matchScore = 0.0;
				
				mXML1Lyric =  musicXML1AlignmentEvent.strippedLyricLowerCaseText;
				mXML2Lyric = musicXML2AlignmentEvent.strippedLyricLowerCaseText;
				
				lyricsBothEmpty = (mXML1Lyric.isEmpty() && mXML2Lyric.isEmpty());
				lyricsEqual = (mXML1Lyric.equals(mXML2Lyric));
				
				matchScore += lyricsBothEmpty ? globalStructureAlignmentParameterization.bothLyricsNull : (lyricsEqual ? globalStructureAlignmentParameterization.lyricsEqual : globalStructureAlignmentParameterization.lyricsUnequal);
				
				diag = prevMatrixRow[col-1] + matchScore;
				left = currMatrixRow[col-1] + (currBackTrackRow[col - 1] == 'L'? globalStructureAlignmentParameterization.gapExtendScore: globalStructureAlignmentParameterization.gapOpenScore);
				up = prevMatrixRow[col] + (prevBacktrackRow[col] == 'U'? globalStructureAlignmentParameterization.gapExtendScore: globalStructureAlignmentParameterization.gapOpenScore);

				if (diag >= up) {
					if (diag >= left) {
						currMatrixRow[col] = diag;
						currBackTrackRow[col] = 'D';
					} else {
						currMatrixRow[col] = left;
						currBackTrackRow[col] = 'L';
					}
				} else {
					if (up >= left) {
						currMatrixRow[col] = up;
						currBackTrackRow[col] = 'U';
					} else {
						currMatrixRow[col] = left;
						currBackTrackRow[col] = 'L';
					}
				}
				
				if (currMatrixRow[col] < 0)
					currMatrixRow[col] = 0.;
			}
		}
		
//		try(FileWriter fw = new FileWriter(HEATMAP_FILE_PREFIX + song.filename.substring(0, 10) +"_" + generation + "_" + globalStructureAlignmentParameterization.getSolutionID() + ".aln.txt", true);
//				BufferedWriter bw = new BufferedWriter(fw);
//				PrintWriter out = new PrintWriter(bw))
//		{
//			
//			for (int col = 0; col < matrixDimension; col++) {
//				out.print(","+(col > 0?events.get(col-1).lyric:""));
//			}
//			out.println();
//			
//			for (int row = 0; row < matrixDimension; row++) {
//				if (row > 0) out.print(events.get(row-1).lyric);
//				for (int col = 0; col < matrixDimension; col++) {
//					out.print("," + (int)matrix[row][col] + backtrack[row][col]);
//				}			
//				out.println();
//			}
//		} catch (IOException e) {
//		}
		
		return new Object[]{matrix,backtrack};
	}

	/**
	 * Given an alignment, infer the locations of the target segment type
	 * @param ptrMatrix 
	 * @param targetSegment 
	 */
	private static Object[] inferTargetSegmentLocations(double[][] alnMatrix,
			char[][] ptrMatrix, GlobalStructureAlignmentParameterization globalStructureAlignmentParameterization) {
		boolean[] inferredLocations = new boolean[alnMatrix.length-1];
		double[][] pathsTaken = new double[alnMatrix.length][alnMatrix.length];
		int row,col;
		for (row = 0; row < alnMatrix.length; row++) {
			for (col = row; col < alnMatrix[row].length; col++) {
				pathsTaken[row][col] = alnMatrix[row][col];
			}
		}
		
		List<Triple<Double, Integer, Integer>> localMaxima = new ArrayList<Triple<Double, Integer, Integer>>();
		double minThresholdForLocalMaxima = 0.25;
		int maxGraphSizeForCliqueFinding = 20;
		
		boolean isLocalMaxima;
		double val;
		// find all local maxima (above some min threshold), store in list together with the row and col where they appear
		for (row = 0; row < alnMatrix.length; row++) {
			double[] rowVals = alnMatrix[row];
			for (col = row+minEventsFromDiagonal; col < alnMatrix[0].length; col++) {
				if (rowVals[col] > minThresholdForLocalMaxima) {
					val = rowVals[col];
					isLocalMaxima = true;
					
					// see if it's a local maxima within a window of +/- minEventsFromDiagonal
					for (int innerRow = Math.max(0, row-minEventsFromDiagonal-1); isLocalMaxima && innerRow < Math.min(alnMatrix.length, row+minEventsFromDiagonal); innerRow++) {
						for (int innerCol = Math.max(row+minEventsFromDiagonal, col-minEventsFromDiagonal-1); innerCol < Math.min(alnMatrix[0].length, col+minEventsFromDiagonal); innerCol++) {
							if (row == innerRow && col == innerCol) continue;
							if (alnMatrix[innerRow][innerCol] > val) {
								isLocalMaxima = false;
								break;
							}
						}
					}
					
					if (isLocalMaxima) {
						localMaxima.add(new Triple<Double,Integer,Integer>(val, row, col));
					}
				}
			}
		}

		// sort the list by the value
		Collections.sort(localMaxima, (a,b) -> a.getFirst() > b.getFirst() ? -1 : (a.getFirst() < b.getFirst()?1:0));
		if (DEBUG > 1) System.out.println("Found " + localMaxima.size() + " above " + minThresholdForLocalMaxima + ":");
		// take the top 20 and...
		localMaxima = localMaxima.subList(0, Math.min(localMaxima.size(), maxGraphSizeForCliqueFinding));
		// do some sort of weighted max-clique solution based on the row and col vals
		if (DEBUG > 1) {
			System.out.println("Finding max clique for top " + localMaxima.size() + "...");
			for (Triple<Double, Integer, Integer> localMaximum: localMaxima) {
				System.out.println(localMaximum);
			}
		}
		reduceViaMaxClique(localMaxima);
		
		int maxRow,maxCol;
		for (Triple<Double, Integer, Integer> triple : localMaxima) {
			// Backtrack
			maxRow = triple.getSecond();
			maxCol = triple.getThird();
			row = maxRow;
			col = maxCol;
			while (alnMatrix[row][col] > 0.) {
				pathsTaken[row][0] = 1.0;
				pathsTaken[col][0] = 1.0;
				pathsTaken[pathsTaken.length-1][col] = 1.0;
				pathsTaken[pathsTaken.length-1][row] = 1.0;
				pathsTaken[row][col] = 1.0;
				switch(ptrMatrix[row][col]) {
				case 'D':
					row--;
					col--;
					break;
				case 'U':
					row--;
					break;
				case 'L':
					col--;
					break;
					default:
					break;
				}
			}
	
			for (int i = row; i < maxRow; i++) {
				inferredLocations[i] = true;
			}
			for (int i = col; i < maxCol; i++) {
				inferredLocations[i] = true;
			}
		}
		
		return new Object[]{inferredLocations,pathsTaken};
	}
	
	private static void reduceViaMaxClique(List<Triple<Double, Integer, Integer>> localMaxima) {
		
		Map<Integer,Integer> clusteredPointMap = clusterIndicesBasedOnProximityToClusters(localMaxima);
//		Map<Integer,Integer> clusteredPointMap = clusterIndicesBasedOnProximityToNearestPoint(localMaxima);
		
		TreeMap<Integer,TreeMap<Integer,Double>> weightedEdges = new TreeMap<Integer,TreeMap<Integer,Double>>();
		int row,col;
		double weight;
		for (Triple<Double, Integer, Integer> triple : localMaxima) {
			weight = triple.getFirst();
			row = clusteredPointMap.get(triple.getSecond());
			col = clusteredPointMap.get(triple.getThird());
			TreeMap<Integer, Double> edgesForRowNode = weightedEdges.get(row);
			if (edgesForRowNode == null) {
				edgesForRowNode = new TreeMap<Integer, Double>();
				weightedEdges.put(row, edgesForRowNode);
			}
			if (!edgesForRowNode.containsKey(col) || edgesForRowNode.get(col) < weight) {
				edgesForRowNode.put(col, weight);
			}
			TreeMap<Integer, Double> edgesForColNode = weightedEdges.get(col);
			if (edgesForColNode == null) {
				edgesForColNode = new TreeMap<Integer, Double>();
				weightedEdges.put(col, edgesForColNode);
			}
			if (!edgesForColNode.containsKey(row) || edgesForColNode.get(row) < weight) {
				edgesForColNode.put(row, weight);
			}
		}
		
		// find clique via recursive means
		StopWatch watch = new StopWatch();
		
		watch.start();
		Pair<Set<Integer>, Double> maxCliqueAndWeight = findMaxCliqueViaTaylor(weightedEdges);
        watch.suspend();
        long time1 = watch.getTime();
        if (DEBUG > 1) System.out.println("In " + time1 + " ms the taylor max clique finder found the clique: \n" + maxCliqueAndWeight);
//        watch.resume();
//		Pair<Set<Integer>, Double> maxCliqueAndWeight2 = findMaxCliqueViaLoop(weightedEdges);
//        watch.suspend();
//        long time2 = watch.getTime();
//        System.out.println("In " + (time2 - time1) + " ms the looping max clique finder found the clique: \n" + maxCliqueAndWeight2);
//		watch.resume();
//		Pair<Set<Integer>, Double> maxCliqueAndWeight3 = findMaxCliqueViaRecursion(weightedEdges);
//		watch.suspend();
//		long time3 = watch.getTime();
//		System.out.println("In " + (time3 - time2) + " ms the recursive max clique finder found the clique: \n" + maxCliqueAndWeight3 );
//		
//		if (time2 - time1 > 10){
//			maxCliqueAndWeight = findMaxCliqueViaTaylor(weightedEdges);
//		}

		final Set<Integer> maxClique = maxCliqueAndWeight.getFirst();
		int maxCliqueSize = maxClique.size();
		
		if (DEBUG > 1) System.out.println("\nMax clique found " + maxCliqueSize + " mutually matching choruses");
		
		// now prune localMaxima using maxclique–both row and col must be in maxclique
		if (DEBUG > 1) System.out.println("\nLocalMaxima:");
		for (int i = localMaxima.size()-1; i >= 0; i--) {
			Triple<Double, Integer, Integer> candidate = localMaxima.get(i);
			if (!maxClique.contains(clusteredPointMap.get(candidate.getSecond())) || !maxClique.contains(clusteredPointMap.get(candidate.getThird()))) {
				localMaxima.remove(i);
			} else {
				if (DEBUG > 1) System.out.println(candidate);
			}
		}
	}
	
	private static int taylorMaxCliqueSize;
	private static double taylorMaxCliqueWeight;
	private static Set<Integer> taylorMaxClique;
	
	private static Set<Integer> taylorCurrClique;
	
	/**
	 * Implemented from Alg 1 pseudocode in https://arxiv.org/pdf/1209.5818.pdf
	 * @param weightedEdges
	 * @return
	 */
	private static Pair<Set<Integer>, Double> findMaxCliqueViaTaylor(
			TreeMap<Integer, TreeMap<Integer, Double>> weightedEdges) {
		taylorMaxCliqueSize = 0;
		taylorMaxCliqueWeight = -1.0;
		taylorMaxClique = new HashSet<Integer>();
		TreeSet<Integer> U;
		for (Integer v_i : weightedEdges.navigableKeySet()) {
			final TreeMap<Integer, Double> v_i_neighbors = weightedEdges.get(v_i);
			if (v_i_neighbors.size()+1 >= taylorMaxCliqueSize) {
				U = new TreeSet<Integer>();
				taylorCurrClique = new HashSet<Integer>();
				taylorCurrClique.add(v_i);
				for (Integer v_j : v_i_neighbors.navigableKeySet()) {
					if (v_j > v_i) { // because nodes are sorted by number, v_j > v_i => j > i
						if (weightedEdges.get(v_j).size() >= taylorMaxCliqueSize) {
							U.add(v_j);
						}
					}
				}
				taylorCliqueSubroutine(weightedEdges, U, 1, 0.0);
			}
		}
		
		return new Pair<Set<Integer>,Double>(taylorMaxClique,taylorMaxCliqueWeight);
	}

	private static void taylorCliqueSubroutine(TreeMap<Integer, TreeMap<Integer, Double>> weightedEdges,
			TreeSet<Integer> U, int size, double weight) {
		if (U.isEmpty()) {
			if (size > taylorMaxCliqueSize || (size == taylorMaxCliqueSize && weight > taylorMaxCliqueWeight)) {
				taylorMaxCliqueSize = size;
				taylorMaxCliqueWeight = weight;
				taylorMaxClique = new HashSet<Integer>(taylorCurrClique);
			}
			return;
		}
		while (!U.isEmpty()) {
			if ((size + U.size()) < taylorMaxCliqueSize) {
				return;
			}
			final Integer u = U.first();
			U.remove(u);
			TreeSet<Integer> neighbors_prime_u = new TreeSet<Integer>();
			
			TreeMap<Integer, Double> u_neighbors = weightedEdges.get(u);
			for (Integer w : u_neighbors.navigableKeySet()) {
				if (weightedEdges.get(w).size() >= taylorMaxCliqueSize && U.contains(w)) {
					neighbors_prime_u.add(w);
				}
			}

			// calculate weight added to clique from including u
			double addedWeight = 0.0;
			for (Integer prev_u : taylorCurrClique) {
				Double edgeWeight = u_neighbors.get(prev_u);
				if (edgeWeight != null) {
					addedWeight += edgeWeight;
				}
			}
			
			taylorCurrClique.add(u);
			taylorCliqueSubroutine(weightedEdges, neighbors_prime_u, size + 1, weight + addedWeight);
			taylorCurrClique.remove(u);
		}
		
	}

	private static Pair<Set<Integer>, Double> findMaxCliqueViaLoop(TreeMap<Integer, TreeMap<Integer, Double>> weightedEdges) {
		// TODO Auto-generated method stub
		
		Pair<Set<Integer>, Double> maxCliqueAndWeightSoFar = new Pair<Set<Integer>, Double>(new HashSet<Integer>(),-1.0); // default if no clique is found
		int maxCliqueSizeSoFar = 0;
		double maxCliqueWeightSoFar = 0.;
		
		List<Integer> sortedNodeList = new ArrayList<Integer>(weightedEdges.descendingKeySet());
		// for each node in the keyset of edges (in reverse order to be facilitate terminating early)
		for (Integer node: sortedNodeList) {
			// find the max clique that 
			// a) includes that node from the set of edges from that node
			final SortedSet<Integer> edgesForNode = weightedEdges.get(node).navigableKeySet();
			if (edgesForNode.size() < maxCliqueSizeSoFar) {
				continue;
			}
			// b) excluding all other nodes that have already been considered previously
			
			SortedSet<Integer> setToComputeCliqueFor = new TreeSet<Integer>();
			setToComputeCliqueFor.add(node);
			for (Integer nodeToAdd: edgesForNode) {
				if (nodeToAdd == node) break;
				setToComputeCliqueFor.add(nodeToAdd);
			}
			// c) that has the potential of being bigger than maxCliqueSoFar
			if (setToComputeCliqueFor.size() <= maxCliqueSizeSoFar) {
				continue;
			}
			
	        Set<Pair<Set<Integer>, Double>> cliquePowerSet = cliquePowerSet(setToComputeCliqueFor,weightedEdges);
	        
	        double weight;
	        int cliqueSize;
	        for (Pair<Set<Integer>, Double> cliqueAndWeight : cliquePowerSet) {
	        	weight = cliqueAndWeight.getSecond();
	        	cliqueSize = cliqueAndWeight.getFirst().size();
				if (cliqueSize > maxCliqueSizeSoFar || cliqueSize == maxCliqueSizeSoFar && weight > maxCliqueWeightSoFar) {
					maxCliqueSizeSoFar = cliqueSize;
					maxCliqueAndWeightSoFar = cliqueAndWeight;
					maxCliqueWeightSoFar = weight;
				}
			}
		}
		
		return maxCliqueAndWeightSoFar;
	}

	private static Pair<Set<Integer>,Double> findMaxCliqueViaRecursion(TreeMap<Integer, TreeMap<Integer, Double>> weightedEdges) {
		int maxCliqueSize = 0;
		double maxCliqueWeight = 0.;
		Pair<Set<Integer>,Double> maxCliqueAndWeight = new Pair<Set<Integer>,Double>(new HashSet<Integer>(),-1.0); // default if no clique is found
        Set<Pair<Set<Integer>,Double>> cliquePowerSet = cliquePowerSet(weightedEdges.keySet(),weightedEdges);
        Double weight;
        int cliqueSize;
        for (Pair<Set<Integer>,Double> cliqueAndWeight : cliquePowerSet) {
        	weight = cliqueAndWeight.getSecond();
        	cliqueSize = cliqueAndWeight.getFirst().size();
			if (cliqueSize > maxCliqueSize || cliqueSize == maxCliqueSize && weight > maxCliqueWeight) {
				maxCliqueSize = cliqueSize;
				maxCliqueWeight = weight;
				maxCliqueAndWeight = cliqueAndWeight;
			}
		}
		return maxCliqueAndWeight;
	}

	private static Set<Pair<Set<Integer>,Double>> cliquePowerSet(Set<Integer> set, TreeMap<Integer, TreeMap<Integer, Double>> weightedEdges) {
		Set<Pair<Set<Integer>,Double>> outerSet = new HashSet<Pair<Set<Integer>,Double>>();
		
		final double cliqueWeight = isClique(set,weightedEdges);
		if (cliqueWeight != -1.0) {
	//		  add set to outerset;
			outerSet.add(new Pair<Set<Integer>,Double>(set, cliqueWeight));
		}
		
//		  for each element in set
		for (Integer node : set) {
			Set<Integer> subset = new HashSet<Integer>();
			subset.addAll(set);
//		   let subset = set excluding element,
			subset.remove(node);
//		   add powerset(subset) to outerset
			outerSet.addAll(cliquePowerSet(subset, weightedEdges));
		}
	  
		return outerSet;      
	}

	private static double isClique(Set<Integer> set, TreeMap<Integer, TreeMap<Integer, Double>> weightedEdges) {
		double cliqueWeight = 0;
		Double weight;
		for (Integer node1 : set) {
			TreeMap<Integer, Double> edgesFromNode1 = weightedEdges.get(node1);
			for (Integer node2 : set) {
				if (node2 <= node1) continue;
				weight = edgesFromNode1.get(node2);
				if (weight == null) {
					return -1.;
				} else {
					cliqueWeight += weight;
				}
			}
		}
		
		return cliqueWeight;
	}

	private static Map<Integer, Integer> clusterIndicesBasedOnProximityToClusters(List<Triple<Double, Integer, Integer>> localMaxima) {
		Map<Integer, Integer> clusteredPointMap = new TreeMap<Integer,Integer>();
		
		// one entry for each cluster; key is cluster mean, value is list of points belonging to that cluster
		TreeMap<Double, List<Integer>> clustersByClusterCenter = new TreeMap<Double, List<Integer>>();
		
		if (DEBUG > 1) System.out.println("\nClustering...");
		for (Triple<Double, Integer, Integer> localMaximum : localMaxima) {
			int songSpot = localMaximum.getSecond();
			List<Integer> songSpots = clustersByClusterCenter.get((double) songSpot);
			if (songSpots == null) {
				songSpots = new ArrayList<Integer>();
				clustersByClusterCenter.put((double) songSpot, songSpots);
			}
			songSpots.add(songSpot);
			songSpot = localMaximum.getThird();
			songSpots = clustersByClusterCenter.get((double) songSpot);
			if (songSpots == null) {
				songSpots = new ArrayList<Integer>();
				clustersByClusterCenter.put((double) songSpot, songSpots);
			}
			songSpots.add(songSpot);
		}
		
		boolean pointsClustered;
		Double firstClusterMeanToCombine;
		Double secondClusterMeanToCombine;
		double prevClusterMean;
		double distance, minDistance;
		do {
			pointsClustered = false;
			minDistance = Double.MAX_VALUE;
			firstClusterMeanToCombine = -1.0;
			secondClusterMeanToCombine = -1.0;
			prevClusterMean = -1.0;
			
			// while there exists a point that is within minEventsFromDiagonal from a cluster mean
			// add the point that is closest
			for (Double clusterMean : clustersByClusterCenter.navigableKeySet()) {
				if (prevClusterMean == -1.0) {
					prevClusterMean = clusterMean;
					continue;
				}
				
				distance = clusterMean - prevClusterMean;
				if (distance < minEventsFromDiagonal && distance < minDistance) {
					minDistance = distance;
					firstClusterMeanToCombine = prevClusterMean;
					secondClusterMeanToCombine = clusterMean;
				}
				prevClusterMean = clusterMean;
			}
			
			if (minDistance < Double.MAX_VALUE) {
				pointsClustered = true;
				List<Integer> pointsInFirstCluster = clustersByClusterCenter.remove(firstClusterMeanToCombine);
				List<Integer> pointsInSecondCluster = clustersByClusterCenter.remove(secondClusterMeanToCombine);
				final int firstClusterSize = pointsInFirstCluster.size();
				final int secondClusterSize = pointsInSecondCluster.size();
				double newClusterMean = (firstClusterMeanToCombine * firstClusterSize + secondClusterMeanToCombine * secondClusterSize ) / (firstClusterSize + secondClusterSize);
				pointsInFirstCluster.addAll(pointsInSecondCluster);
				clustersByClusterCenter.put(newClusterMean, pointsInFirstCluster);
			}
		} while(pointsClustered);
		
		if (DEBUG > 1) System.out.println("Clustering produced " + clustersByClusterCenter.size() + " clusters from " + localMaxima.size()*2 + " points:");

		for (Double clusterMean : clustersByClusterCenter.navigableKeySet()) {
			final int roundedClusterMean = (int) Math.round(clusterMean);
			List<Integer> points = clustersByClusterCenter.get(clusterMean);
			for (Integer point : points) {
				clusteredPointMap.put(point, roundedClusterMean);
				if (DEBUG > 1) System.out.println(point + " -> " + roundedClusterMean);
			}
		}
		
		return clusteredPointMap;
	}
	
	private static Map<Integer, Integer> clusterIndicesBasedOnProximityToNearestPoint(List<Triple<Double, Integer, Integer>> localMaxima) {
		Map<Integer, Integer> clusteredPointMap = new HashMap<Integer,Integer>();
		
		if (DEBUG > 1) System.out.println("\nClustering...");
		List<Integer> allPoints = new ArrayList<Integer>();
		for (Triple<Double, Integer, Integer> localMaximum : localMaxima) {
			allPoints.add(localMaximum.getSecond());
			allPoints.add(localMaximum.getThird());
		}
		Collections.sort(allPoints);
		
		int clusterSum = 0;
		int clusterCount = 0;
		int prevPoint = -1;
		int clusterStartIdx = 0;
		int nextPoint;
		int numberOfClusters = 0;
		for (int nextPointIdx = 0; nextPointIdx < allPoints.size(); nextPointIdx++ ) {
			nextPoint = allPoints.get(nextPointIdx);
			if (prevPoint != -1 && nextPoint - prevPoint <= minEventsFromDiagonal) {
				clusterSum += nextPoint;
				clusterCount++;
			} else {
				// close up previous cluster
				if (clusterCount > 0) {
					int clusterCenter = clusterSum / clusterCount;
					for (int pointIdx = clusterStartIdx; pointIdx < nextPointIdx; pointIdx++) {
						clusteredPointMap.put(allPoints.get(pointIdx), clusterCenter);
						if (DEBUG > 2) System.out.println(allPoints.get(pointIdx) + " -> " + clusterCenter);
					}
					numberOfClusters++;
				}
				// start next cluster
				clusterCount = 1;
				clusterSum = nextPoint;
				clusterStartIdx = nextPointIdx;
			}
			prevPoint = nextPoint;
		}
		// close up last cluster
		if (clusterCount > 0) {
			int clusterCenter = clusterSum / clusterCount;
			for (int pointIdx = clusterStartIdx; pointIdx < allPoints.size(); pointIdx++) {
				clusteredPointMap.put(allPoints.get(pointIdx), clusterCenter);
				if (DEBUG > 2) System.out.println(allPoints.get(pointIdx) + " -> " + clusterCenter);
			}
			numberOfClusters++;
		}
		if (DEBUG > 1) System.out.println("Clustering produced " + numberOfClusters + " clusters");
		
		return clusteredPointMap;
	}

	private static boolean colIsTooCloseToExistingChorus(int col, List<Integer> inferredLocations, int minChorusLength) {
		for (int prevMaxColIdx = 3; prevMaxColIdx < inferredLocations.size(); prevMaxColIdx += 4) {
			int prevMaxCol = inferredLocations.get(prevMaxColIdx);
			if (Math.abs(prevMaxCol-col) < minChorusLength) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Given the inferred locations of the target segment type and the actual global structure, compute the accuracy 
	 * @param targetSegment 
	 */
	private static double computeAccuracy(boolean[] inferredChorusLocation,
			List<ParsedMusicXMLObject.MusicXMLAlignmentEvent> songEvents, SegmentType targetSegment) {

		boolean inferredChorus = false, actualChorus;
		int inferredLocationStartsIdx = 0;
		int correct = 0;
		for (int i = 0; i < songEvents.size(); i++) {
			inferredChorus = inferredChorusLocation[i];
			actualChorus = (songEvents.get(i).segmentType == targetSegment || targetSegment == SegmentType.CHORUS && (songEvents.get(i).segmentType == SegmentType.TAGCHORUS));
			 
			if (actualChorus == inferredChorus) {
				 correct++;
			}
		}
		
		return 1.0 * correct / songEvents.size();
	}

	/**
	 * Given a solution alignment matrix, a score, and the generation, output a heatmap representation of the matrix
	 * @param song 
	 */
	private static void saveHeatmap(int generation, double computedAccuracy, double[][] pathsTaken,
			SortedMap<Integer, SortedMap<Double, SegmentType>> globalStructureBySegmentTokenStart,
			SegmentType targetSegment,
			GlobalStructureAlignmentParameterization globalStructureAlignmentParameterization, ParsedMusicXMLObject song) {
		
		List<ParsedMusicXMLObject.MusicXMLAlignmentEvent> alignmentEvents = song.getAlignmentEvents(globalStructureAlignmentParameterization.eventsPerBeat);
		
		int chartXDimension = alignmentEvents.size() + 1; 
		int chartYDimension = alignmentEvents.size() + 1; 

		String[] xValues = new String[chartXDimension];
		String[] yValues = new String[chartYDimension];
		double[][] chartValues = new double[chartYDimension][chartXDimension];
		
		// set axis labels
		for (int i = 0; i < xValues.length; i++) {
			if (i != 0 && (alignmentEvents.get(i-1).segmentType == targetSegment || targetSegment == SegmentType.CHORUS && (alignmentEvents.get(i-1).segmentType == SegmentType.TAGCHORUS))) {
				xValues[i] = "•";
				yValues[i] = "•";
			} else {
				xValues[i] = "";
				yValues[i] = "";
			}
		}
		
//		// populate heatchart
		for (int row = 0; row < pathsTaken.length; row++) {
			final double[] rowVals = pathsTaken[row];
			final double[] chartRowValues = chartValues[row];
			for (int col = 0; col < rowVals.length; col++) {
				chartRowValues[col] = rowVals[col];
			}
		}
		
//		// populate heatchart
//		for (int row = 0; row < chartValues.length; row++) {
//			for (int col = 0; col < chartValues[0].length; col++) {
//				chartValues[row][col] = 1.0 - (1.0*row/(chartXDimension));
//			}
//		}
		
//		Utils.normalizeByFirstDimension(chartValues);

		HeatChart chart = new HeatChart(chartValues);
		chart.setHighValueColour(Color.RED);
		chart.setLowValueColour(Color.BLUE);
		
		chart.setYAxisLabel(song.filename);
		chart.setXAxisLabel(song.filename);
		chart.setTitle("Generation:" + generation + " Parameters (=,≠,ø,O,E,#):" + globalStructureAlignmentParameterization.toString() + " Target:" + targetSegment + " Accuracy:" + computedAccuracy);
		chart.setXValues(xValues);
		chart.setYValues(yValues);
		chart.setTitleFont(new Font("Times", Font.BOLD, (int) (chartXDimension/3.0)));
		chart.setAxisLabelsFont(new Font("Times", Font.BOLD, (int) (chartXDimension/3.0)));
		chart.setAxisValuesFont(new Font("Times", Font.BOLD, (int) (chartXDimension/3.0)));
//		chart.setCellSize(new Dimension(5,5));
		
		try {
			final String pathname = HEATMAP_FILE_PREFIX + song.filename +"_gen" + generation + "_id" + globalStructureAlignmentParameterization.getSolutionID() + ".jpeg";
			chart.saveToFile(new File(pathname));
		} catch (IOException e) {
			e.printStackTrace();
		}		
		
	}

	private final static Random rand = new Random(SongConfiguration.randSeed);
	private static List<Pair<Double, GlobalStructureAlignmentParameterization>> loadInitialPopulation(SegmentType targetSegment) {
		List<Pair<Double, GlobalStructureAlignmentParameterization>> population = new ArrayList<Pair<Double, GlobalStructureAlignmentParameterization>>();
		try {
			Scanner fileScanner = new Scanner(new File(POPULATION_FILE));
			System.out.println("Loading initial population from file");
			while (fileScanner.hasNextLine()) {
				generation = Integer.parseInt(fileScanner.nextLine().split(" ")[1]);
				population = new ArrayList<Pair<Double, GlobalStructureAlignmentParameterization>>();
				while (fileScanner.hasNextLine()){
					String nextLine = fileScanner.nextLine();
					if (nextLine.startsWith("*****")) {
						fileScanner.nextLine();
						break;
					}
					String[] nextTokens = nextLine.split("\\t",3);
					final GlobalStructureAlignmentParameterization globalStructureAlignmentParameterization = new GlobalStructureAlignmentParameterization(nextTokens[2]);
					int solutionID = Integer.parseInt(nextTokens[0]);
					Integer existingSolutionID = solutionIDMap.get(globalStructureAlignmentParameterization.toString());
					
					if (existingSolutionID == null) {
						solutionIDMap.put(globalStructureAlignmentParameterization.toString(), solutionID);
					} else {
						assert(existingSolutionID == solutionID);
					}
					
					if (population.size() < populationSize) {
						double score = Double.parseDouble(nextTokens[1]);
						population.add(new Pair<Double, GlobalStructureAlignmentParameterization>(score,globalStructureAlignmentParameterization));
					}
				}
			}
			fileScanner.close();
		} catch (FileNotFoundException e) {
			System.out.println("Initializing population of parameterizations");
			// need to create initial population!
			for (int i = 0; i < populationSize; i++) {
				StringBuilder str = new StringBuilder();
				for (int j = 0; j < 11; j++) {
					double var1 = rand.nextInt(7)-3;//rand.nextDouble() * 0.25;
					str.append(var1);
					str.append(", ");
				}
				str.append(rand.nextInt(5)+6); // distance from diagonal
				str.append(", ");
				str.append((int) Math.pow(2,rand.nextInt(2))); // events per beat
				final GlobalStructureAlignmentParameterization globalStructureAlignmentParameterization = new GlobalStructureAlignmentParameterization(str.toString());
				System.out.println("\tScoring initial parameterization:" + globalStructureAlignmentParameterization.toString());
				final double score = scoreParameterization(globalStructureAlignmentParameterization, targetSegment, false);
				population.add(new Pair<Double, GlobalStructureAlignmentParameterization>(score,globalStructureAlignmentParameterization));
			}
			Collections.sort(population, new Comparator<Pair<Double, GlobalStructureAlignmentParameterization>>() {
				@Override
				public int compare(Pair<Double, GlobalStructureAlignmentParameterization> o1,
						Pair<Double, GlobalStructureAlignmentParameterization> o2) {
					final double d = o1.getFirst() - o2.getFirst();
					return d > 0 ? -1 : (d < 0 ? 1 : 0);
				}
			});
			Pair<Double, GlobalStructureAlignmentParameterization> initBest = population.get(0);
			scoreParameterization(initBest.getSecond(), targetSegment, true); // save initial heatmap

			savePopulationToFile(population);
		}
		prevBestAccuracy = population.get(0).getFirst();

		return population;
	}

}
