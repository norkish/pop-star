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
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.naming.OperationNotSupportedException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.tc33.jheatchart.HeatChart;
import org.w3c.dom.Document;

import config.SongConfiguration;
import data.MusicXMLParser;
import data.MusicXMLParser.Harmony;
import data.MusicXMLParser.Note;
import data.MusicXMLSummaryGenerator;
import data.ParsedMusicXMLObject;
import data.ParsedMusicXMLObject.MusicXMLAlignmentEvent;
import globalstructure.SegmentType;
import globalstructure.StructureExtractor;
import main.PopDriver;
import tabcomplete.main.TabDriver;
import utils.Pair;
import utils.Triple;
import utils.Utils;

public class GeneralizedGlobalStructureInferer {

	final private static int DEBUG = 0;
	
	private static DecimalFormat df2 = new DecimalFormat(".##");
	private static DecimalFormat df3 = new DecimalFormat(".###");
	protected final static Random rand = new Random(SongConfiguration.randSeed);

	public static abstract class GeneralizedGlobalStructureAlignmentParameterization {
		
		protected static double MUTATION_RATE = 0.5;
		protected static int MAX_MUTATION_STEP = 5;
		
		// gap scores
		public double gapOpenScore;
		public double gapExtendScore;
		
		// alignment non-scoring params
		public double minThresholdForLocalMaxima;
		public int distanceFromDiagonalInBeats;
		public int eventsPerBeat; //number of divisions into which the beat should be divided.

		public GeneralizedGlobalStructureAlignmentParameterization() {
			gapOpenScore = rand.nextInt(7)-3;//rand.nextDouble() * 0.25;
			gapExtendScore = rand.nextInt(7)-3;//rand.nextDouble() * 0.25;
			minThresholdForLocalMaxima = rand.nextDouble() * 20;
			distanceFromDiagonalInBeats = rand.nextInt(5)+6; // distance from diagonal
			eventsPerBeat = (int) Math.pow(2,rand.nextInt(2)); // events per beat
		}
		
		public GeneralizedGlobalStructureAlignmentParameterization(GeneralizedGlobalStructureAlignmentParameterization p1, GeneralizedGlobalStructureAlignmentParameterization p2){
			this.gapOpenScore = (rand.nextBoolean()?p1.gapOpenScore:p2.gapOpenScore);
			this.gapExtendScore = (rand.nextBoolean()?p1.gapExtendScore:p2.gapExtendScore);
			this.minThresholdForLocalMaxima = (rand.nextBoolean()?p1.minThresholdForLocalMaxima:p2.minThresholdForLocalMaxima);
			this.distanceFromDiagonalInBeats = (rand.nextBoolean()?p1.distanceFromDiagonalInBeats:p2.distanceFromDiagonalInBeats);
			this.eventsPerBeat = (rand.nextBoolean()?p1.eventsPerBeat:p2.eventsPerBeat);
		}

		public GeneralizedGlobalStructureAlignmentParameterization(String[] nextTokens) {
			int i = 0;
			this.gapOpenScore = Double.parseDouble(nextTokens[i++]);
			this.gapExtendScore = Double.parseDouble(nextTokens[i++]);
			this.minThresholdForLocalMaxima = Double.parseDouble(nextTokens[i++]);
			this.distanceFromDiagonalInBeats = Integer.parseInt(nextTokens[i++]);
			this.eventsPerBeat = Integer.parseInt(nextTokens[i++]);
		}

		public abstract GeneralizedGlobalStructureAlignmentParameterization crossoverWith(GeneralizedGlobalStructureAlignmentParameterization p2);
		
		public void mutate() {
			if (rand.nextDouble() < MUTATION_RATE)
				this.gapOpenScore += (rand.nextBoolean()?1:-1) * rand.nextInt(MAX_MUTATION_STEP);
			if (rand.nextDouble() < MUTATION_RATE)
				this.gapExtendScore += (rand.nextBoolean()?1:-1) * rand.nextInt(MAX_MUTATION_STEP);
			if (rand.nextDouble() < MUTATION_RATE)
				this.minThresholdForLocalMaxima *= rand.nextDouble() * 2;
			if (rand.nextDouble() < MUTATION_RATE) {
				this.distanceFromDiagonalInBeats += (rand.nextBoolean()?1:-1) * rand.nextInt(MAX_MUTATION_STEP);
				if (this.distanceFromDiagonalInBeats > 20) {
					this.distanceFromDiagonalInBeats = 20;
				} else if (this.distanceFromDiagonalInBeats < 2) {
					this.distanceFromDiagonalInBeats = 2;
				}
			}
//			if (rand.nextDouble() < MUTATION_RATE)
//				this.eventsPerBeat = (int) Math.pow(2,rand.nextInt(3));	
			
			mutateSubclassParameters();
		}
		
		protected abstract void mutateSubclassParameters();
		
		public abstract double scoreMatch(MusicXMLAlignmentEvent musicXML1AlignmentEvent, MusicXMLAlignmentEvent musicXML2AlignmentEvent);
		
