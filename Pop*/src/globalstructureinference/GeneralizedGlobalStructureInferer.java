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
import java.lang.reflect.InvocationTargetException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.tc33.jheatchart.HeatChart;
import org.w3c.dom.Document;

import config.SongConfiguration;
import data.MusicXMLParser;
import data.MusicXMLParser.Harmony;
import data.MusicXMLParser.Note;
import data.MusicXMLParser.NoteLyric;
import data.MusicXMLSummaryGenerator;
import data.ParsedMusicXMLObject;
import data.ParsedMusicXMLObject.MusicXMLAlignmentEvent;
import globalstructure.StructureExtractor;
import tabcomplete.main.TabDriver;
import tabcomplete.rhyme.HirjeeMatrix;
import tabcomplete.rhyme.RhymeStructureAnalyzer;
import tabcomplete.rhyme.StressedPhone;
import utils.Pair;
import utils.Triple;
import utils.Utils;

public class GeneralizedGlobalStructureInferer {

	final private static int DEBUG = 0;
	
	private final static DecimalFormat df2 = new DecimalFormat("#.##");
	private final static DecimalFormat df3 = new DecimalFormat("#.###");
	protected final static Random rand = new Random(SongConfiguration.randSeed);

	public static abstract class GeneralizedGlobalStructureAlignmentParameterization {
		
		private static final int MIN_DISTANCE_FROM_DIAGNOAL_IN_BEATS = 2;
		private static final int MAX_DISTANCE_FROM_DIAGNOAL_IN_BEATS = 20;
		protected static double MUTATION_RATE = 0.2;
		protected static int MAX_MUTATION_STEP = 10;
		protected static final int NUM_PARAMS_IN_SUPER = 6;
		protected static final int MIN_THRESHOLD = 1;
		
		// gap scores
		public double gapOpenScore;
		public double gapExtendScore;
		
		// alignment non-scoring params
		public double minThresholdForLocalMaxima;
		public int distanceFromDiagonalInBeats = 6;
		public int eventsPerBeat = 2; //number of divisions into which the beat should be divided.
		
//		// measure offset match score
		public double haveSameMeasureOffset;
		public double haveDifferentMeasureOffset;
		public double measureOffsetDifference;
//		public double haveSameMeasureLeadOffset;
//		public double haveDifferentMeasureLeadOffset;
//		public double measureOffsetLeadDifference;

		public GeneralizedGlobalStructureAlignmentParameterization() {
			gapOpenScore = rand.nextInt(7)-3;
			gapExtendScore = rand.nextInt(7)-3;
			minThresholdForLocalMaxima = rand.nextDouble() * 20 + MIN_THRESHOLD;
//			distanceFromDiagonalInBeats = rand.nextInt(5)+6; // distance from diagonal
//			eventsPerBeat = (int) Math.pow(2,rand.nextInt(2)); // events per beat
			haveSameMeasureOffset = rand.nextInt(7)-3;
			haveDifferentMeasureOffset = rand.nextInt(7)-3;
			measureOffsetDifference = rand.nextInt(7)-3;
//			haveSameMeasureLeadOffset = rand.nextInt(7)-3;
//			haveDifferentMeasureLeadOffset = rand.nextInt(7)-3;
//			measureOffsetLeadDifference = rand.nextInt(7)-3;
		}
		
		public GeneralizedGlobalStructureAlignmentParameterization(GeneralizedGlobalStructureAlignmentParameterization p1, GeneralizedGlobalStructureAlignmentParameterization p2){
			
			this.gapOpenScore = (rand.nextBoolean() ? p1.gapOpenScore:p2.gapOpenScore);;
			this.gapExtendScore = (rand.nextBoolean()? p1.gapExtendScore:p2.gapExtendScore);
			this.minThresholdForLocalMaxima = (rand.nextBoolean() ? p1.minThresholdForLocalMaxima:p2.minThresholdForLocalMaxima);
//			this.distanceFromDiagonalInBeats = (rand.nextBoolean() ? p1.distanceFromDiagonalInBeats:p2.distanceFromDiagonalInBeats);
//			this.eventsPerBeat = (rand.nextBoolean() ? p1.eventsPerBeat:p2.eventsPerBeat);
			this.haveSameMeasureOffset = (rand.nextBoolean()?p1.haveSameMeasureOffset:p2.haveSameMeasureOffset);
			this.haveDifferentMeasureOffset = (rand.nextBoolean()?p1.haveDifferentMeasureOffset:p2.haveDifferentMeasureOffset);
			this.measureOffsetDifference = (rand.nextBoolean()?p1.measureOffsetDifference:p2.measureOffsetDifference);
//			this.haveSameMeasureLeadOffset = (rand.nextBoolean()?p1.haveSameMeasureOffset:p2.haveSameMeasureOffset);
//			this.haveDifferentMeasureLeadOffset = (rand.nextBoolean()?p1.haveDifferentMeasureOffset:p2.haveDifferentMeasureOffset);
//			this.measureOffsetLeadDifference = (rand.nextBoolean()?p1.measureOffsetDifference:p2.measureOffsetDifference);
		}

		public GeneralizedGlobalStructureAlignmentParameterization(String[] nextTokens) {
			int i = 0;
			this.gapOpenScore = Double.parseDouble(nextTokens[i++]);
			this.gapExtendScore = Double.parseDouble(nextTokens[i++]);
			this.minThresholdForLocalMaxima = Double.parseDouble(nextTokens[i++]);
//			this.distanceFromDiagonalInBeats = Integer.parseInt(nextTokens[i++]);
//			this.eventsPerBeat = Integer.parseInt(nextTokens[i++]);
			this.haveSameMeasureOffset = Double.parseDouble(nextTokens[i++]);
			this.haveDifferentMeasureOffset = Double.parseDouble(nextTokens[i++]);
			this.measureOffsetDifference = Double.parseDouble(nextTokens[i++]);
//			this.haveSameMeasureLeadOffset = Double.parseDouble(nextTokens[i++]);
//			this.haveDifferentMeasureLeadOffset = Double.parseDouble(nextTokens[i++]);
//			this.measureOffsetLeadDifference = Double.parseDouble(nextTokens[i++]);
		}

		public abstract GeneralizedGlobalStructureAlignmentParameterization crossoverWith(GeneralizedGlobalStructureAlignmentParameterization p2);
		
