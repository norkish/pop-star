package tabcomplete.alignment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import data.MusicXMLParser.Harmony;
import data.MusicXMLParser.Note;
import data.MusicXMLParser.NoteLyric;
import data.ParsedMusicXMLObject;
import globalstructureinference.GlobalStructureInferer.GlobalStructureAlignmentParameterization;

public class MusicXMLPair extends SequencePair {

	public class MusicXMLPairAlignmentBuilder extends AlignmentBuilder {

		List<Integer> firstBldr = new ArrayList<Integer>();
		List<Integer> secondBldr = new ArrayList<Integer>();
		
		@Override
		public void appendCharSequence1(int i) {
			firstBldr.add(i);

		}

		@Override
		public void appendCharSequence2(int j) {
			secondBldr.add(j);
		}

		@Override
		public void appendIndelSequence1() {
			firstBldr.add(null);
		}

		@Override
		public void appendIndelSequence2() {
			secondBldr.add(null);
		}

		@Override
		public Alignment renderAlignment() {
			int[] firstArray = new int[firstBldr.size()];
			for (int i = 0; i < firstArray.length; i++) {
				Integer firstMsr = firstBldr.get(i);
				firstArray[i] = firstMsr == null ? -1 : firstMsr;
			}
			int[] secondArray = new int[secondBldr.size()];
			for (int i = 0; i < secondArray.length; i++) {
				Integer secondMsr = secondBldr.get(i);
				secondArray[i] = secondMsr == null ? -1 : secondMsr;
			}
			
			return new MusicXMLPairAlignment(firstArray, secondArray, scores);
		}
		
		@Override
		public void reverse() {
			Collections.reverse(firstBldr);
			Collections.reverse(secondBldr);
			super.reverse();
		}
	}

	private List<ParsedMusicXMLObject.MusicXMLAlignmentEvent> musicXML1Events, musicXML2Events;
	
	private GlobalStructureAlignmentParameterization scoringValues;
	private int seq1Length, seq2Length;
	
	public MusicXMLPair(ParsedMusicXMLObject musicXML1, ParsedMusicXMLObject musicXML2, 
			GlobalStructureAlignmentParameterization globalStructureAlignmentParameterization) {
		PERCEPTRON_ERROR_TOTAL = 0.0;
		PERCEPTRON_ERROR_COUNT = 0;
		
		System.out.println("Aligning mxl: " + musicXML1.filename + " and " + musicXML2.filename + " at " + globalStructureAlignmentParameterization.eventsPerBeat + " events/beat intervals");
		
		this.musicXML1Events = musicXML1.getAlignmentEvents(globalStructureAlignmentParameterization.eventsPerBeat);
		this.musicXML2Events = musicXML2.getAlignmentEvents(globalStructureAlignmentParameterization.eventsPerBeat);
		
		this.scoringValues = globalStructureAlignmentParameterization;
		seq1Length = (int) (musicXML1.getDurationInBeats() * globalStructureAlignmentParameterization.eventsPerBeat);
		seq2Length = (int) (musicXML2.getDurationInBeats() * globalStructureAlignmentParameterization.eventsPerBeat);
	}

	@Override
	public AlignmentBuilder newAlignmentBuilder() {
		return new MusicXMLPairAlignmentBuilder();
	}

