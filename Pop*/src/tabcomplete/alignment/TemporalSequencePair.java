package tabcomplete.alignment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import harmony.Chord;

public class TemporalSequencePair extends SequencePair {

	public class TemporalSequenceAlignmentBuilder extends AlignmentBuilder{

		List<Double> firstBldr = new ArrayList<Double>();
		List<Double> secondBldr = new ArrayList<Double>();
		
		@Override
		public void appendCharSequence1(int i) {
			firstBldr.add(sequence1.get(i));
		}

		@Override
		public void appendCharSequence2(int j) {
			secondBldr.add(sequence2.get(j));			
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
			return new TemporalSequencePairAlignment(firstBldr.toArray(new Double[0]), secondBldr.toArray(new Double[0]), scores);
		}
		
		@Override
		public void reverse() {
			Collections.reverse(firstBldr);
			Collections.reverse(secondBldr);
			super.reverse();
		}
	}

	List<Double> sequence1 = null;
	List<Double> sequence2 = null;
	
	public TemporalSequencePair(List<Double> seq1, List<Double> seq2) {
		this.sequence1 = seq1;
		this.sequence2 = seq2;
	}

	@Override
	public AlignmentBuilder newAlignmentBuilder() {
		return new TemporalSequenceAlignmentBuilder();
	}

	@Override
	public double matchScore(int i, int j) {
		return -Math.abs(sequence1.get(i) - sequence2.get(j));
	}

	@Override
	public int seq1length() {
		return sequence1.size();
	}

	@Override
	public int seq2length() {
		return sequence2.size();
	}

}