		public void mutate() {
			if (rand.nextDouble() < MUTATION_RATE)
				this.gapOpenScore += (rand.nextBoolean()?1:-1) * rand.nextInt(MAX_MUTATION_STEP);
			if (rand.nextDouble() < MUTATION_RATE)
				this.gapExtendScore += (rand.nextBoolean()?1:-1) * rand.nextInt(MAX_MUTATION_STEP);
			if (rand.nextDouble() < MUTATION_RATE) {
				this.minThresholdForLocalMaxima *= rand.nextDouble() * 2;
				if (this.minThresholdForLocalMaxima < MIN_THRESHOLD) {
					this.minThresholdForLocalMaxima = MIN_THRESHOLD;
				}
			}
//			if (rand.nextDouble() < MUTATION_RATE) {
//				this.distanceFromDiagonalInBeats += (rand.nextBoolean()?1:-1) * rand.nextInt(MAX_MUTATION_STEP);
//				if (this.distanceFromDiagonalInBeats > MAX_DISTANCE_FROM_DIAGNOAL_IN_BEATS) {
//					this.distanceFromDiagonalInBeats = MAX_DISTANCE_FROM_DIAGNOAL_IN_BEATS;
//				} else if (this.distanceFromDiagonalInBeats < MIN_DISTANCE_FROM_DIAGNOAL_IN_BEATS) {
//					this.distanceFromDiagonalInBeats = MIN_DISTANCE_FROM_DIAGNOAL_IN_BEATS;
//				}
//			}
//			if (rand.nextDouble() < MUTATION_RATE)
//				this.eventsPerBeat = (int) Math.pow(2,rand.nextInt(2));	
			
			if (rand.nextDouble() < MUTATION_RATE)
				this.haveSameMeasureOffset += (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
			if (rand.nextDouble() < MUTATION_RATE)
				this.haveDifferentMeasureOffset += (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
			if (rand.nextDouble() < MUTATION_RATE)
				this.measureOffsetDifference += (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
//			if (rand.nextDouble() < MUTATION_RATE)
//				this.haveSameMeasureLeadOffset += (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
//			if (rand.nextDouble() < MUTATION_RATE)
//				this.haveDifferentMeasureLeadOffset += (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
//			if (rand.nextDouble() < MUTATION_RATE)
//				this.measureOffsetLeadDifference += (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
			
			mutateSubclassParameters();
		}
		
		protected abstract void mutateSubclassParameters();
		
		public abstract double scoreMatch(MusicXMLAlignmentEvent musicXML1AlignmentEvent, MusicXMLAlignmentEvent musicXML2AlignmentEvent);
		
		public String toString() {
			return 
					df2.format(gapOpenScore) + ", " + 
					df2.format(gapExtendScore) + ", " +
					df2.format(minThresholdForLocalMaxima) + ", " +
//					distanceFromDiagonalInBeats + ", " + 
//					eventsPerBeat + ", " + 
					df2.format(haveSameMeasureOffset) + ", " + 
					df2.format(haveDifferentMeasureOffset) + ", " + 
					df2.format(measureOffsetDifference); 
//					df2.format(haveSameMeasureLeadOffset) + ", " + 
//					df2.format(haveDifferentMeasureLeadOffset) + ", " + 
//					df2.format(measureOffsetLeadDifference);
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
		
		public double scoreOffset(MusicXMLAlignmentEvent musicXML1AlignmentEvent,
				MusicXMLAlignmentEvent musicXML2AlignmentEvent) {
			
			double matchScore = 0;
//			offset value
			double offsetDifference = Math.abs(musicXML1AlignmentEvent.beat - musicXML2AlignmentEvent.beat);
			if (offsetDifference == 0.0) {
				matchScore += haveSameMeasureOffset;
			} else {
				matchScore += haveDifferentMeasureOffset;
				matchScore += measureOffsetDifference * offsetDifference;
			}
			
//			double offsetDifference = Math.abs(musicXML1AlignmentEvent.beatsToMeasureEnd - musicXML2AlignmentEvent.beatsToMeasureEnd);
//			if (offsetDifference == 0.0) {
//				matchScore += haveSameMeasureLeadOffset;
//			} else {
//				matchScore += haveDifferentMeasureLeadOffset;
//				matchScore += measureOffsetLeadDifference * offsetDifference;
//			}
			
			return matchScore;
		}

	}

	public static class CombinedAlignmentParameterization extends GeneralizedGlobalStructureAlignmentParameterization {

		private GeneralizedGlobalStructureAlignmentParameterization[] parameterizations;
		
		private double harmonyWeight;
		private double pitchWeight;
		private double rhythmWeight;
		private double lyricWeight;
		
		public CombinedAlignmentParameterization() throws FileNotFoundException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
			harmonyWeight = rand.nextInt(7)-3;
			pitchWeight = rand.nextInt(7)-3;
			rhythmWeight = rand.nextInt(7)-3;
			lyricWeight = rand.nextInt(7)-3;
			
			parameterizations = new GeneralizedGlobalStructureAlignmentParameterization[]{
//					new HarmonicAlignmentParameterization(),
//					new PitchAlignmentParameterization(),
//					new RhythmAlignmentParameterization(),
//					new LyricAlignmentParameterization(),
					loadInitialPopulationFromFile("harmony", false).get(0).getSecond(),
					loadInitialPopulationFromFile("pitch", false).get(0).getSecond(),
					loadInitialPopulationFromFile("rhythm", false).get(0).getSecond(),
					loadInitialPopulationFromFile("lyric", false).get(0).getSecond(),
			};
			minThresholdForLocalMaxima = 0.0;
			for (GeneralizedGlobalStructureAlignmentParameterization generalizedGlobalStructureAlignmentParameterization : parameterizations) {
				minThresholdForLocalMaxima += generalizedGlobalStructureAlignmentParameterization.minThresholdForLocalMaxima;
				if (gapOpenScore > generalizedGlobalStructureAlignmentParameterization.gapOpenScore) {
					gapOpenScore = generalizedGlobalStructureAlignmentParameterization.gapOpenScore;
				}
				if (gapExtendScore > generalizedGlobalStructureAlignmentParameterization.gapExtendScore) {
					gapExtendScore = generalizedGlobalStructureAlignmentParameterization.gapExtendScore;
				}
			}
		}

		public CombinedAlignmentParameterization(String[] nextTokens) {
			super(nextTokens);
			
			int i = NUM_PARAMS_IN_SUPER;
			harmonyWeight = Double.parseDouble(nextTokens[i++]);
			pitchWeight = Double.parseDouble(nextTokens[i++]);
			rhythmWeight = Double.parseDouble(nextTokens[i++]);
			lyricWeight = Double.parseDouble(nextTokens[i++]);
			
			parameterizations = new GeneralizedGlobalStructureAlignmentParameterization[]{
					new HarmonicAlignmentParameterization(Arrays.copyOfRange(nextTokens, i, i+=(6+NUM_PARAMS_IN_SUPER))),
					new PitchAlignmentParameterization(Arrays.copyOfRange(nextTokens, i, i+=(9+NUM_PARAMS_IN_SUPER))),
					new RhythmAlignmentParameterization(Arrays.copyOfRange(nextTokens, i, i+=(9+NUM_PARAMS_IN_SUPER))),
					new LyricAlignmentParameterization(Arrays.copyOfRange(nextTokens, i, i+=(9+NUM_PARAMS_IN_SUPER)))
			};
		}
		
		@Override
		public String toString() {
			StringBuilder str = new StringBuilder();
			str.append(super.toString() + ", " +
					df2.format(harmonyWeight) + ", " + 
					df2.format(pitchWeight) + ", " +
					df2.format(rhythmWeight) + ", " + 
					df2.format(lyricWeight));
			
			for (GeneralizedGlobalStructureAlignmentParameterization parameterization : parameterizations) {
				str.append(", ");
				str.append(parameterization.toString());
			}
			
			return str.toString();
		}
		
		public CombinedAlignmentParameterization(GeneralizedGlobalStructureAlignmentParameterization p1g, GeneralizedGlobalStructureAlignmentParameterization p2g) {
			super(p1g, p2g);
			
			CombinedAlignmentParameterization p1 = (CombinedAlignmentParameterization) p1g;
			CombinedAlignmentParameterization p2 = (CombinedAlignmentParameterization) p2g;
			
			this.harmonyWeight = (rand.nextBoolean() ?p1.harmonyWeight:p2.harmonyWeight);
			this.pitchWeight = (rand.nextBoolean() ?p1.pitchWeight:p2.pitchWeight);
			this.rhythmWeight = (rand.nextBoolean() ?p1.rhythmWeight:p2.rhythmWeight);
			this.lyricWeight = (rand.nextBoolean()?p1.lyricWeight:p2.lyricWeight);
			
			parameterizations = new GeneralizedGlobalStructureAlignmentParameterization[]{
					new HarmonicAlignmentParameterization(p1.parameterizations[0],p2.parameterizations[0]),
					new PitchAlignmentParameterization(p1.parameterizations[1],p2.parameterizations[1]),
					new RhythmAlignmentParameterization(p1.parameterizations[2],p2.parameterizations[2]),
					new LyricAlignmentParameterization(p1.parameterizations[3],p2.parameterizations[3])
			};
		}

		@Override
		protected void mutateSubclassParameters() {
			if (rand.nextDouble() < MUTATION_RATE)
				this.harmonyWeight += (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
			if (rand.nextDouble() < MUTATION_RATE)
				this.pitchWeight += (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2);  
			if (rand.nextDouble() < MUTATION_RATE)
				this.rhythmWeight += (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2);  
			if (rand.nextDouble() < MUTATION_RATE)
				this.lyricWeight += (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2);
			
			for (GeneralizedGlobalStructureAlignmentParameterization parameterization : parameterizations) {
				if (rand.nextDouble() < MUTATION_RATE)
					parameterization.mutate();
			}
		}

		@Override
		public GeneralizedGlobalStructureAlignmentParameterization crossoverWith(
				GeneralizedGlobalStructureAlignmentParameterization p2) {
			return new CombinedAlignmentParameterization(this, (CombinedAlignmentParameterization) p2);
		}

		@Override
		public double scoreMatch(MusicXMLAlignmentEvent musicXML1AlignmentEvent,
				MusicXMLAlignmentEvent musicXML2AlignmentEvent) {
			
			double score = 0.0;
			
			score += harmonyWeight * parameterizations[0].scoreMatch(musicXML1AlignmentEvent, musicXML2AlignmentEvent);
			score += pitchWeight * parameterizations[1].scoreMatch(musicXML1AlignmentEvent, musicXML2AlignmentEvent);
			score += rhythmWeight * parameterizations[2].scoreMatch(musicXML1AlignmentEvent, musicXML2AlignmentEvent);
			score += lyricWeight * parameterizations[3].scoreMatch(musicXML1AlignmentEvent, musicXML2AlignmentEvent);
			
			score += super.scoreOffset(musicXML1AlignmentEvent, musicXML2AlignmentEvent);
			
			return score;
		}

		
		public static void setType(String viewpoint) {
			if (viewpoint.equals("verse")) {
				LyricAlignmentParameterization.swapEqualsAndUnequals();
			}
		}
	}

	public static class HarmonicAlignmentParameterization extends GeneralizedGlobalStructureAlignmentParameterization {

		public double harmonyEqual;
		public double harmonyUnequal;
		public double harmonyDifference;
		public double bothHarmonyOnset;
		public double oneHarmonyOnsetOneNot;
		public double bothHarmonyNotOnset;

		public HarmonicAlignmentParameterization() {
			harmonyEqual = rand.nextInt(7)-3;
			harmonyUnequal = rand.nextInt(7)-3;
			harmonyDifference = rand.nextInt(7)-3;
			bothHarmonyOnset = rand.nextInt(7)-3;
			oneHarmonyOnsetOneNot = rand.nextInt(7)-3;
			bothHarmonyNotOnset = rand.nextInt(7)-3;
		}

		public HarmonicAlignmentParameterization(String[] nextTokens) {
			super(nextTokens);
			
			int i = NUM_PARAMS_IN_SUPER;
			this.harmonyEqual = Double.parseDouble(nextTokens[i++]);
			this.harmonyUnequal = Double.parseDouble(nextTokens[i++]);
			this.harmonyDifference = Double.parseDouble(nextTokens[i++]);
			this.bothHarmonyOnset = Double.parseDouble(nextTokens[i++]);
			this.oneHarmonyOnsetOneNot = Double.parseDouble(nextTokens[i++]);
			this.bothHarmonyNotOnset = Double.parseDouble(nextTokens[i++]);
		}

		@Override
		public String toString() {
			return 
					super.toString() + ", " +
					df2.format(harmonyEqual) + ", " + 
					df2.format(harmonyUnequal) + ", " + 
					df2.format(harmonyDifference) + ", " + 
					df2.format(bothHarmonyOnset) + ", " + 
					df2.format(oneHarmonyOnsetOneNot) + ", " +
					df2.format(bothHarmonyNotOnset);
		}
		
		public HarmonicAlignmentParameterization(GeneralizedGlobalStructureAlignmentParameterization p1g, GeneralizedGlobalStructureAlignmentParameterization p2g) {
			super(p1g, p2g);
			
			HarmonicAlignmentParameterization p1 = (HarmonicAlignmentParameterization) p1g;
			HarmonicAlignmentParameterization p2 = (HarmonicAlignmentParameterization) p2g;
			
			this.harmonyEqual = (rand.nextBoolean()?p1.harmonyEqual:p2.harmonyEqual);
			this.harmonyUnequal = (rand.nextBoolean()?p1.harmonyUnequal:p2.harmonyUnequal);
			this.harmonyDifference = (rand.nextBoolean()?p1.harmonyDifference:p2.harmonyDifference);
			this.bothHarmonyOnset = (rand.nextBoolean()?p1.bothHarmonyOnset:p2.bothHarmonyOnset);
			this.oneHarmonyOnsetOneNot = (rand.nextBoolean()?p1.oneHarmonyOnsetOneNot:p2.oneHarmonyOnsetOneNot);
			this.bothHarmonyNotOnset = (rand.nextBoolean()?p1.bothHarmonyNotOnset:p2.bothHarmonyNotOnset);
		}

		public void mutateSubclassParameters() {
			if (rand.nextDouble() < MUTATION_RATE)
				this.harmonyEqual += (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
			if (rand.nextDouble() < MUTATION_RATE)
				this.harmonyUnequal += (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
			if (rand.nextDouble() < MUTATION_RATE)
				this.harmonyDifference += (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2);
			if (rand.nextDouble() < MUTATION_RATE)
				this.bothHarmonyOnset += (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
			if (rand.nextDouble() < MUTATION_RATE)
				this.oneHarmonyOnsetOneNot += (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
			if (rand.nextDouble() < MUTATION_RATE)
				this.bothHarmonyNotOnset += (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
		}

		@Override
		public GeneralizedGlobalStructureAlignmentParameterization crossoverWith(
				GeneralizedGlobalStructureAlignmentParameterization p2) {
			return new HarmonicAlignmentParameterization(this, (HarmonicAlignmentParameterization) p2);
		}
		
		@Override
		public double scoreMatch(MusicXMLAlignmentEvent musicXML1AlignmentEvent,
				MusicXMLAlignmentEvent musicXML2AlignmentEvent) {
			
			double score = 0.0;
			
			Harmony mXML1Harmony = musicXML1AlignmentEvent.harmony;
			Harmony mXML2Harmony = musicXML2AlignmentEvent.harmony;
			
			double similarity;
			if (mXML1Harmony == null) {
				if (mXML2Harmony == null) {
					similarity = 1.0;
				} else {
					similarity = mXML2Harmony.fractionOfSimilarToTotalNotes(mXML1Harmony);
				}
			} else {
				similarity = mXML1Harmony.fractionOfSimilarToTotalNotes(mXML2Harmony);
			}

			if (similarity == 1.0) {
				score += harmonyEqual;
			} else {
				score += harmonyUnequal;
				score += harmonyDifference * 1.0/similarity;
			}
			
//			//onset value
			if (musicXML1AlignmentEvent.harmonyOnset) {
				if (musicXML2AlignmentEvent.harmonyOnset)
					score += bothHarmonyOnset;
				else 
					score += oneHarmonyOnsetOneNot;
			} else {
				if (musicXML2AlignmentEvent.harmonyOnset)
					score += oneHarmonyOnsetOneNot;
				else 
					score += bothHarmonyNotOnset;
			}
			
			score += super.scoreOffset(musicXML1AlignmentEvent, musicXML2AlignmentEvent);
					
			return score;
		}
	}
	
	public static class PitchAlignmentParameterization extends GeneralizedGlobalStructureAlignmentParameterization {

		// pitch weights
		public double bothRests;
		public double oneRest;
		public double neitherRests;

		public double pitchesEqual;
		public double pitchesUnequal;
		public double pitchDifference;
		public double bothPitchesOnset;
		public double onePitchOnsetOneNot;
		public double bothPitchesNotOnset;

		public PitchAlignmentParameterization() {
			bothRests = rand.nextInt(7)-3;
			oneRest = rand.nextInt(7)-3;
			neitherRests = rand.nextInt(7)-3;
			pitchesEqual = rand.nextInt(7)-3;
			pitchesUnequal = rand.nextInt(7)-3;
			pitchDifference = rand.nextInt(7)-3;
			bothPitchesOnset = rand.nextInt(7)-3;
			onePitchOnsetOneNot = rand.nextInt(7)-3;
			bothPitchesNotOnset = rand.nextInt(7)-3;
		}

		public PitchAlignmentParameterization(String[] nextTokens) {
			super(nextTokens);
			
			int i = NUM_PARAMS_IN_SUPER;
			this.bothRests = Double.parseDouble(nextTokens[i++]);
			this.oneRest = Double.parseDouble(nextTokens[i++]);
			this.neitherRests = Double.parseDouble(nextTokens[i++]);
			this.pitchesEqual = Double.parseDouble(nextTokens[i++]);
			this.pitchesUnequal = Double.parseDouble(nextTokens[i++]);
			this.pitchDifference = Double.parseDouble(nextTokens[i++]);
			this.bothPitchesOnset = Double.parseDouble(nextTokens[i++]);
			this.onePitchOnsetOneNot = Double.parseDouble(nextTokens[i++]);
			this.bothPitchesNotOnset = Double.parseDouble(nextTokens[i++]);
		}

		@Override
		public String toString() {
			return 
					super.toString() + ", " +
					df2.format(bothRests) + ", " + 
					df2.format(oneRest) + ", " +
					df2.format(neitherRests) + ", " + 
					df2.format(pitchesEqual) + ", " + 
					df2.format(pitchesUnequal) + ", " + 
					df2.format(pitchDifference) + ", " + 
					df2.format(bothPitchesOnset) + ", " + 
					df2.format(onePitchOnsetOneNot) + ", " +
					df2.format(bothPitchesNotOnset);
		}
		
		public PitchAlignmentParameterization(GeneralizedGlobalStructureAlignmentParameterization p1g, GeneralizedGlobalStructureAlignmentParameterization p2g) {
			super(p1g, p2g);
			
			PitchAlignmentParameterization p1 = (PitchAlignmentParameterization) p1g;
			PitchAlignmentParameterization p2 = (PitchAlignmentParameterization) p2g;
			
			this.bothRests = (rand.nextBoolean() ?p1.bothRests:p2.bothRests);
			this.oneRest = (rand.nextBoolean() ?p1.oneRest:p2.oneRest);
			this.neitherRests = (rand.nextBoolean() ?p1.neitherRests:p2.neitherRests);
			this.pitchesEqual = (rand.nextBoolean()?p1.pitchesEqual:p2.pitchesEqual);
			this.pitchesUnequal = (rand.nextBoolean()?p1.pitchesUnequal:p2.pitchesUnequal);
			this.pitchDifference = (rand.nextBoolean()?p1.pitchDifference:p2.pitchDifference);
			this.bothPitchesOnset = (rand.nextBoolean()?p1.bothPitchesOnset:p2.bothPitchesOnset);
			this.onePitchOnsetOneNot = (rand.nextBoolean()?p1.onePitchOnsetOneNot:p2.onePitchOnsetOneNot);
			this.bothPitchesNotOnset = (rand.nextBoolean()?p1.bothPitchesNotOnset:p2.bothPitchesNotOnset);
		}

		public void mutateSubclassParameters() {
			if (rand.nextDouble() < MUTATION_RATE)
				this.bothRests += (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
			if (rand.nextDouble() < MUTATION_RATE)
				this.oneRest += (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2);  
			if (rand.nextDouble() < MUTATION_RATE)
				this.neitherRests += (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2);  
			if (rand.nextDouble() < MUTATION_RATE)
				this.pitchesEqual += (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2);  
			if (rand.nextDouble() < MUTATION_RATE)
				this.pitchesUnequal += (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2);  
			if (rand.nextDouble() < MUTATION_RATE)
				this.pitchDifference += (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
			if (rand.nextDouble() < MUTATION_RATE)
				this.bothPitchesOnset += (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2);  
			if (rand.nextDouble() < MUTATION_RATE)
				this.onePitchOnsetOneNot += (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2);  
			if (rand.nextDouble() < MUTATION_RATE)
				this.bothPitchesNotOnset += (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2);  
		}

		@Override
		public GeneralizedGlobalStructureAlignmentParameterization crossoverWith(
				GeneralizedGlobalStructureAlignmentParameterization p2) {
			return new PitchAlignmentParameterization(this, (PitchAlignmentParameterization) p2);
		}
		
		@Override
		public double scoreMatch(MusicXMLAlignmentEvent musicXML1AlignmentEvent,
				MusicXMLAlignmentEvent musicXML2AlignmentEvent) {
			
			double score = 0.0;
			
			int mXML1Pitch = musicXML1AlignmentEvent.note.pitch;
			int mXML2Pitch = musicXML2AlignmentEvent.note.pitch;
			
			if (mXML1Pitch == Note.REST) {
				if (mXML2Pitch == Note.REST) {
					score += bothRests;
				} else {
					score += oneRest;
				}
			} else {
				if (mXML2Pitch == Note.REST) {
					score += oneRest;
				} else {
					score += neitherRests;
					int diff = Math.abs(mXML2Pitch - mXML1Pitch);
					if (diff == 0) {
						score += pitchesEqual;
					} else {
						score += pitchesUnequal;
						score += pitchDifference * diff;
					}
					
					//onset value
					if (musicXML1AlignmentEvent.noteOnset) {
						if (musicXML2AlignmentEvent.noteOnset)
							score += bothPitchesOnset;
						else 
							score += onePitchOnsetOneNot;
					} else {
						if (musicXML2AlignmentEvent.noteOnset)
							score += onePitchOnsetOneNot;
						else 
							score += bothPitchesNotOnset;
					}
					
					score += super.scoreOffset(musicXML1AlignmentEvent, musicXML2AlignmentEvent);
				}
			}
			return score;
		}
	}
	
	public static class RhythmAlignmentParameterization extends GeneralizedGlobalStructureAlignmentParameterization {
		
		public double bothNotesRest;
		public double oneNoteRestOneNot;
		public double bothNotesNotRest;

		public double noteDurationEqual;
		public double noteDurationUnequal;
		public double noteDurationDifference;
		
		public double bothNotesOnset;
		public double oneNoteOnsetOneNot;
		public double bothNotesNotOnset;


		public RhythmAlignmentParameterization() {
			bothNotesRest = rand.nextInt(7)-3;
			oneNoteRestOneNot = rand.nextInt(7)-3;
			bothNotesNotRest = rand.nextInt(7)-3;
			noteDurationEqual = rand.nextInt(7)-3;
			noteDurationUnequal = rand.nextInt(7)-3;
			noteDurationDifference = rand.nextInt(7)-3;
			bothNotesOnset = rand.nextInt(7)-3;
			oneNoteOnsetOneNot = rand.nextInt(7)-3;
			bothNotesNotOnset = rand.nextInt(7)-3;
		}

		public RhythmAlignmentParameterization(String[] nextTokens) {
			super(nextTokens);
			
			int i = NUM_PARAMS_IN_SUPER;
			this.bothNotesRest = Double.parseDouble(nextTokens[i++]);
			this.oneNoteRestOneNot = Double.parseDouble(nextTokens[i++]);
			this.bothNotesNotRest = Double.parseDouble(nextTokens[i++]);
			this.noteDurationEqual = Double.parseDouble(nextTokens[i++]);
			this.noteDurationUnequal = Double.parseDouble(nextTokens[i++]);
			this.noteDurationDifference = Double.parseDouble(nextTokens[i++]);
			this.bothNotesOnset = Double.parseDouble(nextTokens[i++]);
			this.oneNoteOnsetOneNot = Double.parseDouble(nextTokens[i++]);
			this.bothNotesNotOnset = Double.parseDouble(nextTokens[i++]);
		}

		@Override
		public String toString() {
			return 
					super.toString() + ", " +
					df2.format(bothNotesRest) + ", " + 
					df2.format(oneNoteRestOneNot) + ", " +
					df2.format(bothNotesNotRest) + ", " + 
					df2.format(noteDurationEqual) + ", " + 
					df2.format(noteDurationUnequal) + ", " + 
					df2.format(noteDurationDifference) + ", " + 
					df2.format(bothNotesOnset) + ", " + 
					df2.format(oneNoteOnsetOneNot) + ", " + 
					df2.format(bothNotesNotOnset); 
		}
		
		public RhythmAlignmentParameterization(GeneralizedGlobalStructureAlignmentParameterization p1g, GeneralizedGlobalStructureAlignmentParameterization p2g) {
			super(p1g, p2g);
			
			RhythmAlignmentParameterization p1 = (RhythmAlignmentParameterization) p1g;
			RhythmAlignmentParameterization p2 = (RhythmAlignmentParameterization) p2g;
			
			this.oneNoteRestOneNot = (rand.nextBoolean()?p1.oneNoteRestOneNot:p2.oneNoteRestOneNot);
			this.bothNotesRest = (rand.nextBoolean()?p1.bothNotesRest:p2.bothNotesRest);
			this.bothNotesNotRest = (rand.nextBoolean()?p1.bothNotesNotRest:p2.bothNotesNotRest);
			this.noteDurationEqual = (rand.nextBoolean()?p1.noteDurationEqual:p2.noteDurationEqual);
			this.noteDurationUnequal = (rand.nextBoolean()?p1.noteDurationUnequal:p2.noteDurationUnequal);
			this.noteDurationDifference = (rand.nextBoolean()?p1.noteDurationDifference:p2.noteDurationDifference);
			this.bothNotesOnset = (rand.nextBoolean()?p1.bothNotesOnset:p2.bothNotesOnset);
			this.oneNoteOnsetOneNot = (rand.nextBoolean()?p1.oneNoteOnsetOneNot:p2.oneNoteOnsetOneNot);
			this.bothNotesNotOnset = (rand.nextBoolean()?p1.bothNotesNotOnset:p2.bothNotesNotOnset);
		}

		public void mutateSubclassParameters() {
			if (rand.nextDouble() < MUTATION_RATE)
				this.bothNotesRest += (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
			if (rand.nextDouble() < MUTATION_RATE)
				this.oneNoteRestOneNot += (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
			if (rand.nextDouble() < MUTATION_RATE)
				this.bothNotesNotRest += (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
			if (rand.nextDouble() < MUTATION_RATE)
				this.noteDurationEqual += (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
			if (rand.nextDouble() < MUTATION_RATE)
				this.noteDurationUnequal += (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
			if (rand.nextDouble() < MUTATION_RATE)
				this.noteDurationDifference += (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2);
			if (rand.nextDouble() < MUTATION_RATE)
				this.bothNotesOnset += (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
			if (rand.nextDouble() < MUTATION_RATE)
				this.oneNoteOnsetOneNot += (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
			if (rand.nextDouble() < MUTATION_RATE)
				this.bothNotesNotOnset += (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
		}

		@Override
		public GeneralizedGlobalStructureAlignmentParameterization crossoverWith(
				GeneralizedGlobalStructureAlignmentParameterization p2) {
			return new RhythmAlignmentParameterization(this, (RhythmAlignmentParameterization) p2);
		}
		
		@Override
		public double scoreMatch(MusicXMLAlignmentEvent musicXML1AlignmentEvent,
				MusicXMLAlignmentEvent musicXML2AlignmentEvent) {
			
			double score = 0.0;

			boolean mXML1Resting = musicXML1AlignmentEvent.note.pitch == Note.REST;
			boolean mXML2Resting = musicXML2AlignmentEvent.note.pitch == Note.REST;

			double mXML1BeatsSinceOnset = musicXML1AlignmentEvent.currBeatsSinceOnset;
			double mXML2BeatsSinceOnset = musicXML2AlignmentEvent.currBeatsSinceOnset;
			double onsetDiff = Math.abs(mXML2BeatsSinceOnset - mXML1BeatsSinceOnset);

			if (onsetDiff == 0.0) {
				score += noteDurationEqual;
			} else {
				score += noteDurationUnequal;
				score += onsetDiff * noteDurationDifference;
			}
			
			//onset value
			if (musicXML1AlignmentEvent.noteOnset) {
				if (musicXML2AlignmentEvent.noteOnset)
					score += bothNotesOnset;
				else 
					score += oneNoteOnsetOneNot;
			} else {
				if (musicXML2AlignmentEvent.noteOnset)
					score += oneNoteOnsetOneNot;
				else 
					score += bothNotesNotOnset;
			}
			
			score += super.scoreOffset(musicXML1AlignmentEvent, musicXML2AlignmentEvent);

			// if notes are both rests
			if (mXML1Resting) {
				if (mXML2Resting) {
					score *= bothNotesRest;
				} else {
					score *= oneNoteRestOneNot;
				}
			} else {
				if (mXML2Resting) {
					score *= oneNoteRestOneNot;
				} else {
					score *= bothNotesNotRest;
				}
			}
					
			return score;
		}
	}
	
	public static class LyricAlignmentParameterization extends GeneralizedGlobalStructureAlignmentParameterization {

		private static boolean swapEqualsAndUnequals = false;
		// pitch weights
		public double bothRests;
		public double oneRest;

		// lyric weights
		public double bothLyricsNull;
		public double oneLyricNull;
		public double lyricsEqual;
		public double lyricsUnequal;
		public double bothLyricsOnset;
		public double oneLyricOnsetOneNot;
		public double bothLyricsNotOnset;

		public LyricAlignmentParameterization() {
			bothRests = rand.nextInt(7)-3;
			oneRest = rand.nextInt(7)-3;
			bothLyricsNull = rand.nextInt(7)-3;
			oneLyricNull = rand.nextInt(7)-3;
			lyricsEqual = rand.nextInt(7)-3;
			lyricsUnequal = rand.nextInt(7)-3;
			bothLyricsOnset = rand.nextInt(7)-3;
			oneLyricOnsetOneNot = rand.nextInt(7)-3;
			bothLyricsNotOnset = rand.nextInt(7)-3;
		}

		public static void swapEqualsAndUnequals() {
			swapEqualsAndUnequals = true;
		}

		public LyricAlignmentParameterization(String[] nextTokens) {
			super(nextTokens);
			
			int i = NUM_PARAMS_IN_SUPER;
			this.bothRests = Double.parseDouble(nextTokens[i++]);
			this.oneRest = Double.parseDouble(nextTokens[i++]);
			this.bothLyricsNull = Double.parseDouble(nextTokens[i++]);
			this.oneLyricNull = Double.parseDouble(nextTokens[i++]);
			if (swapEqualsAndUnequals)
				this.lyricsUnequal = Double.parseDouble(nextTokens[i++]);
			this.lyricsEqual = Double.parseDouble(nextTokens[i++]);
			if (!swapEqualsAndUnequals)
				this.lyricsUnequal = Double.parseDouble(nextTokens[i++]);
			this.bothLyricsOnset = Double.parseDouble(nextTokens[i++]);
			this.oneLyricOnsetOneNot = Double.parseDouble(nextTokens[i++]);
			this.bothLyricsNotOnset = Double.parseDouble(nextTokens[i++]);
		}

		@Override
		public String toString() {
			return 
					super.toString() + ", " +
					df2.format(bothRests) + ", " + 
					df2.format(oneRest) + ", " +
					df2.format(bothLyricsNull) + ", " + 
					df2.format(oneLyricNull) + ", " + 
					df2.format(lyricsEqual) + ", " + 
					df2.format(lyricsUnequal) + ", " + 
					df2.format(bothLyricsOnset) + ", " + 
					df2.format(oneLyricOnsetOneNot) + ", " +
					df2.format(bothLyricsNotOnset);
		}
		
		public LyricAlignmentParameterization(GeneralizedGlobalStructureAlignmentParameterization p1g, GeneralizedGlobalStructureAlignmentParameterization p2g) {
			super(p1g, p2g);
			
			LyricAlignmentParameterization p1 = (LyricAlignmentParameterization) p1g;
			LyricAlignmentParameterization p2 = (LyricAlignmentParameterization) p2g;
			
			this.bothRests = (rand.nextBoolean()?p1.bothRests:p2.bothRests);
			this.oneRest = (rand.nextBoolean()?p1.oneRest:p2.oneRest);
			this.bothLyricsNull = (rand.nextBoolean()?p1.bothLyricsNull:p2.bothLyricsNull);
			this.oneLyricNull = (rand.nextBoolean()?p1.oneLyricNull:p2.oneLyricNull);
			this.lyricsEqual = (rand.nextBoolean()?p1.lyricsEqual:p2.lyricsEqual);
			this.lyricsUnequal = (rand.nextBoolean()?p1.lyricsUnequal:p2.lyricsUnequal);
			this.bothLyricsOnset = (rand.nextBoolean()?p1.bothLyricsOnset:p2.bothLyricsOnset);
			this.oneLyricOnsetOneNot = (rand.nextBoolean()?p1.oneLyricOnsetOneNot:p2.oneLyricOnsetOneNot);
			this.bothLyricsNotOnset = (rand.nextBoolean()?p1.bothLyricsNotOnset:p2.bothLyricsNotOnset);
		}

		public void mutateSubclassParameters() {
			if (rand.nextDouble() < MUTATION_RATE)
				this.bothRests += (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
			if (rand.nextDouble() < MUTATION_RATE)
				this.oneRest += (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
			if (rand.nextDouble() < MUTATION_RATE)
				this.bothLyricsNull += (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2);
			if (rand.nextDouble() < MUTATION_RATE)
				this.oneLyricNull += (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2);
			if (rand.nextDouble() < MUTATION_RATE)
				this.lyricsEqual += (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
			if (rand.nextDouble() < MUTATION_RATE)
				this.lyricsUnequal += (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
			if (rand.nextDouble() < MUTATION_RATE)
				this.bothLyricsOnset += (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
			if (rand.nextDouble() < MUTATION_RATE)
				this.oneLyricOnsetOneNot += (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
			if (rand.nextDouble() < MUTATION_RATE)
				this.bothLyricsNotOnset += (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
		}

		@Override
		public GeneralizedGlobalStructureAlignmentParameterization crossoverWith(
				GeneralizedGlobalStructureAlignmentParameterization p2) {
			return new LyricAlignmentParameterization(this, (LyricAlignmentParameterization) p2);
		}
		
		@Override
		public double scoreMatch(MusicXMLAlignmentEvent musicXML1AlignmentEvent,
				MusicXMLAlignmentEvent musicXML2AlignmentEvent) {
			String mXML1Lyric = musicXML1AlignmentEvent.strippedLyricLowerCaseText;
			String mXML2Lyric = musicXML2AlignmentEvent.strippedLyricLowerCaseText;
			
			if (musicXML1AlignmentEvent.note.pitch == Note.REST) {
				if (musicXML2AlignmentEvent.note.pitch == Note.REST) {
					// both rests
					return bothRests;
				} else {
					// one rest
					return oneRest;
				}
			} else if (musicXML2AlignmentEvent.note.pitch == Note.REST) {
				// one rest
				return oneRest;
			} else {
				// neither are rests
				if (mXML1Lyric.isEmpty()) {
					if (mXML2Lyric.isEmpty()) {
						// neither have lyrics, but notes are on
						return bothLyricsNull;
					} else {
						// one has lyrics, both notes are on
						return oneLyricNull;
					}
				} else {
					if (mXML2Lyric.isEmpty()) {
						// one has lyrics, both notes are on
						return oneLyricNull;
					} else {
						// both have lyrics
						double matchScore = 0.0;
						
						//equality of lyrics
						if (mXML1Lyric.equals(mXML2Lyric)) {
							matchScore += lyricsEqual;
						} else {
							matchScore += lyricsUnequal;
						}
						
						//onset value
						if (musicXML1AlignmentEvent.lyricOnset) {
							if (musicXML2AlignmentEvent.lyricOnset)
								matchScore += bothLyricsOnset;
							else 
								matchScore += oneLyricOnsetOneNot;
						} else {
							if (musicXML2AlignmentEvent.lyricOnset)
								matchScore += oneLyricOnsetOneNot;
							else 
								matchScore += bothLyricsNotOnset;
						}
						
						matchScore += super.scoreOffset(musicXML1AlignmentEvent,musicXML2AlignmentEvent);
						
						return matchScore;
					}
				}
			}
		}
	}
	
	public static class RhymeAlignmentParameterization extends GeneralizedGlobalStructureAlignmentParameterization {
		
		// hirjee vowel alignment score
		// Pat's rules score
		// Pat's rules w/Hirjee score
		// measure position
		// distance (in measures) between events
		// proximity (in syllable count) of (later) token to phrase/line-end punctuation
		//
		
		public double noMatch;
		public double identicalVowel;
		public double differentVowel;
		
		public double hirjeeVowelMatchScore;
		public double patAlnScore;
		
		public double sameStress;

		public double bothLyricOnset;
		public double neitherLyricOnset;
		public double oneLyricOnsetOneNot;

		public double minMeasureDistance;
		public double maxMeasureDistance;

		public double earlierHasEndPunctuation;
		public double earlierDistanceFromPunctuation;
		public double laterHasEndPunctuation;
		public double laterDistanceFromPunctuation;
		
		public RhymeAlignmentParameterization() {
			noMatch = rand.nextInt(7)-3;
			identicalVowel = rand.nextInt(7)-3;
			differentVowel = rand.nextInt(7)-3;
			hirjeeVowelMatchScore = rand.nextInt(7)-3;
			patAlnScore = rand.nextInt(7)-3;
			sameStress = rand.nextInt(7)-3;
			bothLyricOnset = rand.nextInt(7)-3;
			neitherLyricOnset = rand.nextInt(7)-3;
			oneLyricOnsetOneNot = rand.nextInt(7)-3;
			minMeasureDistance = rand.nextInt(7);
			maxMeasureDistance = minMeasureDistance + rand.nextInt(7);
			earlierHasEndPunctuation = rand.nextInt(7)-3;
			earlierDistanceFromPunctuation = rand.nextInt(7)-3;
			laterHasEndPunctuation = rand.nextInt(7)-3;
			laterDistanceFromPunctuation = rand.nextInt(7)-3;
		}

		public RhymeAlignmentParameterization(String[] nextTokens) {
			super(nextTokens);
			
			int i = NUM_PARAMS_IN_SUPER;
			this.noMatch = Double.parseDouble(nextTokens[i++]);
			this.identicalVowel = Double.parseDouble(nextTokens[i++]);
			this.differentVowel = Double.parseDouble(nextTokens[i++]);
			this.hirjeeVowelMatchScore = Double.parseDouble(nextTokens[i++]);
			this.patAlnScore = Double.parseDouble(nextTokens[i++]);
			this.sameStress = Double.parseDouble(nextTokens[i++]);
			this.bothLyricOnset = Double.parseDouble(nextTokens[i++]);
			this.neitherLyricOnset = Double.parseDouble(nextTokens[i++]);
			this.oneLyricOnsetOneNot = Double.parseDouble(nextTokens[i++]);
			this.minMeasureDistance = Double.parseDouble(nextTokens[i++]);
			this.maxMeasureDistance = Double.parseDouble(nextTokens[i++]);
			this.earlierHasEndPunctuation = Double.parseDouble(nextTokens[i++]);
			this.earlierDistanceFromPunctuation = Double.parseDouble(nextTokens[i++]);
			this.laterHasEndPunctuation = Double.parseDouble(nextTokens[i++]);
			this.laterDistanceFromPunctuation = Double.parseDouble(nextTokens[i++]);
		}

		@Override
		public String toString() {
			return 
					super.toString() + ", " +
					df2.format(noMatch) + ", " + 
					df2.format(identicalVowel) + ", " + 
					df2.format(differentVowel) + ", " + 
					df2.format(hirjeeVowelMatchScore) + ", " + 
					df2.format(patAlnScore) + ", " + 
					df2.format(sameStress) + ", " + 
					df2.format(bothLyricOnset) + ", " + 
					df2.format(neitherLyricOnset) + ", " + 
					df2.format(oneLyricOnsetOneNot) + ", " + 
					df2.format(minMeasureDistance) + ", " + 
					df2.format(maxMeasureDistance) + ", " + 
					df2.format(earlierHasEndPunctuation) + ", " + 
					df2.format(earlierDistanceFromPunctuation) + ", " + 
					df2.format(laterHasEndPunctuation) + ", " + 
					df2.format(laterDistanceFromPunctuation); 
		}
		
		public RhymeAlignmentParameterization(GeneralizedGlobalStructureAlignmentParameterization p1g, GeneralizedGlobalStructureAlignmentParameterization p2g) {
			super(p1g, p2g);
			
			RhymeAlignmentParameterization p1 = (RhymeAlignmentParameterization) p1g;
			RhymeAlignmentParameterization p2 = (RhymeAlignmentParameterization) p2g;
			
			this.noMatch = (rand.nextBoolean()?p1.noMatch:p2.noMatch);
			this.identicalVowel = (rand.nextBoolean()?p1.identicalVowel:p2.identicalVowel);
			this.differentVowel = (rand.nextBoolean()?p1.differentVowel:p2.differentVowel);
			this.hirjeeVowelMatchScore = (rand.nextBoolean()?p1.hirjeeVowelMatchScore:p2.hirjeeVowelMatchScore);
			this.patAlnScore = (rand.nextBoolean()?p1.patAlnScore:p2.patAlnScore);
			this.sameStress = (rand.nextBoolean()?p1.sameStress:p2.sameStress);
			this.bothLyricOnset = (rand.nextBoolean()?p1.bothLyricOnset:p2.bothLyricOnset);
			this.neitherLyricOnset = (rand.nextBoolean()?p1.neitherLyricOnset:p2.neitherLyricOnset);
			this.oneLyricOnsetOneNot = (rand.nextBoolean()?p1.oneLyricOnsetOneNot:p2.oneLyricOnsetOneNot);
			if (rand.nextBoolean()) {
				this.minMeasureDistance = p1.minMeasureDistance;
				this.maxMeasureDistance = p1.maxMeasureDistance;
			} else {
				this.minMeasureDistance = p2.minMeasureDistance;
				this.maxMeasureDistance = p2.maxMeasureDistance;
			}
			this.earlierHasEndPunctuation = (rand.nextBoolean()?p1.earlierHasEndPunctuation:p2.earlierHasEndPunctuation);
			this.earlierDistanceFromPunctuation = (rand.nextBoolean()?p1.earlierDistanceFromPunctuation:p2.earlierDistanceFromPunctuation);
			this.laterHasEndPunctuation = (rand.nextBoolean()?p1.laterHasEndPunctuation:p2.laterHasEndPunctuation);
			this.laterDistanceFromPunctuation = (rand.nextBoolean()?p1.laterDistanceFromPunctuation:p2.laterDistanceFromPunctuation);
		}

		public void mutateSubclassParameters() {
			if (rand.nextDouble() < MUTATION_RATE)
				this.noMatch += (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
			if (rand.nextDouble() < MUTATION_RATE)
				this.identicalVowel += (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
			if (rand.nextDouble() < MUTATION_RATE)
				this.differentVowel += (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2);
			if (rand.nextDouble() < MUTATION_RATE)
				this.hirjeeVowelMatchScore += (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
			if (rand.nextDouble() < MUTATION_RATE)
				this.patAlnScore += (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
			if (rand.nextDouble() < MUTATION_RATE)
				this.sameStress += (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
			if (rand.nextDouble() < MUTATION_RATE)
				this.bothLyricOnset += (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
			if (rand.nextDouble() < MUTATION_RATE)
				this.neitherLyricOnset += (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
			if (rand.nextDouble() < MUTATION_RATE)
				this.oneLyricOnsetOneNot += (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
			if (rand.nextDouble() < MUTATION_RATE) {
				this.minMeasureDistance += (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2);
				if (this.minMeasureDistance < 0) {
					this.minMeasureDistance = 0;
				}
			}
			if (rand.nextDouble() < MUTATION_RATE) {
				this.maxMeasureDistance += (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2);
				if (this.maxMeasureDistance < this.minMeasureDistance) {
					this.maxMeasureDistance = this.minMeasureDistance;
				}
			}
			if (rand.nextDouble() < MUTATION_RATE)
				this.earlierHasEndPunctuation += (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
			if (rand.nextDouble() < MUTATION_RATE)
				this.earlierDistanceFromPunctuation += (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
			if (rand.nextDouble() < MUTATION_RATE)
				this.laterHasEndPunctuation += (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
			if (rand.nextDouble() < MUTATION_RATE)
				this.laterDistanceFromPunctuation += (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
		}

		@Override
		public GeneralizedGlobalStructureAlignmentParameterization crossoverWith(
				GeneralizedGlobalStructureAlignmentParameterization p2) {
			return new RhymeAlignmentParameterization(this, (RhymeAlignmentParameterization) p2);
		}
		
		private static double[][] hMatrix = HirjeeMatrix.load();

		final private static Set<Character> endPunctuation = new HashSet<Character>();
		static {
			endPunctuation.add('!');
			endPunctuation.add('?');
			endPunctuation.add('.');
			endPunctuation.add(';');
			endPunctuation.add(',');
		}
		@Override
		public double scoreMatch(MusicXMLAlignmentEvent musicXML1AlignmentEvent,
				MusicXMLAlignmentEvent musicXML2AlignmentEvent) {
			
			if (musicXML1AlignmentEvent == musicXML2AlignmentEvent)
				return noMatch;

			NoteLyric mXML1NoteLyric = musicXML1AlignmentEvent.note.getLyric(musicXML1AlignmentEvent.segmentType.mustHaveDifferentLyricsOnRepeats());
			NoteLyric mXML2NoteLyric = musicXML2AlignmentEvent.note.getLyric(musicXML2AlignmentEvent.segmentType.mustHaveDifferentLyricsOnRepeats());
			
			if (mXML1NoteLyric == null || mXML2NoteLyric == null) {
				return noMatch;
			}
			
			int mXML1Measure = musicXML1AlignmentEvent.measure;
			int mXML2Measure = musicXML2AlignmentEvent.measure;
			
			double mXML1Beat = musicXML1AlignmentEvent.beat;
			double mXML2Beat = musicXML2AlignmentEvent.beat;

			if (mXML1Measure > mXML2Measure || mXML1Measure == mXML2Measure && mXML1Beat > mXML2Beat) { // let's make sure the 2nd event always comes after the first temporally
				MusicXMLAlignmentEvent tmp = musicXML1AlignmentEvent;
				musicXML1AlignmentEvent = musicXML2AlignmentEvent;
				musicXML2AlignmentEvent = tmp;
				
				mXML1Measure = musicXML1AlignmentEvent.measure;
				mXML2Measure = musicXML2AlignmentEvent.measure;
				
				mXML1Beat = musicXML1AlignmentEvent.beat;
				mXML2Beat = musicXML2AlignmentEvent.beat;
				
				NoteLyric tmp2 = mXML1NoteLyric;
				mXML1NoteLyric = mXML2NoteLyric;
				mXML2NoteLyric = tmp2;
			} 
			
			int measureDiff = mXML2Measure - mXML1Measure;
			if (measureDiff < minMeasureDistance || measureDiff > maxMeasureDistance) {
				return noMatch;
			}
			//has to be a lyric and a lyric onset to consider 
			
			double score = 0.0;
			
			List<Triple<String, StressedPhone[], StressedPhone>> mXML1LyricAndPronuns = mXML1NoteLyric.syllableStresses;
			List<Triple<String, StressedPhone[], StressedPhone>> mXML2LyricAndPronuns = mXML2NoteLyric.syllableStresses;
			
			final String mXML1Text = mXML1LyricAndPronuns.get(0).getFirst();
			if (endPunctuation.contains(mXML1Text.charAt(mXML1Text.length()-1))){
				score += earlierHasEndPunctuation;
			}
			final String mXML2Text = mXML2LyricAndPronuns.get(0).getFirst();
			if (endPunctuation.contains(mXML2Text.charAt(mXML2Text.length()-1))){
				score += laterHasEndPunctuation;
			}
			
			int maxSubScore = Integer.MIN_VALUE;
			int subscore;
			StressedPhone[] mXML1LyricPronun,mXML2LyricPronun;
			StressedPhone mXML1VowelPronun, mXML2VowelPronun;
			
			for (Triple<String, StressedPhone[], StressedPhone> mXML1LyricAndPronun : mXML1LyricAndPronuns) {
				for (Triple<String, StressedPhone[], StressedPhone> mXML2LyricAndPronun : mXML2LyricAndPronuns) {
					subscore = 0;
					mXML1LyricPronun = mXML1LyricAndPronun.getSecond();
					mXML2LyricPronun = mXML2LyricAndPronun.getSecond();
					mXML1VowelPronun = mXML1LyricAndPronun.getThird();
					mXML2VowelPronun = mXML2LyricAndPronun.getThird();
					if (mXML1VowelPronun.phone == mXML2VowelPronun.phone) {
						subscore += identicalVowel;
					} else {
						subscore += differentVowel;
					}
					if (mXML1VowelPronun.stress == mXML2VowelPronun.stress){
						subscore += sameStress;
					}
					
					subscore += hirjeeVowelMatchScore * hMatrix[mXML1VowelPronun.phone][mXML2VowelPronun.phone];
					subscore += patAlnScore * RhymeStructureAnalyzer.scoreRhymeByPatsRules(mXML1LyricPronun, mXML2LyricPronun);
					
					if (subscore > maxSubScore) {
						maxSubScore = subscore;
					}
				}
			}
			score += maxSubScore;
			
			score += super.scoreOffset(musicXML1AlignmentEvent, musicXML2AlignmentEvent);

			if (musicXML1AlignmentEvent.lyricOnset) {
				if (musicXML2AlignmentEvent.lyricOnset) {
					score *= bothLyricOnset;
				} else {
					score *= oneLyricOnsetOneNot;
				}
			} else {
				if (musicXML2AlignmentEvent.lyricOnset) {
					score *= oneLyricOnsetOneNot;
				} else {
					score *= neitherLyricOnset;
				}
			}
			
			return score;
		}
	}
	
	static int generation = 1;
	private static final File[] files = new File(
			TabDriver.dataDir + "/Wikifonia_edited_xmls").listFiles();
	
	private static final Map<String, String> songTitleFromFileName = new LinkedHashMap<String, String>();
	static{
		songTitleFromFileName.put("None","None");
		songTitleFromFileName.put("Traditional - Twinkle, twinkle, little star.xml","Twinkle, Twinkle, Little Star");
		songTitleFromFileName.put("Harold Arlen, Yip Harburg - Over The Rainbow.xml","Over the Rainbow");
		songTitleFromFileName.put("John Lennon and Paul McCartney - Hey Jude.xml","Hey Jude");
		songTitleFromFileName.put("Bill Danoff, Taffy Nivert & John Denver - Take Me Home, Country Roads.xml","Take Me Home, Country Roads");
		songTitleFromFileName.put("John Lennon - Imagine.xml","Imagine");
	}
	
	private static final List<ParsedMusicXMLObject> trainingSongs;
	static {
//		PopDriver.annotateSysOutErrCalls();

		trainingSongs = new ArrayList<ParsedMusicXMLObject>();
		for (File file : files) {
//			if (!file.getName().startsWith("Trad")) continue; // TODO: Filenameadjust marker
			if (!StructureExtractor.generalizedAnnotationsExistForFile(file)) {
				System.out.println("No annotations for " + file.getName());
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
			ParsedMusicXMLObject musicXML = musicXMLParser.parse(true);
			if (musicXML == null) {
				System.err.println("musicXML was null for " + file.getName());
				continue;
			}
			try {
				StructureExtractor.loadGeneralizedStructureAnnotations(musicXML);
			} catch (Exception e) {
				System.err.println("For " + file.getName() + ":\n");
				throw new RuntimeException(e);
			}
			
			trainingSongs.add(musicXML);
		}
	}

	private static final int populationSize = 20;
	private static final int LITTER_SIZE = 10;
	private static String TYPE = "lyric"; 
	private static String HOLDOUT; 
	private static String POPULATION_FILE;
	private static String HEATMAP_FILE_PREFIX;
	private final static int TOTAL_GENERATIONS = 2500;
	private static final String[] viewpoints = new String[]{"harmony","pitch","rhythm","lyric","chorus","verse"};
	
	private static double prevBestAccuracy = 0.0;
	public static void main(String[] args) throws Exception {
		
		// for each file (and "None") 
		for (String holdoutSong : songTitleFromFileName.keySet()) {
			// bar one file from the F-Score calculations
			System.out.println("Setting Holdout to " + holdoutSong);
			HOLDOUT = holdoutSong;
			// for each viewpoint
			for (String viewpoint : viewpoints) {
				System.out.println("Setting Type to " + viewpoint);
				TYPE = viewpoint;
				System.out.println("Resetting static vars...");
				
				POPULATION_FILE = "generalized_global_alignment_inference/parameterization_holdout_" + songTitleFromFileName.get(HOLDOUT) + "_";
				HEATMAP_FILE_PREFIX = "generalized_global_alignment_inference/holdout_" + songTitleFromFileName.get(HOLDOUT) + "_" + TYPE +"_visualizations/";
				generation = 1;
				prevBestAccuracy = 0.0;
				solutionIDMap = new HashMap<String, Integer>();
				// TRAIN

				// create/load initial population of x parameterizations and their accuracy scores when used to
				List<Pair<Double, GeneralizedGlobalStructureAlignmentParameterization>> population = loadInitialPopulation(TYPE);
				System.out.println(generation + "\t" + prevBestAccuracy);
				
				generation++;
				for (; generation <= TOTAL_GENERATIONS && prevBestAccuracy < 1.0; generation++) {
					// cross-over and mutate the scores, possible modifying just one score at a time?
					List<GeneralizedGlobalStructureAlignmentParameterization> offSpring = generateNewPopulation(TYPE,population);
					
					// score solutions 
					population.addAll(scoreParameterizations(offSpring, TYPE));
					
					// save/keep the top x parameterizations
					Collections.sort(population, new Comparator<Pair<Double, GeneralizedGlobalStructureAlignmentParameterization>>() {
						@Override
						public int compare(Pair<Double, GeneralizedGlobalStructureAlignmentParameterization> o1,
								Pair<Double, GeneralizedGlobalStructureAlignmentParameterization> o2) {
							final double d = o1.getFirst() - o2.getFirst();
							return d > 0 ? -1 : (d < 0 ? 1 : 0);
						}
					});
					
					List<Pair<Double, GeneralizedGlobalStructureAlignmentParameterization>> rest = population.subList(3, population.size());
					Collections.shuffle(rest);
					
					population = population.subList(0, 3);
					population.addAll(rest.subList(0, populationSize-3));
					final Pair<Double, GeneralizedGlobalStructureAlignmentParameterization> best = population.get(0);
					if (best.getFirst() > prevBestAccuracy) {
						prevBestAccuracy = best.getFirst();
						scoreParameterization(best.getSecond(), TYPE, HEATMAP_FILE_PREFIX); // save best heatmap
						System.out.println(generation + "\t" + prevBestAccuracy);
					}
					
					// print top y parameterizations
					savePopulationToFile(population, TYPE);
				}
			}
		}
	}
	
	private static void createDirForVisualizations(String path) {
		File theDir = new File(path);

		// if the directory does not exist, create it
		if (!theDir.exists()) {
		    System.out.println("creating directory: " + theDir.getName());
		    boolean result = false;

		    try{
		        theDir.mkdir();
		        result = true;
		    } 
		    catch(SecurityException se){
		        //handle it
		    }        
		    if(result) {    
		        System.out.println("DIR " + path + " created");  
		    }
		}
	}

	private static List<GeneralizedGlobalStructureAlignmentParameterization> generateNewPopulation(String viewpoint,
			List<Pair<Double, GeneralizedGlobalStructureAlignmentParameterization>> population) throws Exception {
		List<GeneralizedGlobalStructureAlignmentParameterization> newPop = new ArrayList<GeneralizedGlobalStructureAlignmentParameterization>();
		
		while (newPop.size() < LITTER_SIZE) {
			// selection
			int parentIdx = rand.nextInt(population.size());
			GeneralizedGlobalStructureAlignmentParameterization parent1 = population.get(parentIdx).getSecond();
			GeneralizedGlobalStructureAlignmentParameterization parent2;
			do {
				parentIdx = rand.nextInt(population.size());
				parent2 = population.get(parentIdx).getSecond();
			} while (parent2 == parent1);
			
			// crossover
			GeneralizedGlobalStructureAlignmentParameterization newOffspring = (GeneralizedGlobalStructureAlignmentParameterization) getParameterizationClass(viewpoint).getDeclaredConstructor(GeneralizedGlobalStructureAlignmentParameterization.class, GeneralizedGlobalStructureAlignmentParameterization.class).newInstance(parent1,parent2);
			// mutation
			newOffspring.mutate();
			if (!solutionIDMap.containsKey(newOffspring.toString())) {
				newPop.add(newOffspring);
			}
		}
		
		return newPop;
	}

	private static Class getParameterizationClass(String viewpoint) {
		Class parameterizationClass = null;
		if (viewpoint.equals("lyric"))
			parameterizationClass = LyricAlignmentParameterization.class;
		else if (viewpoint.equals("pitch"))
			parameterizationClass = PitchAlignmentParameterization.class;
		else if (viewpoint.equals("harmony"))
			parameterizationClass = HarmonicAlignmentParameterization.class;
		else if (viewpoint.equals("rhythm"))
			parameterizationClass = RhythmAlignmentParameterization.class;
		else if (viewpoint.equals("rhyme"))
			parameterizationClass = RhymeAlignmentParameterization.class;
		else if (viewpoint.equals("chorus") || viewpoint.equals("verse")) {
			parameterizationClass = CombinedAlignmentParameterization.class;
			CombinedAlignmentParameterization.setType(viewpoint);
		}
		else 
			throw new RuntimeException("Unknown viewpoint: " + viewpoint);
		
		return parameterizationClass;
	}

	static Map<String, Integer> solutionIDMap = new HashMap<String, Integer>();
	
	private static void savePopulationToFile(List<Pair<Double, GeneralizedGlobalStructureAlignmentParameterization>> population, String targetSegment) {
		try(FileWriter fw = new FileWriter(POPULATION_FILE + targetSegment + ".txt", true);
				BufferedWriter bw = new BufferedWriter(fw);
				PrintWriter out = new PrintWriter(bw))
		{
		    out.println("Generation " + generation);
		    for (Pair<Double, GeneralizedGlobalStructureAlignmentParameterization> pair : population) {
		    	Integer solutionID = pair.getSecond().getSolutionID();
				out.println(solutionID + "\t" + pair.getFirst() + "\t" + pair.getSecond());
			}
		    out.println("*****\n");
		} catch (IOException e) {
		}
	}

	private static List<Pair<Double, GeneralizedGlobalStructureAlignmentParameterization>> scoreParameterizations(List<GeneralizedGlobalStructureAlignmentParameterization> offSpring, String targetSegment) throws IOException {
		List<Pair<Double, GeneralizedGlobalStructureAlignmentParameterization>> offSpringPopulation = new ArrayList<Pair<Double, GeneralizedGlobalStructureAlignmentParameterization>>();
		boolean first = true;
		for (GeneralizedGlobalStructureAlignmentParameterization globalStructureAlignmentParameterization : offSpring) {
			double score = scoreParameterization(globalStructureAlignmentParameterization, targetSegment, null);
			first = false;
			offSpringPopulation.add(new Pair<>(score, globalStructureAlignmentParameterization));
		}
		return offSpringPopulation;
	}
	
	private static double scoreParameterization(GeneralizedGlobalStructureAlignmentParameterization globalStructureAlignmentParameterization, String targetSegment, String vizDirPath) throws IOException {
		double correct = 0.0;
		double total = 0.0;
		
		boolean isCombo = targetSegment.contains("_");
		
		if (DEBUG > 0) System.out.println("Scoring parameterization");
		for (ParsedMusicXMLObject song : trainingSongs) {
			if (HOLDOUT.equals(song.filename)) continue;
			if (DEBUG > 0) System.out.println(song.filename);
			// do an alignment for each song in the training set using the parameterization
			Object[] matrices = align(song, globalStructureAlignmentParameterization);
			double[][] alnMatrix = (double[][]) matrices[0];
			char[][] ptrMatrix = (char[][]) matrices[1];
//			
//			
//			//TODO remove
//			for (double[] row : alnMatrix) {
//				for (double col : row) {
//					System.out.print(df2.format(col) + " ");
//				}
//				System.out.println();
//			}
			
			// use the alignment to infer the locations of the target segment type
			Object[] inferredSegments = inferTargetSegmentLocations(alnMatrix, ptrMatrix, globalStructureAlignmentParameterization);
			List<Map<Integer, Integer>> inferredLocationStarts = (List<Map<Integer,Integer>>) inferredSegments[0];
			double[][] pathsTaken = (double[][]) inferredSegments[1];
			List<Pair<Pair<Integer,Integer>, Pair<Integer,Integer>>> matchedRegions = (List<Pair<Pair<Integer,Integer>, Pair<Integer,Integer>>>) inferredSegments[2];
			
			Utils.normalizeByMaxVal(pathsTaken);
			
			// Given the inferred locations of the target segment type and the actual global structure, compute the accuracy
			final Triple<Double, Double, Double> precisionRecallFScore = isCombo ? null:computePrecisionRecallFScore(song, inferredLocationStarts, song.getAlignmentEvents(globalStructureAlignmentParameterization.eventsPerBeat), targetSegment);
			if (DEBUG > 0 && !isCombo) System.out.println("Computed fScore: " + precisionRecallFScore.getThird());
			
//			if (total == 0.0 && generation % 100 == 0 && saveHeatmap) { // Always just print heatmap for first song
			if (vizDirPath != null) { // Always just print heatmap for first song
				String fileSuffix = isCombo? targetSegment:"_gen" + generation + "_id" + globalStructureAlignmentParameterization.getSolutionID();
				createDirForVisualizations(vizDirPath);
				final String txtPathname = vizDirPath + songTitleFromFileName.get(song.filename) + fileSuffix + ".txt";
				BufferedWriter bw = new BufferedWriter(new FileWriter(txtPathname));
				
				if (!isCombo) {
					bw.write("Precision:" + precisionRecallFScore.getFirst() + "\n");
					bw.write("Recall:" + precisionRecallFScore.getSecond() + "\n");
					bw.write("Accuracy:" + precisionRecallFScore.getThird() + "\n\n");
				}
				
				String mapTitle = isCombo? targetSegment:songTitleFromFileName.get(song.filename) + "      Viewpoint: " + StringUtils.capitalize(targetSegment) + "      Generation: " + generation + "      F-Score: " + df3.format(precisionRecallFScore.getThird());
				String pathname = vizDirPath + songTitleFromFileName.get(song.filename) +(isCombo?targetSegment:("_gen" + generation + "_id" + globalStructureAlignmentParameterization.getSolutionID())) + ".jpeg";
				saveHeatmap(pathname, mapTitle, pathsTaken, globalStructureAlignmentParameterization, song, matchedRegions, bw);
				bw.close();
			}
			
			if (!isCombo) {
				correct += precisionRecallFScore.getThird();
				total+=1;
			}
		}
		
		return correct/total;
	}

	static int minEventsFromDiagonal = -1; // how many events apart do events need to be before they can be considered for equality (computed to be about 2 measures)
	
	/**
	 * Given a parameterization and song, do an alignment 
	 * @return 
	 */
	private static Object[] align(ParsedMusicXMLObject song,
			GeneralizedGlobalStructureAlignmentParameterization globalStructureAlignmentParameterization) {
		
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
		
		double diag, up, left;
		for (int row = 1; row < matrixDimension; row++) {
			double[] prevMatrixRow = matrix[row-1];
			double[] currMatrixRow = matrix[row];
			char[] prevBacktrackRow = backtrack[row-1];
			char[] currBackTrackRow = backtrack[row];
			for (int col = row+minEventsFromDiagonal; col < matrixDimension; col++) {
				ParsedMusicXMLObject.MusicXMLAlignmentEvent musicXML1AlignmentEvent = events.get(row-1);
				ParsedMusicXMLObject.MusicXMLAlignmentEvent musicXML2AlignmentEvent = events.get(col-1);
				
				diag = prevMatrixRow[col-1] + globalStructureAlignmentParameterization.scoreMatch(musicXML1AlignmentEvent, musicXML2AlignmentEvent);
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
	@SuppressWarnings("unused")
	private static Object[] inferTargetSegmentLocations(double[][] alnMatrix,
			char[][] ptrMatrix, GeneralizedGlobalStructureAlignmentParameterization globalStructureAlignmentParameterization) {
		List<Map<Integer,Integer>> inferredLocations = new ArrayList<Map<Integer,Integer>>(Collections.nCopies(alnMatrix.length-1, null));
		double[][] pathsTaken = new double[alnMatrix.length][alnMatrix.length];
		int row,col;
		for (row = 0; row < alnMatrix.length; row++) {
			for (col = row; col < alnMatrix[row].length; col++) {
				pathsTaken[row][col] = alnMatrix[row][col];
			}
		}
		
		List<Triple<Double, Integer, Integer>> localMaxima = new ArrayList<Triple<Double, Integer, Integer>>();
		double minThresholdForLocalMaxima = globalStructureAlignmentParameterization.minThresholdForLocalMaxima;
		int maxAlignmentsToConsider = 50;
		
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
		localMaxima = localMaxima.subList(0, Math.min(localMaxima.size(), maxAlignmentsToConsider));
		// do some sort of weighted max-clique solution based on the row and col vals
//		if (DEBUG > 1) {
//			System.out.println("Finding max clique for top " + localMaxima.size() + "...");
//			for (Triple<Double, Integer, Integer> localMaximum: localMaxima) {
//				System.out.println(localMaximum);
//			}
//		}
//		reduceViaMaxClique(localMaxima);
		
		List<Pair<Pair<Integer,Integer>, Pair<Integer,Integer>>> matchedRegions = new ArrayList<Pair<Pair<Integer,Integer>, Pair<Integer,Integer>>>();
		
		int maxRow,maxCol;
		double maxVal = localMaxima.isEmpty()?-1.0:localMaxima.get(0).getFirst()*1.1;
		for (Triple<Double, Integer, Integer> triple : localMaxima) {
			// Backtrack
			maxRow = triple.getSecond();
			maxCol = triple.getThird();
			row = maxRow;
			col = maxCol;
			Map<Integer,Integer> inferredLocationForPos; 
			while (alnMatrix[row][col] > 0.) {
				inferredLocationForPos = inferredLocations.get(row-1);
				if (inferredLocationForPos == null) {
					inferredLocationForPos = new HashMap<Integer,Integer>();
					inferredLocationForPos.put(col-1, 1);
					inferredLocations.set(row-1, inferredLocationForPos);
				} else {
					Utils.incrementValueForKey(inferredLocationForPos, col-1);
				}
				inferredLocationForPos = inferredLocations.get(col-1);
				if (inferredLocationForPos == null) {
					inferredLocationForPos = new HashMap<Integer,Integer>();
					inferredLocationForPos.put(row-1, 1);

					inferredLocations.set(col-1, inferredLocationForPos);
				} else {
					Utils.incrementValueForKey(inferredLocationForPos,row-1);
				}
				pathsTaken[row][0] = maxVal;
				pathsTaken[col][0] = maxVal;
				pathsTaken[pathsTaken.length-1][col] = maxVal;
				pathsTaken[pathsTaken.length-1][row] = maxVal;
				pathsTaken[row][col] = maxVal;
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
			matchedRegions.add(new Pair<Pair<Integer,Integer>,Pair<Integer,Integer>>(new Pair<Integer,Integer>(row, maxRow-1), new Pair<Integer,Integer>(col, maxCol-1)));
		}
		
		return new Object[]{inferredLocations,pathsTaken,matchedRegions};
	}
		
	//attaches β times as much importance to recall as precision	
	private final static double fScoreBetaValueSquared = 1.0;
	public final static int groupLabelLength = 4;
	/**
	 * Given the inferred locations of the target segment type and the actual global structure, compute the accuracy 
	 * @param song 
	 * @param targetSegment 
	 */
	private static Triple<Double, Double, Double> computePrecisionRecallFScore(ParsedMusicXMLObject song, List<Map<Integer,Integer>> inferredMatchLocations,
			List<ParsedMusicXMLObject.MusicXMLAlignmentEvent> songEvents, String type) {

		Map<Integer,Integer> inferredMatchesForRowPosition;
		int truePositive = 0;
//		int trueNegative = 0;
		int falsePositive = 0;
		int falseNegative = 0;
		Set<String> actualGroups;
		MusicXMLAlignmentEvent currentSongEvent, inferredSongEvent;

		@SuppressWarnings("rawtypes")
		Map<Character, List> allMatchingGroups = getMatchingGroups(song, type);
		Set<String> inferredGroups;
		int allGroupsForLabelCount;
		
		Set<String> matchingRegions;
		for (int i = 0; i < songEvents.size(); i++) {
			currentSongEvent = songEvents.get(i);
			inferredMatchesForRowPosition = inferredMatchLocations.get(i);
			actualGroups = getGroups(type, currentSongEvent);
			
			if (actualGroups.isEmpty()) { // didn't belong to any groups, so shouldn't have any matches
				if (inferredMatchesForRowPosition == null) { // didn't find any matches
//					trueNegative++; // didn't infer match, was no match
					truePositive++;
				} else { // had matches (wrong)
					for (Integer count : inferredMatchesForRowPosition.values()) {
						falsePositive += count;
					}
				}
				// inferred matches, but was no match => incorrect
			} else {
				// what should it have inferred?
				matchingRegions = new HashSet<String>();
				for (String actualGroup: actualGroups) { // for each actual group (and group ID) for this event
					Character group = actualGroup.charAt(0); // abstract to the group label
					allGroupsForLabelCount = allMatchingGroups.get(group).size(); // find all group IDs with this label
					for (int j = 1; j <= allGroupsForLabelCount; j++) { // for each of the group IDs with this label
						String groupID = "" + group + j;
						if (!actualGroups.contains(groupID)) { // if the ID doesn't already belong to this event
							matchingRegions.add(groupID); // then this event should match to that ID
						}
					}
				}
				
				if (inferredMatchesForRowPosition == null) {
					falseNegative += matchingRegions.size(); // it didn't find any of the matching regions
				} else {
//					correctInferredPositionMatchingGroups = new HashSet<String>();
					for (Integer inferredPosition : inferredMatchesForRowPosition.keySet()) { // for each matching position
						falsePositive += inferredMatchesForRowPosition.get(inferredPosition)-1; // if any event aligned multiple times it's definitely wrong for all but the first time
						inferredSongEvent = songEvents.get(inferredPosition); // get the event for the position
						
						inferredGroups = getGroups(type, inferredSongEvent); // get the inferred Groups for the event
						
						// for each inferred group, see if it is correct
						for (String inferredGroup : inferredGroups) {
							if (matchingRegions.remove(inferredGroup)) { // if it was in matching regions, we take it out and 
								// say the inference was correct
								truePositive++;
							} else { // if it was not in matching regions, or was already taken out because it was already matched by a previously inferred position
								falsePositive++;
							}
						}
					}
					// after looking through all (and removing correct) inferred groups, any matching regions that weren't inferred are false negatives 
					falseNegative += matchingRegions.size();
				}
			}
		}
		Double precision = 1.0 * truePositive / (truePositive + falsePositive);
		Double recall = 1.0 * truePositive / (truePositive + falseNegative);
		Double fScore = ((1+fScoreBetaValueSquared) * truePositive) / ((1+fScoreBetaValueSquared) * truePositive + fScoreBetaValueSquared * falseNegative + falsePositive);
		
		return new Triple<Double,Double, Double>(precision,recall, fScore);
	}

	private static Set<String> getGroups(String type,
			MusicXMLAlignmentEvent currentSongEvent) {
		if (type.equals("lyric")) {
			return currentSongEvent.getLyricGroups();
		} else if (type.equals("pitch")){
			return currentSongEvent.getPitchGroups();
		} else if (type.equals("harmony")){
			return currentSongEvent.getHarmonyGroups();
		} else if (type.equals("rhythm")){
			return currentSongEvent.getRhythmGroups();
		} else if (type.equals("rhyme")){
			return currentSongEvent.getRhymeGroups();
		} else if (type.equals("chorus")){
			return currentSongEvent.getChorusGroups();
		} else if (type.equals("verse")){
			return currentSongEvent.getVerseGroups();
		} else {
			throw new RuntimeException("Not yet implemented");
		}
	}

	@SuppressWarnings("unchecked")
	private static <T extends List> Map<Character, T> getMatchingGroups(
			ParsedMusicXMLObject song, String type) {
		if (type.equals("lyric")) {
			return (Map<Character, T>) song.getAllMatchingLyricGroups();
		} else if (type.equals("pitch")){
			return (Map<Character, T>) song.getAllMatchingPitchGroups();
		} else if (type.equals("harmony")){
			return (Map<Character, T>) song.getAllMatchingHarmonyGroups();
		} else if (type.equals("rhythm")){
			return (Map<Character, T>) song.getAllMatchingRhythmGroups();
		} else if (type.equals("rhyme")){
			return (Map<Character, T>) song.getAllMatchingRhymeGroups();
		} else if (type.equals("chorus")){
			return (Map<Character, T>) song.getAllMatchingChorusGroups();
		} else if (type.equals("verse")){
			return (Map<Character, T>) song.getAllMatchingVerseGroups();
		} else {
			throw new RuntimeException("Not yet implemented");
		}
	}

	/**
	 * Given a solution alignment matrix, a score, and the generation, output a heatmap representation of the matrix
	 * @param song 
	 * @param matchedRegions 
	 * @throws IOException 
	 */
	private static void saveHeatmap(String pathname, String mapTitle, double[][] pathsTaken,
			GeneralizedGlobalStructureAlignmentParameterization globalStructureAlignmentParameterization, ParsedMusicXMLObject song, List<Pair<Pair<Integer,Integer>, 
			Pair<Integer,Integer>>> matchedRegions,
			BufferedWriter bw) throws IOException {

		List<ParsedMusicXMLObject.MusicXMLAlignmentEvent> alignmentEvents = song.getAlignmentEvents(globalStructureAlignmentParameterization.eventsPerBeat);
			
		for (Pair<Pair<Integer, Integer>, Pair<Integer, Integer>> pair : matchedRegions) {
			Pair<Integer,Integer> firstMatch = pair.getFirst();
			Pair<Integer,Integer> secondMatch = pair.getSecond();
			int firstMatchStart = firstMatch.getFirst();
			int firstMatchEnd = firstMatch.getSecond();
			int secondMatchStart = secondMatch.getFirst();
			int secondMatchEnd = secondMatch.getSecond();
			
			MusicXMLAlignmentEvent event = alignmentEvents.get(firstMatchStart);
			bw.write("" + event.measure);
			bw.write('\t');
			for (int i = firstMatchStart; i <= firstMatchEnd; i++) {
				event = alignmentEvents.get(i);
				if (!event.strippedLyricLowerCaseText.isEmpty()) {
					if (event.lyricOnset) {
						bw.write(event.lyric.text);
						bw.write(' ');
					} else if (i == firstMatchStart) {
						bw.write('[');
						bw.write(event.lyric.text);
						bw.write(']');
						bw.write(' ');
					}
				}
			}
			bw.write('\t');
			bw.write("" + event.measure);
			bw.write('\n');
			
			event = alignmentEvents.get(secondMatchStart);
			bw.write("" + event.measure);
			bw.write('\t');
			for (int i = secondMatchStart; i <= secondMatchEnd; i++) {
				event = alignmentEvents.get(i);
				if (!event.strippedLyricLowerCaseText.isEmpty()) {
					if (event.lyricOnset) {
						bw.write(event.lyric.text);
						bw.write(' ');
					} else if (i == firstMatchStart) {
						bw.write('[');
						bw.write(event.lyric.text);
						bw.write(']');
						bw.write(' ');
					}
				}
			}
			bw.write('\t');
			bw.write("" + event.measure);
			bw.write('\n');
			
			bw.write('\n');
		}
		
		int chartXDimension = alignmentEvents.size() + 1; 
		int chartYDimension = alignmentEvents.size() + 1; 

		String[] xValues = new String[chartXDimension];
		String[] yValues = new String[chartYDimension];
		double[][] chartValues = new double[chartYDimension][chartXDimension];
		
		// set axis labels
		MusicXMLAlignmentEvent event = null;
		for (int i = 0; i < xValues.length; i++) {
			if (i != 0) event = alignmentEvents.get(i-1);
			if (i != 0 && event.lyricOnset && event.lyric != null) {
				xValues[i] = event.lyric.text;
				yValues[i] = event.lyric.text;
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
		chart.setLowValueColour(Color.WHITE);
		
//		chart.setYAxisLabel(song.filename);
//		chart.setXAxisLabel(song.filename);
		chart.setTitle(mapTitle);
		chart.setXValues(xValues);
		chart.setYValues(yValues);
		chart.setTitleFont(new Font("Times", Font.BOLD, (int) (chartXDimension/2)));
		chart.setAxisLabelsFont(new Font("Times", Font.BOLD, (int) (chartXDimension/3.0)));
		chart.setAxisValuesFont(new Font("Times", Font.BOLD, (int) (chartXDimension/2.5)));
//		chart.setCellSize(new Dimension(5,5));
		
		try {
			chart.saveToFile(new File(pathname));
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}

	private static List<Pair<Double, GeneralizedGlobalStructureAlignmentParameterization>> loadInitialPopulation(String targetSegment) throws Exception {
		List<Pair<Double, GeneralizedGlobalStructureAlignmentParameterization>> population = new ArrayList<Pair<Double, GeneralizedGlobalStructureAlignmentParameterization>>();
		try {
			population = loadInitialPopulationFromFile(targetSegment, true);
		} catch (FileNotFoundException e) {
			System.out.println("Initializing population of parameterizations");
			// need to create initial population!
			for (int i = 0; i < populationSize; i++) {
				final GeneralizedGlobalStructureAlignmentParameterization globalStructureAlignmentParameterization = (GeneralizedGlobalStructureAlignmentParameterization) getParameterizationClass(targetSegment).newInstance();
//				System.out.println("\tScoring initial parameterization:" + globalStructureAlignmentParameterization.toString());
				final double score = scoreParameterization(globalStructureAlignmentParameterization, targetSegment, null);
				System.out.println("\t\t" + score);
				population.add(new Pair<Double, GeneralizedGlobalStructureAlignmentParameterization>(score,globalStructureAlignmentParameterization));
			}
			Collections.sort(population, new Comparator<Pair<Double, GeneralizedGlobalStructureAlignmentParameterization>>() {
				@Override
				public int compare(Pair<Double, GeneralizedGlobalStructureAlignmentParameterization> o1,
						Pair<Double, GeneralizedGlobalStructureAlignmentParameterization> o2) {
					final double d = o1.getFirst() - o2.getFirst();
					return d > 0 ? -1 : (d < 0 ? 1 : 0);
				}
			});
			Pair<Double, GeneralizedGlobalStructureAlignmentParameterization> initBest = population.get(0);
			scoreParameterization(initBest.getSecond(), targetSegment, HEATMAP_FILE_PREFIX); // save initial heatmap

			savePopulationToFile(population, targetSegment);
		}
		prevBestAccuracy = population.get(0).getFirst();

		return population;
	}

	private static List<Pair<Double, GeneralizedGlobalStructureAlignmentParameterization>> loadInitialPopulationFromFile(String targetSegment, boolean modifyGlobalVariables)
			throws FileNotFoundException, InstantiationException, IllegalAccessException, InvocationTargetException,
			NoSuchMethodException {
		Scanner fileScanner = new Scanner(new File(POPULATION_FILE + targetSegment + ".txt"));
		if (modifyGlobalVariables) System.out.println("Loading initial population from file");
		List<Pair<Double, GeneralizedGlobalStructureAlignmentParameterization>> population = null;
		while (fileScanner.hasNextLine()) {
			if (modifyGlobalVariables) generation = Integer.parseInt(fileScanner.nextLine().split(" ")[1]);
			else fileScanner.nextLine();
			population = new ArrayList<Pair<Double, GeneralizedGlobalStructureAlignmentParameterization>>();
			while (fileScanner.hasNextLine()){
				String nextLine = fileScanner.nextLine();
				if (nextLine.startsWith("*****")) {
					fileScanner.nextLine();
					break;
				}
				String[] nextTokens = nextLine.split("\\t",3);
				final GeneralizedGlobalStructureAlignmentParameterization globalStructureAlignmentParameterization = (GeneralizedGlobalStructureAlignmentParameterization) getParameterizationClass(targetSegment).getDeclaredConstructor(String[].class).newInstance(new Object[] {nextTokens[2].split(", ")});
				int solutionID = Integer.parseInt(nextTokens[0]);
				if (modifyGlobalVariables) {
					Integer existingSolutionID = solutionIDMap.get(globalStructureAlignmentParameterization.toString());
					if (existingSolutionID == null) {
						solutionIDMap.put(globalStructureAlignmentParameterization.toString(), solutionID);
					} else {
						assert(existingSolutionID == solutionID);
					}
				}
				
				if (population.size() < populationSize) {
					double score = Double.parseDouble(nextTokens[1]);
					population.add(new Pair<Double, GeneralizedGlobalStructureAlignmentParameterization>(score,globalStructureAlignmentParameterization));
				}
			}
		}
		fileScanner.close();
		return population;
	}

}
