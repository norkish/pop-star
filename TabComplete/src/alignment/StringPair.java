package alignment;

public class StringPair extends SequencePair {

	public class StringPairAlignmentBuilder extends AlignmentBuilder {

		StringBuilder first = new StringBuilder();
		StringBuilder second = new StringBuilder();
		
		@Override
		public void appendCharSequence1(int i) {
			first.append(seq1.charAt(i));
		}

		@Override
		public void appendCharSequence2(int j) {
			second.append(seq2.charAt(j));
		}

		@Override
		public void appendIndelSequence1() {
			first.append(INDEL_CHAR);
		}

		@Override
		public void appendIndelSequence2() {
			second.append(INDEL_CHAR);
		}

		@Override
		public void reverse() {
			first.reverse();
			second.reverse();
			super.reverse();
		}

		@Override
		public Alignment renderAlignment() {
			return new StringPairAlignment(first.toString(), second.toString(), scores);
		}

	}

	private String seq1;
	private String seq2;

	public StringPair(String string, String string2) {
		seq1 = string;
		seq2 = string2;
	}

	@Override
	public AlignmentBuilder newAlignmentBuilder() {
		return new StringPairAlignmentBuilder();
	}

	@Override
	public double matchScore(int i, int j) {
		return (TokenComparator.matchCharactersGenerally(seq1.charAt(i),seq2.charAt(j)) ? MATCH_SCORE : MISMATCH_SCORE);
	}

	@Override
	public int seq1length() {
		return seq1.length();
	}

	@Override
	public int seq2length() {
		return seq2.length();
	}
}
