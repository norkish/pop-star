package tabcomplete.alignment;

import java.util.SortedMap;

public class MSASequencePair extends SequencePair {

	public class MSAStringAlignmentBuilder extends AlignmentBuilder {

		StringBuilder[] newAlnSeqBldrs = new StringBuilder[numSeqs];
		StringBuilder lyrSeqBldr = new StringBuilder();;
		
		public MSAStringAlignmentBuilder() {
			for (int k = 0; k < newAlnSeqBldrs.length; k++) {
				newAlnSeqBldrs[k] = new StringBuilder();
			}
		}
		
		@Override
		public void appendCharSequence1(int i) {
			for (int k = 0; k < numSeqs; k++) {
				newAlnSeqBldrs[k].append(alignmentsSequence[k].charAt(i));
			}
		}

		@Override
		public void appendCharSequence2(int j) {
			lyrSeqBldr.append(sequenceToAlign.charAt(j));
		}

		@Override
		public void appendIndelSequence1() {
			for (int k = 0; k < numSeqs; k++) {
				newAlnSeqBldrs[k].append(INDEL_CHAR);
			}
		}

		@Override
		public void appendIndelSequence2() {
			lyrSeqBldr.append(INDEL_CHAR);
		}

		@Override
		public void reverse() {
			for (int k = 0; k < numSeqs; k++) {
				newAlnSeqBldrs[k].reverse();
			}
			lyrSeqBldr.reverse();
		}

		@Override
		public Alignment renderAlignment() {
			String[] newAlignedSeqs = new String[numSeqs];
			for (int k = 0; k < numSeqs; k++) {
				newAlignedSeqs[k] = newAlnSeqBldrs[k].toString();
			}
			String alignedLyricSeq = lyrSeqBldr.toString();
			
			return new MSAStringAlignment(newAlignedSeqs, alignedLyricSeq, scores);
		}

	}

	String[] alignmentsSequence = null;
	int numSeqs;
	String sequenceToAlign;
	
	public MSASequencePair(SortedMap<Integer, String> alignedSeqs, String sequence) {
		numSeqs = alignedSeqs.size();
		alignmentsSequence = new String[numSeqs];
		int i = 0;
		for(String seq: alignedSeqs.values()) {
			alignmentsSequence[i++] = seq;
		}
		
		sequenceToAlign = sequence;
	}

	@Override
	public AlignmentBuilder newAlignmentBuilder() {
		return new MSAStringAlignmentBuilder();
	}

	@Override
	public double matchScore(int i, int j) {
		char charA;
		int cost = 0;
		for (String alnSeq: alignmentsSequence) {
			charA = alnSeq.charAt(i);
			if (charA == AlignmentBuilder.INDEL_CHAR){
				cost += GAP_EXTEND_SCORE;
			} else if (TokenComparator.matchCharactersGenerally(charA, sequenceToAlign.charAt(j))) {
				cost += MATCH_SCORE;
			} else {
				cost += MISMATCH_SCORE;
			}
		}
		// score for matching is the match/mismatch costs + #gaps * gap extend ost
		return cost;
	}
	
	@Override
	public int nonGapCharCount(int i) {
		char charA;
		int count = 0;
		for (String alnSeq: alignmentsSequence) {
			charA = alnSeq.charAt(i);
			if (charA != AlignmentBuilder.INDEL_CHAR){
				count++;
			} 
		}
		return count;
	}

	@Override
	public double leftGapCost(boolean forceExtend, int i) {
		if (forceExtend) {
			//Definitely gap extend for all of them
			return numSeqs * GAP_EXTEND_SCORE;
		} else { // otherwise
			// gotta check if the previous character for each aligned seq was a gap char
			// if so, then it is a gap extend
			// if not, then it is a gap open
			char charA;
			int gapCharCount = 0;
			for (String alnSeq: alignmentsSequence) {
				charA = alnSeq.charAt(i);
				if (charA == AlignmentBuilder.INDEL_CHAR){
					gapCharCount++;
				} 
			}
			return GAP_EXTEND_SCORE * gapCharCount + GAP_OPEN_SCORE * (numSeqs - gapCharCount);
		}
	}

	@Override
	public int seq1length() {
		return alignmentsSequence[0].length();
	}

	@Override
	public int seq2length() {
		return sequenceToAlign.length();
	}

}
