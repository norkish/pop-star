package alignment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rhyme.HirjeeMatrix;
import rhyme.StressedPhone;

public class StressedPhonePair extends SequencePair {
	public class StressedPhoneAlignment extends AlignmentBuilder {

		List<StressedPhone> sequence1Builder = new ArrayList<StressedPhone>();
		List<StressedPhone> sequence2Builder = new ArrayList<StressedPhone>();
		
		@Override
		public void appendCharSequence1(int i) {
			sequence1Builder.add(word1[i]);
		}

		@Override
		public void appendCharSequence2(int j) {
			sequence2Builder.add(word2[j]);
		}

		@Override
		public void appendIndelSequence1() {
			sequence1Builder.add(null);
		}

		@Override
		public void appendIndelSequence2() {
			sequence2Builder.add(null);
		}
		
		@Override
		public void reverse() {
			Collections.reverse(sequence1Builder);
			Collections.reverse(sequence2Builder);
			super.reverse();
		}

		@Override
		public Alignment renderAlignment() {
			return new StressedPhonePairAlignment(sequence1Builder.toArray(new StressedPhone[0]), sequence2Builder.toArray(new StressedPhone[0]),scores);
		}

	}

	StressedPhone[] word1, word2;
	
	public StressedPhonePair(StressedPhone[] word1sPs, StressedPhone[] word2sPs) {
		word1 = word1sPs;
		word2 = word2sPs;
	}

	@Override
	public AlignmentBuilder newAlignmentBuilder() {
		return new StressedPhoneAlignment();
	}

	@Override
	public double matchScore(int row_1, int i) {
		return HirjeeMatrix.score(word1[row_1].phone,word2[i].phone);
	}

	@Override
	public int seq1length() {
		return word1.length;
	}

	@Override
	public int seq2length() {
		// TODO Auto-generated method stub
		return word2.length;
	}

}