		public String toString() {
			return 
					df2.format(gapOpenScore) + ", " + 
					df2.format(gapExtendScore) + ", " +
					df2.format(minThresholdForLocalMaxima) + ", " +
					distanceFromDiagonalInBeats + ", " + 
					eventsPerBeat;
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

	public static class LyricAlignmentParameterization extends GeneralizedGlobalStructureAlignmentParameterization {

		// pitch weights
		public double bothRests;
		public double oneRests;
		public double neitherRests;

		// lyric weights
		public double lyricsEqual;
		public double lyricsUnequal;
		public double bothLyricsNull;
		public double oneLyricsNull;
		public double bothLyricsOnset;
		public double bothLyricsNotOnset;
		public double oneLyricOnsetOneNot;

		// measure offset match score
		public double haveSameMeasureOffset;
		public double haveDifferentMeasureOffset;
		public double measureOffsetDifference;

		public LyricAlignmentParameterization() {
			bothRests = rand.nextInt(7)-3;
			oneRests = rand.nextInt(7)-3;
			neitherRests = rand.nextInt(7)-3;
			lyricsEqual = rand.nextInt(7)-3;
			lyricsUnequal = rand.nextInt(7)-3;
			bothLyricsNull = rand.nextInt(7)-3;
			oneLyricsNull = rand.nextInt(7)-3;
			bothLyricsOnset = rand.nextInt(7)-3;
			bothLyricsNotOnset = rand.nextInt(7)-3;
			oneLyricOnsetOneNot = rand.nextInt(7)-3;
			haveSameMeasureOffset = rand.nextInt(7)-3;
			haveDifferentMeasureOffset = rand.nextInt(7)-3;
			measureOffsetDifference = rand.nextInt(7)-3;
		}

		public LyricAlignmentParameterization(String[] nextTokens) {
			super(nextTokens);
			
			int i = 4;
			this.bothRests = Double.parseDouble(nextTokens[i++]);
			this.oneRests = Double.parseDouble(nextTokens[i++]);
			this.neitherRests = Double.parseDouble(nextTokens[i++]);
			this.lyricsEqual = Double.parseDouble(nextTokens[i++]);
			this.lyricsUnequal = Double.parseDouble(nextTokens[i++]);
			this.bothLyricsNull = Double.parseDouble(nextTokens[i++]);
			this.oneLyricsNull = Double.parseDouble(nextTokens[i++]);
			this.bothLyricsOnset = Double.parseDouble(nextTokens[i++]);
			this.bothLyricsNotOnset = Double.parseDouble(nextTokens[i++]);
			this.oneLyricOnsetOneNot = Double.parseDouble(nextTokens[i++]);
			this.haveSameMeasureOffset = Double.parseDouble(nextTokens[i++]);
			this.haveDifferentMeasureOffset = Double.parseDouble(nextTokens[i++]);
			this.measureOffsetDifference = Double.parseDouble(nextTokens[i++]);
		}

		@Override
		public String toString() {
			return 
					super.toString() + ", " +
					df2.format(bothRests) + ", " + 
					df2.format(oneRests) + ", " +
					df2.format(neitherRests) + ", " + 
					df2.format(lyricsEqual) + ", " + 
					df2.format(lyricsUnequal) + ", " + 
					df2.format(bothLyricsNull) + ", " + 
					df2.format(oneLyricsNull) + ", " + 
					df2.format(bothLyricsOnset) + ", " + 
					df2.format(bothLyricsNotOnset) + ", " + 
					df2.format(oneLyricOnsetOneNot) + ", " + 
					df2.format(haveSameMeasureOffset) + ", " + 
					df2.format(haveDifferentMeasureOffset) + ", " + 
					df2.format(measureOffsetDifference);
		}
		
		public LyricAlignmentParameterization(GeneralizedGlobalStructureAlignmentParameterization p1g, GeneralizedGlobalStructureAlignmentParameterization p2g) {
			super(p1g, p2g);
			
			LyricAlignmentParameterization p1 = (LyricAlignmentParameterization) p1g;
			LyricAlignmentParameterization p2 = (LyricAlignmentParameterization) p2g;
			
			this.bothRests = (rand.nextBoolean()?p1.bothRests:p2.bothRests);
			this.oneRests = (rand.nextBoolean()?p1.oneRests:p2.oneRests);
			this.neitherRests = (rand.nextBoolean()?p1.neitherRests:p2.neitherRests);
			this.lyricsEqual = (rand.nextBoolean()?p1.lyricsEqual:p2.lyricsEqual);
			this.lyricsUnequal = (rand.nextBoolean()?p1.lyricsUnequal:p2.lyricsUnequal);
			this.bothLyricsNull = (rand.nextBoolean()?p1.bothLyricsNull:p2.bothLyricsNull);
			this.oneLyricsNull = (rand.nextBoolean()?p1.oneLyricsNull:p2.oneLyricsNull);
			this.bothLyricsOnset = (rand.nextBoolean()?p1.bothLyricsOnset:p2.bothLyricsOnset);
			this.bothLyricsNotOnset = (rand.nextBoolean()?p1.bothLyricsNotOnset:p2.bothLyricsNotOnset);
			this.oneLyricOnsetOneNot = (rand.nextBoolean()?p1.oneLyricOnsetOneNot:p2.oneLyricOnsetOneNot);
			this.haveSameMeasureOffset = (rand.nextBoolean()?p1.haveSameMeasureOffset:p2.haveSameMeasureOffset);
			this.haveDifferentMeasureOffset = (rand.nextBoolean()?p1.haveDifferentMeasureOffset:p2.haveDifferentMeasureOffset);
			this.measureOffsetDifference = (rand.nextBoolean()?p1.measureOffsetDifference:p2.measureOffsetDifference);
		}

		public void mutateSubclassParameters() {
			if (rand.nextDouble() < MUTATION_RATE)
				this.bothRests = (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
			if (rand.nextDouble() < MUTATION_RATE)
				this.oneRests = (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
			if (rand.nextDouble() < MUTATION_RATE)
				this.neitherRests = (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
			if (rand.nextDouble() < MUTATION_RATE)
				this.lyricsEqual += (rand.nextBoolean()?1:-1) * rand.nextInt(MAX_MUTATION_STEP); 
			if (rand.nextDouble() < MUTATION_RATE)
				this.lyricsUnequal += (rand.nextBoolean()?1:-1) * rand.nextInt(MAX_MUTATION_STEP); 
			if (rand.nextDouble() < MUTATION_RATE)
				this.bothLyricsNull += (rand.nextBoolean()?1:-1) * rand.nextInt(MAX_MUTATION_STEP);
			if (rand.nextDouble() < MUTATION_RATE)
				this.oneLyricsNull += (rand.nextBoolean()?1:-1) * rand.nextInt(MAX_MUTATION_STEP);
			if (rand.nextDouble() < MUTATION_RATE)
				this.bothLyricsOnset = (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
			if (rand.nextDouble() < MUTATION_RATE)
				this.bothLyricsNotOnset = (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
			if (rand.nextDouble() < MUTATION_RATE)
				this.oneLyricOnsetOneNot = (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
			if (rand.nextDouble() < MUTATION_RATE)
				this.haveSameMeasureOffset = (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
			if (rand.nextDouble() < MUTATION_RATE)
				this.haveDifferentMeasureOffset = (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
			if (rand.nextDouble() < MUTATION_RATE)
				this.measureOffsetDifference = (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
		}

		@Override
		public GeneralizedGlobalStructureAlignmentParameterization crossoverWith(
				GeneralizedGlobalStructureAlignmentParameterization p2) {
			return new LyricAlignmentParameterization(this, (LyricAlignmentParameterization) p2);
		}
		
		@Override
		public double scoreMatch(MusicXMLAlignmentEvent musicXML1AlignmentEvent,
				MusicXMLAlignmentEvent musicXML2AlignmentEvent) {
			String mXML1Lyric = musicXML1AlignmentEvent.strippedLyricLCText;
			String mXML2Lyric = musicXML2AlignmentEvent.strippedLyricLCText;
			
			if (musicXML1AlignmentEvent.note.pitch == Note.REST) {
				if (musicXML2AlignmentEvent.note.pitch == Note.REST) {
					// both rests
					return bothRests;
				} else {
					// one rest
					return oneRests;
				}
			} else if (musicXML2AlignmentEvent.note.pitch == Note.REST) {
				// one rest
				return oneRests;
			} else {
				// neither are rests
				if (mXML1Lyric.isEmpty()) {
					if (mXML2Lyric.isEmpty()) {
						// neither have lyrics, but notes are on
						return bothLyricsNull;
					} else {
						// one has lyrics, both notes are on
						return oneLyricsNull;
					}
				} else {
					if (mXML2Lyric.isEmpty()) {
						// one has lyrics, both notes are on
						return oneLyricsNull;
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
						
						//offset value
						double offsetDifference = Math.abs(musicXML1AlignmentEvent.beat - musicXML2AlignmentEvent.beat);
						if (offsetDifference == 0.0) {
							matchScore += haveSameMeasureOffset;
						} else {
							matchScore += haveDifferentMeasureOffset;
							matchScore += measureOffsetDifference * offsetDifference;
						}
						
						return matchScore;
					}
				}
			}
		}
	}
	
	public static class PitchAlignmentParameterization extends GeneralizedGlobalStructureAlignmentParameterization {

		// pitch weights
		public double bothRests;
		public double oneRests;
		public double neitherRests;

		public double pitchesEqual;
		public double pitchesUnequal;
		public double pitchDifference;
		public double bothPitchesOnset;
		public double bothPitchesNotOnset;
		public double onePitchOnsetOneNot;

		// measure offset match score
		public double haveSameMeasureOffset;
		public double haveDifferentMeasureOffset;
		public double measureOffsetDifference;

		public PitchAlignmentParameterization() {
			bothRests = rand.nextInt(7)-3;
			oneRests = rand.nextInt(7)-3;
			neitherRests = rand.nextInt(7)-3;
			pitchesEqual = rand.nextInt(7)-3;
			pitchesUnequal = rand.nextInt(7)-3;
			pitchDifference = rand.nextInt(7)-3;
			bothPitchesOnset = rand.nextInt(7)-3;
			bothPitchesNotOnset = rand.nextInt(7)-3;
			onePitchOnsetOneNot = rand.nextInt(7)-3;
			haveSameMeasureOffset = rand.nextInt(7)-3;
			haveDifferentMeasureOffset = rand.nextInt(7)-3;
			measureOffsetDifference = rand.nextInt(7)-3;
		}

		public PitchAlignmentParameterization(String[] nextTokens) {
			super(nextTokens);
			
			int i = 4;
			this.bothRests = Double.parseDouble(nextTokens[i++]);
			this.oneRests = Double.parseDouble(nextTokens[i++]);
			this.neitherRests = Double.parseDouble(nextTokens[i++]);
			this.pitchesEqual = Double.parseDouble(nextTokens[i++]);
			this.pitchesUnequal = Double.parseDouble(nextTokens[i++]);
			this.pitchDifference = Double.parseDouble(nextTokens[i++]);
			this.bothPitchesOnset = Double.parseDouble(nextTokens[i++]);
			this.bothPitchesNotOnset = Double.parseDouble(nextTokens[i++]);
			this.onePitchOnsetOneNot = Double.parseDouble(nextTokens[i++]);
			this.haveSameMeasureOffset = Double.parseDouble(nextTokens[i++]);
			this.haveDifferentMeasureOffset = Double.parseDouble(nextTokens[i++]);
			this.measureOffsetDifference = Double.parseDouble(nextTokens[i++]);
		}

		@Override
		public String toString() {
			return 
					super.toString() + ", " +
					df2.format(bothRests) + ", " + 
					df2.format(oneRests) + ", " +
					df2.format(neitherRests) + ", " + 
					df2.format(pitchesEqual) + ", " + 
					df2.format(pitchesUnequal) + ", " + 
					df2.format(pitchDifference) + ", " + 
					df2.format(bothPitchesOnset) + ", " + 
					df2.format(bothPitchesNotOnset) + ", " + 
					df2.format(onePitchOnsetOneNot) + ", " + 
					df2.format(haveSameMeasureOffset) + ", " + 
					df2.format(haveDifferentMeasureOffset) + ", " + 
					df2.format(measureOffsetDifference);
		}
		
		public PitchAlignmentParameterization(GeneralizedGlobalStructureAlignmentParameterization p1g, GeneralizedGlobalStructureAlignmentParameterization p2g) {
			super(p1g, p2g);
			
			PitchAlignmentParameterization p1 = (PitchAlignmentParameterization) p1g;
			PitchAlignmentParameterization p2 = (PitchAlignmentParameterization) p2g;
			
			this.bothRests = (rand.nextBoolean()?p1.bothRests:p2.bothRests);
			this.oneRests = (rand.nextBoolean()?p1.oneRests:p2.oneRests);
			this.neitherRests = (rand.nextBoolean()?p1.neitherRests:p2.neitherRests);
			this.pitchesEqual = (rand.nextBoolean()?p1.pitchesEqual:p2.pitchesEqual);
			this.pitchesUnequal = (rand.nextBoolean()?p1.pitchesUnequal:p2.pitchesUnequal);
			this.pitchDifference = (rand.nextBoolean()?p1.pitchDifference:p2.pitchDifference);
			this.bothPitchesOnset = (rand.nextBoolean()?p1.bothPitchesOnset:p2.bothPitchesOnset);
			this.bothPitchesNotOnset = (rand.nextBoolean()?p1.bothPitchesNotOnset:p2.bothPitchesNotOnset);
			this.onePitchOnsetOneNot = (rand.nextBoolean()?p1.onePitchOnsetOneNot:p2.onePitchOnsetOneNot);
			this.haveSameMeasureOffset = (rand.nextBoolean()?p1.haveSameMeasureOffset:p2.haveSameMeasureOffset);
			this.haveDifferentMeasureOffset = (rand.nextBoolean()?p1.haveDifferentMeasureOffset:p2.haveDifferentMeasureOffset);
			this.measureOffsetDifference = (rand.nextBoolean()?p1.measureOffsetDifference:p2.measureOffsetDifference);
		}

		public void mutateSubclassParameters() {
			if (rand.nextDouble() < MUTATION_RATE)
				this.bothRests = (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
			if (rand.nextDouble() < MUTATION_RATE)
				this.oneRests = (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
			if (rand.nextDouble() < MUTATION_RATE)
				this.neitherRests = (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
			if (rand.nextDouble() < MUTATION_RATE)
				this.pitchesEqual += (rand.nextBoolean()?1:-1) * rand.nextInt(MAX_MUTATION_STEP); 
			if (rand.nextDouble() < MUTATION_RATE)
				this.pitchesUnequal += (rand.nextBoolean()?1:-1) * rand.nextInt(MAX_MUTATION_STEP); 
			if (rand.nextDouble() < MUTATION_RATE)
				this.pitchDifference += (rand.nextBoolean()?1:-1) * rand.nextInt(MAX_MUTATION_STEP);
			if (rand.nextDouble() < MUTATION_RATE)
				this.bothPitchesOnset = (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
			if (rand.nextDouble() < MUTATION_RATE)
				this.bothPitchesNotOnset = (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
			if (rand.nextDouble() < MUTATION_RATE)
				this.onePitchOnsetOneNot = (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
			if (rand.nextDouble() < MUTATION_RATE)
				this.haveSameMeasureOffset = (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
			if (rand.nextDouble() < MUTATION_RATE)
				this.haveDifferentMeasureOffset = (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
			if (rand.nextDouble() < MUTATION_RATE)
				this.measureOffsetDifference = (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
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
					score += oneRests;
				}
			} else {
				if (mXML2Pitch == Note.REST) {
					score += oneRests;
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
					
					//offset value
					double offsetDifference = Math.abs(musicXML1AlignmentEvent.beat - musicXML2AlignmentEvent.beat);
					if (offsetDifference == 0.0) {
						score += haveSameMeasureOffset;
					} else {
						score += haveDifferentMeasureOffset;
						score += measureOffsetDifference * offsetDifference;
					}
					
				}
			}
			return score;
		}
	}
	
	public static class HarmonicAlignmentParameterization extends GeneralizedGlobalStructureAlignmentParameterization {

		public double harmonyEqual;
		public double harmonyUnequal;
		public double harmonyDifference;
		public double bothHarmonyOnset;
		public double bothHarmonyNotOnset;
		public double oneHarmonyOnsetOneNot;

		// measure offset match score
		public double haveSameMeasureOffset;
		public double haveDifferentMeasureOffset;
		public double measureOffsetDifference;

		public HarmonicAlignmentParameterization() {
			harmonyEqual = rand.nextInt(7)-3;
			harmonyUnequal = rand.nextInt(7)-3;
			harmonyDifference = rand.nextInt(7)-3;
			bothHarmonyOnset = rand.nextInt(7)-3;
			bothHarmonyNotOnset = rand.nextInt(7)-3;
			oneHarmonyOnsetOneNot = rand.nextInt(7)-3;
			haveSameMeasureOffset = rand.nextInt(7)-3;
			haveDifferentMeasureOffset = rand.nextInt(7)-3;
			measureOffsetDifference = rand.nextInt(7)-3;
		}

		public HarmonicAlignmentParameterization(String[] nextTokens) {
			super(nextTokens);
			
			int i = 4;
			this.harmonyEqual = Double.parseDouble(nextTokens[i++]);
			this.harmonyUnequal = Double.parseDouble(nextTokens[i++]);
			this.harmonyDifference = Double.parseDouble(nextTokens[i++]);
			this.bothHarmonyOnset = Double.parseDouble(nextTokens[i++]);
			this.bothHarmonyNotOnset = Double.parseDouble(nextTokens[i++]);
			this.oneHarmonyOnsetOneNot = Double.parseDouble(nextTokens[i++]);
			this.haveSameMeasureOffset = Double.parseDouble(nextTokens[i++]);
			this.haveDifferentMeasureOffset = Double.parseDouble(nextTokens[i++]);
			this.measureOffsetDifference = Double.parseDouble(nextTokens[i++]);
		}

		@Override
		public String toString() {
			return 
					super.toString() + ", " +
					df2.format(harmonyEqual) + ", " + 
					df2.format(harmonyUnequal) + ", " + 
					df2.format(harmonyDifference) + ", " + 
					df2.format(bothHarmonyOnset) + ", " + 
					df2.format(bothHarmonyNotOnset) + ", " + 
					df2.format(oneHarmonyOnsetOneNot) + ", " + 
					df2.format(haveSameMeasureOffset) + ", " + 
					df2.format(haveDifferentMeasureOffset) + ", " + 
					df2.format(measureOffsetDifference);
		}
		
		public HarmonicAlignmentParameterization(GeneralizedGlobalStructureAlignmentParameterization p1g, GeneralizedGlobalStructureAlignmentParameterization p2g) {
			super(p1g, p2g);
			
			HarmonicAlignmentParameterization p1 = (HarmonicAlignmentParameterization) p1g;
			HarmonicAlignmentParameterization p2 = (HarmonicAlignmentParameterization) p2g;
			
			this.harmonyEqual = (rand.nextBoolean()?p1.harmonyEqual:p2.harmonyEqual);
			this.harmonyUnequal = (rand.nextBoolean()?p1.harmonyUnequal:p2.harmonyUnequal);
			this.harmonyDifference = (rand.nextBoolean()?p1.harmonyDifference:p2.harmonyDifference);
			this.bothHarmonyOnset = (rand.nextBoolean()?p1.bothHarmonyOnset:p2.bothHarmonyOnset);
			this.bothHarmonyNotOnset = (rand.nextBoolean()?p1.bothHarmonyNotOnset:p2.bothHarmonyNotOnset);
			this.oneHarmonyOnsetOneNot = (rand.nextBoolean()?p1.oneHarmonyOnsetOneNot:p2.oneHarmonyOnsetOneNot);
			this.haveSameMeasureOffset = (rand.nextBoolean()?p1.haveSameMeasureOffset:p2.haveSameMeasureOffset);
			this.haveDifferentMeasureOffset = (rand.nextBoolean()?p1.haveDifferentMeasureOffset:p2.haveDifferentMeasureOffset);
			this.measureOffsetDifference = (rand.nextBoolean()?p1.measureOffsetDifference:p2.measureOffsetDifference);
		}

		public void mutateSubclassParameters() {
			if (rand.nextDouble() < MUTATION_RATE)
				this.harmonyEqual += (rand.nextBoolean()?1:-1) * rand.nextInt(MAX_MUTATION_STEP); 
			if (rand.nextDouble() < MUTATION_RATE)
				this.harmonyUnequal += (rand.nextBoolean()?1:-1) * rand.nextInt(MAX_MUTATION_STEP); 
			if (rand.nextDouble() < MUTATION_RATE)
				this.harmonyDifference += (rand.nextBoolean()?1:-1) * rand.nextInt(MAX_MUTATION_STEP);
			if (rand.nextDouble() < MUTATION_RATE)
				this.bothHarmonyOnset = (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
			if (rand.nextDouble() < MUTATION_RATE)
				this.bothHarmonyNotOnset = (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
			if (rand.nextDouble() < MUTATION_RATE)
				this.oneHarmonyOnsetOneNot = (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
			if (rand.nextDouble() < MUTATION_RATE)
				this.haveSameMeasureOffset = (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
			if (rand.nextDouble() < MUTATION_RATE)
				this.haveDifferentMeasureOffset = (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
			if (rand.nextDouble() < MUTATION_RATE)
				this.measureOffsetDifference = (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
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
			
			double diff = 1/mXML1Harmony.fractionOfSimilarToTotalNotes(mXML2Harmony);
			if (diff == 0.) {
				score += harmonyEqual;
			} else {
				score += harmonyUnequal;
				score += harmonyDifference * diff;
			}
			
			//onset value
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
			
			//offset value
			double offsetDifference = Math.abs(musicXML1AlignmentEvent.beat - musicXML2AlignmentEvent.beat);
			if (offsetDifference == 0.0) {
				score += haveSameMeasureOffset;
			} else {
				score += haveDifferentMeasureOffset;
				score += measureOffsetDifference * offsetDifference;
			}
					
			return score;
		}
	}
	
	public static class RhythmAlignmentParameterization extends GeneralizedGlobalStructureAlignmentParameterization {
		
		public double noteDurationEqual;
		public double noteDurationUnequal;
		public double noteDurationDifference;
		
		public double bothNoteOnset;
		public double bothNoteNotOnset;
		public double oneNoteOnsetOneNot;

		public double bothNotesNotRest;
		public double bothNotesRest;
		public double oneNoteRestOneNot;

		// measure offset match score
		public double haveSameMeasureOffset;
		public double haveDifferentMeasureOffset;
		public double measureOffsetDifference;

		public RhythmAlignmentParameterization() {
			noteDurationEqual = rand.nextInt(7)-3;
			noteDurationUnequal = rand.nextInt(7)-3;
			noteDurationDifference = rand.nextInt(7)-3;
			bothNoteOnset = rand.nextInt(7)-3;
			bothNoteNotOnset = rand.nextInt(7)-3;
			oneNoteOnsetOneNot = rand.nextInt(7)-3;
			bothNotesNotRest = rand.nextInt(7)-3;
			bothNotesRest = rand.nextInt(7)-3;
			oneNoteRestOneNot = rand.nextInt(7)-3;
			haveSameMeasureOffset = rand.nextInt(7)-3;
			haveDifferentMeasureOffset = rand.nextInt(7)-3;
			measureOffsetDifference = rand.nextInt(7)-3;
		}

		public RhythmAlignmentParameterization(String[] nextTokens) {
			super(nextTokens);
			
			int i = 4;
			this.noteDurationEqual = Double.parseDouble(nextTokens[i++]);
			this.noteDurationUnequal = Double.parseDouble(nextTokens[i++]);
			this.noteDurationDifference = Double.parseDouble(nextTokens[i++]);
			this.bothNoteOnset = Double.parseDouble(nextTokens[i++]);
			this.bothNoteNotOnset = Double.parseDouble(nextTokens[i++]);
			this.oneNoteOnsetOneNot = Double.parseDouble(nextTokens[i++]);
			this.bothNotesNotRest = Double.parseDouble(nextTokens[i++]);
			this.bothNotesRest = Double.parseDouble(nextTokens[i++]);
			this.oneNoteRestOneNot = Double.parseDouble(nextTokens[i++]);
			this.haveSameMeasureOffset = Double.parseDouble(nextTokens[i++]);
			this.haveDifferentMeasureOffset = Double.parseDouble(nextTokens[i++]);
			this.measureOffsetDifference = Double.parseDouble(nextTokens[i++]);
		}

		@Override
		public String toString() {
			return 
					super.toString() + ", " +
					df2.format(noteDurationEqual) + ", " + 
					df2.format(noteDurationUnequal) + ", " + 
					df2.format(noteDurationDifference) + ", " + 
					df2.format(bothNoteOnset) + ", " + 
					df2.format(bothNoteNotOnset) + ", " + 
					df2.format(oneNoteOnsetOneNot) + ", " + 
					df2.format(bothNotesNotRest) + ", " + 
					df2.format(bothNotesRest) + ", " + 
					df2.format(oneNoteRestOneNot) + ", " + 
					df2.format(haveSameMeasureOffset) + ", " + 
					df2.format(haveDifferentMeasureOffset) + ", " + 
					df2.format(measureOffsetDifference);
		}
		
		public RhythmAlignmentParameterization(GeneralizedGlobalStructureAlignmentParameterization p1g, GeneralizedGlobalStructureAlignmentParameterization p2g) {
			super(p1g, p2g);
			
			RhythmAlignmentParameterization p1 = (RhythmAlignmentParameterization) p1g;
			RhythmAlignmentParameterization p2 = (RhythmAlignmentParameterization) p2g;
			
			this.noteDurationEqual = (rand.nextBoolean()?p1.noteDurationEqual:p2.noteDurationEqual);
			this.noteDurationUnequal = (rand.nextBoolean()?p1.noteDurationUnequal:p2.noteDurationUnequal);
			this.noteDurationDifference = (rand.nextBoolean()?p1.noteDurationDifference:p2.noteDurationDifference);
			this.bothNoteOnset = (rand.nextBoolean()?p1.bothNoteOnset:p2.bothNoteOnset);
			this.bothNoteNotOnset = (rand.nextBoolean()?p1.bothNoteNotOnset:p2.bothNoteNotOnset);
			this.oneNoteOnsetOneNot = (rand.nextBoolean()?p1.oneNoteOnsetOneNot:p2.oneNoteOnsetOneNot);
			this.bothNotesNotRest = (rand.nextBoolean()?p1.bothNotesNotRest:p2.bothNotesNotRest);
			this.bothNotesRest = (rand.nextBoolean()?p1.bothNotesRest:p2.bothNotesRest);
			this.oneNoteRestOneNot = (rand.nextBoolean()?p1.oneNoteRestOneNot:p2.oneNoteRestOneNot);
			this.haveSameMeasureOffset = (rand.nextBoolean()?p1.haveSameMeasureOffset:p2.haveSameMeasureOffset);
			this.haveDifferentMeasureOffset = (rand.nextBoolean()?p1.haveDifferentMeasureOffset:p2.haveDifferentMeasureOffset);
			this.measureOffsetDifference = (rand.nextBoolean()?p1.measureOffsetDifference:p2.measureOffsetDifference);
		}

		public void mutateSubclassParameters() {
			if (rand.nextDouble() < MUTATION_RATE)
				this.noteDurationEqual += (rand.nextBoolean()?1:-1) * rand.nextInt(MAX_MUTATION_STEP); 
			if (rand.nextDouble() < MUTATION_RATE)
				this.noteDurationUnequal += (rand.nextBoolean()?1:-1) * rand.nextInt(MAX_MUTATION_STEP); 
			if (rand.nextDouble() < MUTATION_RATE)
				this.noteDurationDifference += (rand.nextBoolean()?1:-1) * rand.nextInt(MAX_MUTATION_STEP);
			if (rand.nextDouble() < MUTATION_RATE)
				this.bothNoteOnset = (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
			if (rand.nextDouble() < MUTATION_RATE)
				this.bothNoteNotOnset = (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
			if (rand.nextDouble() < MUTATION_RATE)
				this.oneNoteOnsetOneNot = (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
			if (rand.nextDouble() < MUTATION_RATE)
				this.bothNotesNotRest = (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
			if (rand.nextDouble() < MUTATION_RATE)
				this.bothNotesRest = (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
			if (rand.nextDouble() < MUTATION_RATE)
				this.oneNoteRestOneNot = (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
			if (rand.nextDouble() < MUTATION_RATE)
				this.haveSameMeasureOffset = (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
			if (rand.nextDouble() < MUTATION_RATE)
				this.haveDifferentMeasureOffset = (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
			if (rand.nextDouble() < MUTATION_RATE)
				this.measureOffsetDifference = (rand.nextDouble()-0.5) * (MAX_MUTATION_STEP*2); 
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
					score += bothNoteOnset;
				else 
					score += oneNoteOnsetOneNot;
			} else {
				if (musicXML2AlignmentEvent.noteOnset)
					score += oneNoteOnsetOneNot;
				else 
					score += bothNoteNotOnset;
			}
			
			//offset value
			double offsetDifference = Math.abs(musicXML1AlignmentEvent.beat - musicXML2AlignmentEvent.beat);
			if (offsetDifference == 0.0) {
				score += haveSameMeasureOffset;
			} else {
				score += haveDifferentMeasureOffset;
				score += measureOffsetDifference * offsetDifference;
			}

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
//			WikifoniaCorrection.applyManualCorrections(musicXMLParser, file.getName());
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
//			if (!file.getName().startsWith("Micha")){
//				List<Integer> playedToAbsoluteMeasureNumberMap = musicXML.playedToAbsoluteMeasureNumberMap;
//				for (int i = 0; i < playedToAbsoluteMeasureNumberMap.size(); i++) {
//					System.out.println("Played measure " + (i+1) + " corresponds to absolute measure " + (playedToAbsoluteMeasureNumberMap.get(i) + 1));
//				}
//			}
			trainingSongs.add(musicXML);
		}
	}

	private final static String TYPE = "rhythm"; // if you change this, you will need to implement how accuracy is calculated
	private static Class parameterizationClass = null;
	static {
		if (TYPE.equals("lyric"))
			parameterizationClass = LyricAlignmentParameterization.class;
		else if (TYPE.equals("pitch"))
			parameterizationClass = PitchAlignmentParameterization.class;
		if (TYPE.equals("harmony"))
			parameterizationClass = HarmonicAlignmentParameterization.class;
		if (TYPE.equals("rhythm"))
			parameterizationClass = RhythmAlignmentParameterization.class;
	}
	private final static String POPULATION_FILE = "generalized_global_alignment_inference/parameterization_pop_"+ TYPE +".txt";
	private final static String HEATMAP_FILE_PREFIX = "generalized_global_alignment_inference/"+ TYPE +"_visualizations/";
	private final static int TOTAL_GENERATIONS = 10000;
	private static double prevBestAccuracy = 0.0;
	public static void main(String[] args) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		// create/load initial population of x parameterizations and their accuracy scores when used to
		List<Pair<Double, GeneralizedGlobalStructureAlignmentParameterization>> population = loadInitialPopulation(TYPE);
		System.out.println("Previous Best Accuracy: " + prevBestAccuracy);
		
		for (int i = 0; i < TOTAL_GENERATIONS && prevBestAccuracy != 1.0; i++) {
			generation++;
			// cross-over and mutate the scores, possible modifying just one score at a time?
			List<GeneralizedGlobalStructureAlignmentParameterization> offSpring = generateNewPopulation(population);
			
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
			population = population.subList(0, populationSize);
			final Pair<Double, GeneralizedGlobalStructureAlignmentParameterization> best = population.get(0);
			if (best.getFirst() > prevBestAccuracy) {
				prevBestAccuracy = best.getFirst();
				scoreParameterization(best.getSecond(), TYPE, true); // save best heatmap
				System.out.println(i + "\t" + prevBestAccuracy);
			}
//			System.out.println(i + "\t" + prevBestAccuracy);

			// print top y parameterizations
			savePopulationToFile(population);
		}
		
		// when parameterizations have settled, test on a song not used in training
		
	}
	
	private static final int LITTER_SIZE = 10;
	private static List<GeneralizedGlobalStructureAlignmentParameterization> generateNewPopulation(
			List<Pair<Double, GeneralizedGlobalStructureAlignmentParameterization>> population) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		List<GeneralizedGlobalStructureAlignmentParameterization> newPop = new ArrayList<GeneralizedGlobalStructureAlignmentParameterization>();
		
		for (int i = 0; i < LITTER_SIZE; i++) {
			// selection
			int parentIdx = rand.nextInt(population.size());
			GeneralizedGlobalStructureAlignmentParameterization parent1 = population.get(parentIdx).getSecond();
			parentIdx = rand.nextInt(population.size());
			GeneralizedGlobalStructureAlignmentParameterization parent2 = population.get(parentIdx).getSecond();
			
			// crossover
			GeneralizedGlobalStructureAlignmentParameterization newOffspring = (GeneralizedGlobalStructureAlignmentParameterization) parameterizationClass.getDeclaredConstructor(GeneralizedGlobalStructureAlignmentParameterization.class, GeneralizedGlobalStructureAlignmentParameterization.class).newInstance(parent1,parent2);
			// mutation
			newOffspring.mutate();
			
			newPop.add(newOffspring);
		}
		
		return newPop;
	}

	static Map<String, Integer> solutionIDMap = new HashMap<String, Integer>();
	
	private static void savePopulationToFile(List<Pair<Double, GeneralizedGlobalStructureAlignmentParameterization>> population) {
		try(FileWriter fw = new FileWriter(POPULATION_FILE, true);
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

	private static List<Pair<Double, GeneralizedGlobalStructureAlignmentParameterization>> scoreParameterizations(List<GeneralizedGlobalStructureAlignmentParameterization> offSpring, String targetSegment) {
		List<Pair<Double, GeneralizedGlobalStructureAlignmentParameterization>> offSpringPopulation = new ArrayList<Pair<Double, GeneralizedGlobalStructureAlignmentParameterization>>();
		boolean first = true;
		for (GeneralizedGlobalStructureAlignmentParameterization globalStructureAlignmentParameterization : offSpring) {
			double score = scoreParameterization(globalStructureAlignmentParameterization, targetSegment, false);
			first = false;
			offSpringPopulation.add(new Pair<>(score, globalStructureAlignmentParameterization));
		}
		return offSpringPopulation;
	}
	
	private static double scoreParameterization(GeneralizedGlobalStructureAlignmentParameterization globalStructureAlignmentParameterization, String targetSegment, boolean saveHeatmap) {
		double correct = 0.0;
		double total = 0.0;
		
		if (DEBUG > 0) System.out.println("Scoring parameterization");
		for (ParsedMusicXMLObject song : trainingSongs) {
			if (DEBUG > 0) System.out.println(song.filename);
			// do an alignment for each song in the training set using the parameterization
			Object[] matrices = align(song, globalStructureAlignmentParameterization);
			double[][] alnMatrix = (double[][]) matrices[0];
			char[][] ptrMatrix = (char[][]) matrices[1];
//			Utils.normalizeByMaxVal(alnMatrix);
			
			// use the alignment to infer the locations of the target segment type
			Object[] inferredSegments = inferTargetSegmentLocations(alnMatrix, ptrMatrix, globalStructureAlignmentParameterization);
			List<Set<Integer>> inferredLocationStarts = (List<Set<Integer>>) inferredSegments[0];
			double[][] pathsTaken = (double[][]) inferredSegments[1];
			Utils.normalizeByMaxVal(pathsTaken);
			
			// Given the inferred locations of the target segment type and the actual global structure, compute the accuracy
			final double computedAccuracy = computeAccuracy(song, inferredLocationStarts, song.getAlignmentEvents(globalStructureAlignmentParameterization.eventsPerBeat), targetSegment);
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
		List<Set<Integer>> inferredLocations = new ArrayList<Set<Integer>>(Collections.nCopies(alnMatrix.length-1, null));
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
		
		int maxRow,maxCol;
		double maxVal = localMaxima.isEmpty()?-1.0:localMaxima.get(0).getFirst()*1.1;
		for (Triple<Double, Integer, Integer> triple : localMaxima) {
			// Backtrack
			maxRow = triple.getSecond();
			maxCol = triple.getThird();
			row = maxRow;
			col = maxCol;
			Set<Integer> inferredLocationForPos; 
			while (alnMatrix[row][col] > 0.) {
				inferredLocationForPos = inferredLocations.get(row-1);
				if (inferredLocationForPos == null) {
					inferredLocationForPos = new HashSet<Integer>();
					inferredLocations.set(row-1, inferredLocationForPos);
				}
				inferredLocationForPos.add(col-1);
				inferredLocationForPos = inferredLocations.get(col-1);
				if (inferredLocationForPos == null) {
					inferredLocationForPos = new HashSet<Integer>();
					inferredLocations.set(col-1, inferredLocationForPos);
				}
				inferredLocationForPos.add(row-1);
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

	//attaches β times as much importance to recall as precision	
	private static double fScoreBetaValueSquared = 1.0;
	/**
	 * Given the inferred locations of the target segment type and the actual global structure, compute the accuracy 
	 * @param song 
	 * @param targetSegment 
	 */
	private static double computeAccuracy(ParsedMusicXMLObject song, List<Set<Integer>> inferredMatchLocations,
			List<ParsedMusicXMLObject.MusicXMLAlignmentEvent> songEvents, String type) {

		Set<Integer> inferredMatchesForRowPosition;
		int truePositive = 0;
		int falsePositive = 0;
		int falseNegative = 0;
		Set<String> actualGroups;
		MusicXMLAlignmentEvent currentSongEvent, inferredSongEvent;

		Map<Character, List<Pair<Pair<Integer, Double>, Pair<Integer, Double>>>> allMatchingGroups = getMatchingGroups(song, type);
		Set<String> inferredGroups;
		
		Set<String> matchingRegions;
		for (int i = 0; i < songEvents.size(); i++) {
			inferredMatchesForRowPosition = inferredMatchLocations.get(i);
			currentSongEvent = songEvents.get(i);
			actualGroups = getGroups(type, currentSongEvent);
			
			if (actualGroups.isEmpty()) { // didn't belong to any groups, so shouldn't have any matches
				if (inferredMatchesForRowPosition == null) { // didn't find any matches
//					trueNegative++; // didn't infer match, was no match
				} else { // had matches (wrong)
					falsePositive += inferredMatchesForRowPosition.size();
				}
				// inferred matches, but was no match => incorrect
			} else {
				// what should it have inferred?
				matchingRegions = new HashSet<String>();
				for (String actualGroup: actualGroups) { // for each actual group (and group ID) for this event
					Character group = actualGroup.charAt(0); // abstract to the group label
					final List<Pair<Pair<Integer, Double>, Pair<Integer, Double>>> allGroupsForLabel = allMatchingGroups.get(group); // find all group IDs with this label
					for (int j = 1; j <= allGroupsForLabel.size(); j++) { // for each of the group IDs with this label
						String groupID = "" + group + j;
						if (!actualGroups.contains(groupID)) { // if the ID doesn't already belong to this event
							matchingRegions.add(groupID); // then this even should match to that ID
						}
					}
				}
				
				if (inferredMatchesForRowPosition == null) {
					falseNegative += matchingRegions.size(); // it didn't find any of the matching regions
				} else {
//					correctInferredPositionMatchingGroups = new HashSet<String>();
					for (Integer inferredPosition : inferredMatchesForRowPosition) { // for each matching position
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
		
		return ((1+fScoreBetaValueSquared) * truePositive) / ((1+fScoreBetaValueSquared) * truePositive + fScoreBetaValueSquared * falseNegative + falsePositive);
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
		} else {
			throw new RuntimeException("Not yet implemented");
		}
	}

	private static Map<Character, List<Pair<Pair<Integer, Double>, Pair<Integer, Double>>>> getMatchingGroups(
			ParsedMusicXMLObject song, String type) {
		if (type.equals("lyric")) {
			return song.getAllMatchingLyricGroups();
		} else if (type.equals("pitch")){
			return song.getAllMatchingPitchGroups();
		} else if (type.equals("harmony")){
			return song.getAllMatchingHarmonyGroups();
		} else if (type.equals("rhythm")){
			return song.getAllMatchingRhythmGroups();
		} else {
			throw new RuntimeException("Not yet implemented");
		}
	}

	/**
	 * Given a solution alignment matrix, a score, and the generation, output a heatmap representation of the matrix
	 * @param song 
	 */
	private static void saveHeatmap(int generation, double computedAccuracy, double[][] pathsTaken,
			SortedMap<Integer, SortedMap<Double, SegmentType>> globalStructureBySegmentTokenStart,
			String targetSegment,
			GeneralizedGlobalStructureAlignmentParameterization globalStructureAlignmentParameterization, ParsedMusicXMLObject song) {
		
		List<ParsedMusicXMLObject.MusicXMLAlignmentEvent> alignmentEvents = song.getAlignmentEvents(globalStructureAlignmentParameterization.eventsPerBeat);
		
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
		chart.setLowValueColour(Color.BLUE);
		
		chart.setYAxisLabel(song.filename);
		chart.setXAxisLabel(song.filename);
		chart.setTitle("Gen:" + generation + " Targ:" + targetSegment + " F-1: " + df3.format(computedAccuracy) + " Params:" + globalStructureAlignmentParameterization.toString());
		chart.setXValues(xValues);
		chart.setYValues(yValues);
		chart.setTitleFont(new Font("Times", Font.BOLD, (int) (chartXDimension/3.0)));
		chart.setAxisLabelsFont(new Font("Times", Font.BOLD, (int) (chartXDimension/3.0)));
		chart.setAxisValuesFont(new Font("Times", Font.BOLD, (int) (chartXDimension/3.2)));
//		chart.setCellSize(new Dimension(5,5));
		
		try {
			final String pathname = HEATMAP_FILE_PREFIX + song.filename +"_gen" + generation + "_id" + globalStructureAlignmentParameterization.getSolutionID() + ".jpeg";
			chart.saveToFile(new File(pathname));
		} catch (IOException e) {
			e.printStackTrace();
		}		
		
	}

	private static List<Pair<Double, GeneralizedGlobalStructureAlignmentParameterization>> loadInitialPopulation(String targetSegment) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		List<Pair<Double, GeneralizedGlobalStructureAlignmentParameterization>> population = new ArrayList<Pair<Double, GeneralizedGlobalStructureAlignmentParameterization>>();
		try {
			Scanner fileScanner = new Scanner(new File(POPULATION_FILE));
			System.out.println("Loading initial population from file");
			while (fileScanner.hasNextLine()) {
				generation = Integer.parseInt(fileScanner.nextLine().split(" ")[1]);
				population = new ArrayList<Pair<Double, GeneralizedGlobalStructureAlignmentParameterization>>();
				while (fileScanner.hasNextLine()){
					String nextLine = fileScanner.nextLine();
					if (nextLine.startsWith("*****")) {
						fileScanner.nextLine();
						break;
					}
					String[] nextTokens = nextLine.split("\\t",3);
					final GeneralizedGlobalStructureAlignmentParameterization globalStructureAlignmentParameterization = (GeneralizedGlobalStructureAlignmentParameterization) parameterizationClass.getDeclaredConstructor(String[].class).newInstance(new Object[] {nextTokens[2].split(", ")});
					int solutionID = Integer.parseInt(nextTokens[0]);
					Integer existingSolutionID = solutionIDMap.get(globalStructureAlignmentParameterization.toString());
					
					if (existingSolutionID == null) {
						solutionIDMap.put(globalStructureAlignmentParameterization.toString(), solutionID);
					} else {
						assert(existingSolutionID == solutionID);
					}
					
					if (population.size() < populationSize) {
						double score = Double.parseDouble(nextTokens[1]);
						population.add(new Pair<Double, GeneralizedGlobalStructureAlignmentParameterization>(score,globalStructureAlignmentParameterization));
					}
				}
			}
			fileScanner.close();
		} catch (FileNotFoundException e) {
			System.out.println("Initializing population of parameterizations");
			// need to create initial population!
			for (int i = 0; i < populationSize; i++) {
				final GeneralizedGlobalStructureAlignmentParameterization globalStructureAlignmentParameterization = (GeneralizedGlobalStructureAlignmentParameterization) parameterizationClass.newInstance();
				System.out.println("\tScoring initial parameterization:" + globalStructureAlignmentParameterization.toString());
				final double score = scoreParameterization(globalStructureAlignmentParameterization, targetSegment, false);
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
			scoreParameterization(initBest.getSecond(), targetSegment, true); // save initial heatmap

			savePopulationToFile(population);
		}
		prevBestAccuracy = population.get(0).getFirst();

		return population;
	}

}
