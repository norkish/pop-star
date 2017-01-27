package tabcomplete.alignment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import data.MusicXMLParser.Harmony;
import data.MusicXMLParser.Note;
import data.MusicXMLParser.NoteLyric;
import data.MusicXMLParser.Time;
import data.ParsedMusicXMLObject;
import utils.Triple;
import utils.Utils;

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

	private SortedMap<Integer, SortedMap<Integer, Note>> notesByMeasure1;
	private SortedMap<Integer, SortedMap<Integer, Note>> notesByMeasure2;
	private double identityWeight, lyricWeight, harmonyWeight, melodyWeight;
	private SortedMap<Integer, SortedMap<Integer, Harmony>> harmonyByMeasure1;
	private SortedMap<Integer, SortedMap<Integer, Harmony>> harmonyByMeasure2;
	private ParsedMusicXMLObject musicXML1;
	private ParsedMusicXMLObject musicXML2;
	
	public MusicXMLPair(ParsedMusicXMLObject musicXML, ParsedMusicXMLObject musicXML2, 
			double identityWeight, double lyricWeight, double harmonyWeight, double melodyWeight) {
		
		this.notesByMeasure1 = new TreeMap<Integer, SortedMap<Integer, Note>>();
		for (Triple<Integer,Integer,Note> triple : musicXML.notesByMeasure) {
			Integer measure = triple.getFirst();
			Integer divOffset = triple.getSecond();
			Note note = triple.getThird();
			SortedMap<Integer, Note> notesByOffset = notesByMeasure1.get(measure);
			if (notesByOffset == null) {
				notesByOffset = new TreeMap<Integer, Note>();
				notesByMeasure1.put(measure, notesByOffset);
			}
			
			// keep the highest note if there are multiple
			Note currNote = notesByOffset.get(divOffset);
			if (currNote == null || note.pitch > currNote.pitch) {
				notesByOffset.put(divOffset, note);
			}
		}

		this.notesByMeasure2 = new TreeMap<Integer, SortedMap<Integer, Note>>();
		for (Triple<Integer,Integer,Note> triple : musicXML2.notesByMeasure) {
			Integer measure = triple.getFirst();
			Integer divOffset = triple.getSecond();
			Note note = triple.getThird();
			SortedMap<Integer, Note> notesByOffset = notesByMeasure2.get(measure);
			if (notesByOffset == null) {
				notesByOffset = new TreeMap<Integer, Note>();
				notesByMeasure2.put(measure, notesByOffset);
			}
			
			// keep the highest note if there are multiple
			Note currNote = notesByOffset.get(divOffset);
			if (currNote == null || note.pitch > currNote.pitch) {
				notesByOffset.put(divOffset, note);
			}
		}
		
		this.harmonyByMeasure1 = musicXML.unoverlappingHarmonyByMeasure;
		this.harmonyByMeasure2 = musicXML2.unoverlappingHarmonyByMeasure;
		this.identityWeight = identityWeight;
		this.lyricWeight = lyricWeight;
		this.harmonyWeight = harmonyWeight;
		this.melodyWeight = melodyWeight;
		this.musicXML1 = musicXML;
		this.musicXML2 = musicXML2;
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
	public double matchScore(int mXML1MsrNo, int mXML2MsrNo) {
//		return matchScoreViaElementAlignment(mXML1MsrNo, mXML2MsrNo);
		return matchScoreViaDivComparison(mXML1MsrNo, mXML2MsrNo);
	}

	private double matchScoreViaDivComparison(int mXML1MsrNo, int mXML2MsrNo) {
		double score = 0.0;
		
		if (identityWeight == 0.0 && mXML1MsrNo == mXML2MsrNo) {
			return score;
		}
		
		SortedMap<Integer, Note> notesfor1Measure = notesByMeasure1.get(mXML1MsrNo);
		SortedMap<Integer, Note> notesfor2Measure = notesByMeasure2.get(mXML2MsrNo);
		SortedMap<Integer, Harmony> harmoniesfor1Measure = harmonyByMeasure1.get(mXML1MsrNo);
		SortedMap<Integer, Harmony> harmoniesfor2Measure = harmonyByMeasure2.get(mXML2MsrNo);

		Time time1 = Utils.valueForKeyBeforeOrEqualTo(mXML1MsrNo, musicXML1.timeByMeasure);
		int beatsIn1Msr = time1.beats;
		Time time2 = Utils.valueForKeyBeforeOrEqualTo(mXML2MsrNo, musicXML2.timeByMeasure);
		int beatsIn2Msr = time2.beats;
		int comparableBeats = Math.min(beatsIn1Msr, beatsIn2Msr);

		double divsPerBeatIn1Msr = Utils.valueForKeyBeforeOrEqualTo(mXML1MsrNo, musicXML1.divsPerQuarterByMeasure) * (4.0/time1.beatType);
		double divsPerBeatIn2Msr = Utils.valueForKeyBeforeOrEqualTo(mXML2MsrNo, musicXML2.divsPerQuarterByMeasure) * (4.0/time2.beatType);
		// max number of divs we consider is 12
		double comparableDivs = Math.min(4.0, Math.max(divsPerBeatIn1Msr, divsPerBeatIn2Msr));
		double divs1PerComparedDiv = divsPerBeatIn1Msr/comparableDivs;
		double divs2PerComparedDiv = divsPerBeatIn2Msr/comparableDivs;
		System.err.println("Comparing measures " + mXML1MsrNo + " and " + mXML2MsrNo);
//		System.err.println("divsPerBeatIn1Msr = " + divsPerBeatIn1Msr);
//		System.err.println("divsPerBeatIn2Msr = " + divsPerBeatIn2Msr);
//		System.err.println("ComparableDivs = " + comparableDivs);
		// for each comparable beat
		double divsOffset1 = 0;
		double divsOffset2 = 0;

		double lyricScore = 0.0;
		double melodyScore = 0.0;
		double harmonyScore = 0.0;
		
		
		//note that all pitches and harmonies are already normalized to the key for the measure, which we are ignoring (measures that are identical but in different keys—we count them as identical)
		for (int beat = 0; beat < comparableBeats; beat++) {
			for (int div = 0; div < comparableDivs; div++) {
				// Calculate divs offset for both measures
				if (lyricWeight != 0.0 || melodyWeight != 0.0) { 
//					System.err.println("ComparbleDiv #" + (beat * comparableDivs + div));
					Note note1 = Utils.valueForKeyBeforeOrEqualTo((int) divsOffset1,notesfor1Measure);
					Note note2 = Utils.valueForKeyBeforeOrEqualTo((int) divsOffset2,notesfor2Measure);
//					System.err.println("Note1 (divOffset " + divsOffset1 + "):" + note1);
//					System.err.println("Note2 (divOffset " + divsOffset2 + "):" + note2);
					
					// calculate the subscore for lyrics
					if (lyricWeight != 0.0) {
						NoteLyric note1Lyric = note1.lyric;
						if (note1Lyric == null && note2.lyric == null || note1Lyric !=null && note1Lyric.equals(note2.lyric)) {
							lyricScore += SequencePair.MATCH_SCORE;
						} else {
							lyricScore += SequencePair.MISMATCH_SCORE;
						}
					}
					
					// calculate the subscore for melody
					if (melodyWeight != 0.0 && note1.pitch == note2.pitch) {
						melodyScore += SequencePair.MATCH_SCORE;
					} else {
						melodyScore += SequencePair.MISMATCH_SCORE;
					}
				}
				
				// calculate the subscore for harmony
				if (harmonyWeight != 0.0) {
					Harmony harmony1, harmony2;
					
					if (harmoniesfor1Measure == null) harmony1 = Utils.valueForKeyBeforeOrEqualTo(mXML1MsrNo,(int) divsOffset1,harmonyByMeasure1);
					else harmony1 = Utils.valueForKeyBeforeOrEqualTo((int) divsOffset1,harmoniesfor1Measure);

					if (harmoniesfor2Measure == null) harmony2 = Utils.valueForKeyBeforeOrEqualTo(mXML2MsrNo,(int) divsOffset2,harmonyByMeasure2);
					else harmony2 = Utils.valueForKeyBeforeOrEqualTo((int) divsOffset2,harmoniesfor2Measure);
					
//					System.err.println("Harmony1 (divOffset " + divsOffset1 + "):" + harmony1);
//					System.err.println("Harmony2 (divOffset " + divsOffset2 + "):" + harmony2);
					
					if (harmony1 == null && harmony2 == null || harmony1 != null && harmony1.equals(harmony2)) {
						harmonyScore += SequencePair.MATCH_SCORE;
					} else {
						harmonyScore += SequencePair.MISMATCH_SCORE;
					}
				}
				
				divsOffset1 += divs1PerComparedDiv;
				divsOffset2 += divs2PerComparedDiv;
			}
		}
		
		score += lyricWeight * lyricScore;
		score += melodyWeight * melodyScore;
		score += harmonyWeight * harmonyScore;

		score /= (comparableBeats*comparableDivs);
		System.err.println("Normalized score for lyrics:" + lyricWeight * lyricScore);
		System.err.println("Normalized score for melody:" + melodyWeight * melodyScore);
		System.err.println("Normalized score for harmony:" + harmonyWeight * harmonyScore);
		System.err.println("Normalized score for measure:" + score);
		
		if (mXML1MsrNo == mXML2MsrNo)
			return identityWeight * score;
		else
			return score;
	}

	@Override
	public int seq1length() {
		return musicXML1.getMeasureCount();
	}

	@Override
	public int seq2length() {
		return musicXML2.getMeasureCount();
	}

}