	@Override
	/**
	 * Return the score of aligning the elements in measure number mXML1MsrNo in musicXML1 with the elements 
	 * in mXML2MsrNo in musicXML2, using the various element weights given in the constructor. 
	 */
	public double matchScore(int mXML1EventNo, int mXML2EventNo) {
		//TODO
		
//		System.out.println("Computing match score for mXML1EventNo " + mXML1EventNo + " and mXML2EventNo " + mXML2EventNo);
		
		ParsedMusicXMLObject.MusicXMLAlignmentEvent musicXML1AlignmentEvent = musicXML1Events.get(mXML1EventNo);
		ParsedMusicXMLObject.MusicXMLAlignmentEvent musicXML2AlignmentEvent = musicXML2Events.get(mXML2EventNo);
		
//		Pair<Integer, Double> mXML1MeasureBeat = musicXML1.getMeasureAndBeatForEvent(mXML1EventNo, scoringValues.eventsPerBeat);
//		final Integer mXML1Measure = mXML1MeasureBeat.getFirst();
//		final Double mXML1Beat = mXML1MeasureBeat.getSecond();
//		assert(mXML1Beat == musicXML1AlignmentEvent.beat);
//		
//		Pair<Integer, Double> mXML2MeasureBeat = musicXML2.getMeasureAndBeatForEvent(mXML2EventNo, scoringValues.eventsPerBeat);
//		final Integer mXML2Measure = mXML2MeasureBeat.getFirst();
//		final Double mXML2Beat = mXML2MeasureBeat.getSecond();
//		assert(mXML2Beat == musicXML2AlignmentEvent.beat);

		Note mXML1Note = musicXML1AlignmentEvent.note;
		Note mXML2Note = musicXML2AlignmentEvent.note;
		
		int mXML1Pitch = mXML1Note.pitch;
		boolean mXML1PitchOnset = musicXML1AlignmentEvent.noteOnset;
		int mXML2Pitch = mXML2Note.pitch;
		boolean mXML2PitchOnset = musicXML2AlignmentEvent.noteOnset;
		
		Harmony mXML1Harmony = musicXML1AlignmentEvent.harmony;
		boolean mXML1HarmonyOnset = musicXML1AlignmentEvent.harmonyOnset;
		Harmony mXML2Harmony =  musicXML2AlignmentEvent.harmony;
		boolean mXML2HarmonyOnset = musicXML2AlignmentEvent.harmonyOnset;
		
		NoteLyric mXML1Lyric =  musicXML1AlignmentEvent.lyric;
		boolean mXML1LyricOnset =  musicXML1AlignmentEvent.lyricOnset;
		NoteLyric mXML2Lyric = musicXML2AlignmentEvent.lyric;
		boolean mXML2LyricOnset = musicXML2AlignmentEvent.lyricOnset;

//		System.out.println("\tThis corresponds to m " + mXML1Measure + ", b " + mXML1Beat + ":");
//		System.out.println("\t\tpitch:" + Pitch.getPitchName((mXML1Pitch+3)%12) + (mXML1PitchOnset?" ONSET":""));
//		System.out.println("\t\tharmony:" + mXML1Harmony + (mXML1HarmonyOnset?" ONSET":""));
//		System.out.println("\t\tlyric:" + mXML1Lyric + (mXML1LyricOnset?" ONSET":""));
//		System.out.println("\tand m " + mXML2Measure + ", b " + mXML2Beat + ":");
//		System.out.println("\t\tpitch:" + Pitch.getPitchName((mXML2Pitch+3)%12) + (mXML2PitchOnset?" ONSET":""));
//		System.out.println("\t\tharmony:" + mXML2Harmony + (mXML2HarmonyOnset?" ONSET":""));
//		System.out.println("\t\tlyric:" + mXML2Lyric + (mXML2LyricOnset?" ONSET":""));
		
		
		//features
		int pitchesEqual = (mXML1Pitch == mXML2Pitch ? 1 : 0);
		int bothRests = (mXML1Pitch == Note.REST && mXML2Pitch == Note.REST? 1 : 0);
		int oneRests = ((mXML1Pitch != mXML2Pitch && (mXML1Pitch == Note.REST || mXML2Pitch == Note.REST))? 1 : 0);
		int neitherRests = ((mXML1Pitch != Note.REST && mXML2Pitch != Note.REST)? 1 : 0);
		int bothPitchesOnset = (mXML1PitchOnset && mXML2PitchOnset ? 1 : 0);
		int bothPitchesNotOnset = (!mXML1PitchOnset && !mXML2PitchOnset ? 1 : 0);
		int onePitchOnsetOneNot = (mXML1PitchOnset != mXML2PitchOnset ? 1 : 0);
		int pitchDifference = Math.abs(mXML1Pitch - mXML2Pitch);
		
		int harmonyEqual = (mXML1Harmony.equals(mXML2Harmony) ? 1 : 0);
		int bothHarmoniesOnset = (mXML1HarmonyOnset && mXML2HarmonyOnset ? 1 : 0);
		int bothHarmoniesNotOnset = (!mXML1HarmonyOnset && !mXML2HarmonyOnset ? 1 : 0);
		int oneHarmonyOnsetOneNot = (mXML1HarmonyOnset != mXML2HarmonyOnset ? 1 : 0);
		int harmonyDifference;
		
		int lyricsEqual = ((mXML1Lyric == null && mXML2Lyric == null) || (mXML1Lyric != null && mXML1Lyric.equals(mXML2Lyric)) ? 1 : 0);
		int bothLyricsOnset = (mXML1LyricOnset && mXML2LyricOnset ? 1 : 0);
		int bothLyricsNotOnset = (!mXML1LyricOnset && !mXML2LyricOnset ? 1 : 0);
		int oneLyricOnsetOneNot = (mXML1LyricOnset != mXML2LyricOnset ? 1 : 0);
		
		// calculate output
		double y = 0.;
//		y = pitchesEqual * scoringValues.pitchesEqual * PERCEPTRON_LEARNING_RATE;
//		y += bothRests * scoringValues.bothRests * PERCEPTRON_LEARNING_RATE;
//		y += oneRests * scoringValues.oneRests * PERCEPTRON_LEARNING_RATE;
//		y += neitherRests * scoringValues.neitherRests * PERCEPTRON_LEARNING_RATE;
//		y += bothPitchesOnset * scoringValues.bothPitchesOnset * PERCEPTRON_LEARNING_RATE;
//		y += bothPitchesNotOnset * scoringValues.bothPitchesNotOnset * PERCEPTRON_LEARNING_RATE;
//		y += onePitchOnsetOneNot * scoringValues.onePitchOnsetOneNot * PERCEPTRON_LEARNING_RATE;
//		y += pitchDifference * scoringValues.pitchDifference * PERCEPTRON_LEARNING_RATE;
//		y += harmonyEqual * scoringValues.harmonyEqual * PERCEPTRON_LEARNING_RATE;
//		y += bothHarmoniesOnset * scoringValues.bothHarmoniesNotOnset * PERCEPTRON_LEARNING_RATE;
//		y += bothHarmoniesNotOnset * scoringValues.bothHarmoniesNotOnset * PERCEPTRON_LEARNING_RATE;
//		y += oneHarmonyOnsetOneNot * scoringValues.oneHarmonyOnsetOneNot * PERCEPTRON_LEARNING_RATE;
		y += lyricsEqual * scoringValues.lyricsEqual;
//		y += bothLyricsOnset * scoringValues.bothLyricsOnset * PERCEPTRON_LEARNING_RATE;
//		y += bothLyricsNotOnset * scoringValues.bothLyricsNotOnset * PERCEPTRON_LEARNING_RATE;
//		y += oneLyricOnsetOneNot * scoringValues.oneLyricOnsetOneNot * PERCEPTRON_LEARNING_RATE;
		
		return y;
	}
	
	public static double PERCEPTRON_ERROR_TOTAL = -1.0;
	public static int PERCEPTRON_ERROR_COUNT = 0;
	
	@Override
	public int seq1length() {
		return seq1Length;
	}

	@Override
	public int seq2length() {
		return seq2Length;
	}
	
	@Override
	public boolean saveMatrix() {
		System.out.println("STORING MATRIX");
		return true;
	}
}
