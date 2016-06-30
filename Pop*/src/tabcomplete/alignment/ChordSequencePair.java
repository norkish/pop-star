package tabcomplete.alignment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;

import harmony.Chord;

public class ChordSequencePair extends SequencePair {

	public class ChordSequenceAlignmentBuilder extends AlignmentBuilder {

		List<Chord> firstBldr = new ArrayList<Chord>();
		List<Chord> secondBldr = new ArrayList<Chord>();
		
		@Override
		public void appendCharSequence1(int i) {
			firstBldr.add(first.get(firstKeys[i]));
		}

		@Override
		public void appendCharSequence2(int j) {
			secondBldr.add(second.get(secondKeys[j]));
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
			return new ChordSequencePairAlignment(firstBldr.toArray(new Chord[0]), secondBldr.toArray(new Chord[0]), scores);
		}

		@Override
		public void reverse() {
			Collections.reverse(firstBldr);
			Collections.reverse(secondBldr);
			super.reverse();
		}
	}

	SortedMap<Integer, Chord> first, second;
	Integer[] firstKeys, secondKeys;
	
	public ChordSequencePair(SortedMap<Integer, Chord> first, SortedMap<Integer, Chord> second) {
		this.first = first;
		this.second = second;
		firstKeys = first.keySet().toArray(new Integer[0]);
		secondKeys = second.keySet().toArray(new Integer[0]);
	}

	@Override
	public AlignmentBuilder newAlignmentBuilder() {
		return new ChordSequenceAlignmentBuilder();
	}

	@Override
	public double matchScore(int i, int j) {
		return (TokenComparator.matchChordsGenerally(first.get(firstKeys[i]),second.get(secondKeys[j])) ? MATCH_SCORE : MISMATCH_SCORE);
	}

	@Override
	public int seq1length() {
		return first.size();
	}

	@Override
	public int seq2length() {
		return second.size();
	}

}
