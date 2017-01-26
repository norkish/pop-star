package tabcomplete.alignment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import data.MusicXMLParser.Harmony;
import data.MusicXMLParser.Note;
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
				firstArray[i] = firstBldr.get(i);
			}
			int[] secondArray = new int[firstBldr.size()];
			for (int i = 0; i < secondArray.length; i++) {
				secondArray[i] = firstBldr.get(i);
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
		double score = 0.0;
		
		SortedMap<Integer, Note> notesfor1Measure = notesByMeasure1.get(mXML1MsrNo);
		SortedMap<Integer, Note> notesfor2Measure = notesByMeasure2.get(mXML2MsrNo);
		int beatsIn1Msr = Utils.valueForKeyBeforeOrEqualTo(mXML1MsrNo, musicXML1.timeByMeasure).beats;
		int beatsIn2Msr = Utils.valueForKeyBeforeOrEqualTo(mXML2MsrNo, musicXML2.timeByMeasure).beats;
		int comparableBeats = Math.min(beatsIn1Msr, beatsIn2Msr);

		int divsPerQIn1Msr = Utils.valueForKeyBeforeOrEqualTo(mXML1MsrNo, musicXML1.divsPerQuarterByMeasure);
		int divsPerQIn2Msr = Utils.valueForKeyBeforeOrEqualTo(mXML2MsrNo, musicXML2.divsPerQuarterByMeasure);
		int comparableDivs = Math.max(divsPerQIn1Msr, divsPerQIn2Msr);
		
		// for each comparable beat
		for (int beat = 0; beat < comparableBeats; beat++) {
			for (int div = 0; div < comparableDivs; div++) {
				
			}
		}
		
		// 
		
		// find smallest division, iterate in those increments across both measures, comparing values at each div offset
		
		
		// calculate the subscore for lyrics
		if (lyricWeight != 0.0) {
			double lyricScore = 0.0;
			
			
			score += lyricWeight * lyricScore ;
		}
		
		// calculate the subscore for melody
		if (melodyWeight != 0.0) {
			double melodyScore = 0.0;
			
			
			score += melodyWeight * melodyScore ;
		}
		
		// calculate the subscore for harmony
		if (harmonyWeight != 0.0) {
			double harmonyScore = 0.0;
			
			
			score += harmonyWeight * harmonyScore ;
		}
		
		
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
