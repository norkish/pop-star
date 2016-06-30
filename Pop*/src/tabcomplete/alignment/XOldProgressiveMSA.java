/**
 * 
 */
package tabcomplete.alignment;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;

import tabcomplete.alignment.SequencePair.AlignmentBuilder;
import tabcomplete.rawsheet.LyricSheet;
import tabcomplete.rawsheet.RawDataLoader;
import tabcomplete.utils.Utils;

/**
 * @author norkish
 *
 */
public class XOldProgressiveMSA {

	SortedMap<Integer, String> alignedSeqs = new TreeMap<Integer, String>();
	int[] scores;
	int numSeqs;
	private int maxScoreAlnPos;
	private int maxScore;

	public XOldProgressiveMSA(String[] sequences) {
		numSeqs = sequences.length;
		XOptimizedBandedPairwiseAlignment aln;
		if (numSeqs <= 1) {
			throw new RuntimeException("Cannot perform MSA with " + numSeqs + " sequence(s)");
		} else if (numSeqs == 2) {
			aln = new XOptimizedBandedPairwiseAlignment(sequences[0], sequences[1]);

			alignedSeqs.put(0, aln.getFirst());
			alignedSeqs.put(1, aln.getSecond());
			scores = aln.getScores();
		} else {

			Aligner.setMinPercOverlap(0.5);
			XOptimizedBandedPairwiseAlignment[][] alns = new XOptimizedBandedPairwiseAlignment[numSeqs][numSeqs];

			int maxScoreIdx = -1;
			int maxScoreIdx2 = -1;
			int maxScore = -1;
			String sequence1, sequence2;
			// Align all pairs of songs and enqueue with priority equal to alignment score
			for (int i = 0; i < numSeqs - 1; i++) {
				sequence1 = sequences[i];
				for (int j = i + 1; j < numSeqs; j++) {
					sequence2 = sequences[j];
					aln = new XOptimizedBandedPairwiseAlignment(sequence1, sequence2);
					alns[i][j] = aln;
					alns[j][i] = aln;
					if (aln.getScore() > maxScore) {
						maxScore = aln.getScore();
						maxScoreIdx = i;
						maxScoreIdx2 = j;
					}
				}
			}

			int[] accumulativeAlnScores = new int[numSeqs];
			aln = alns[maxScoreIdx][maxScoreIdx2];

			alignedSeqs.put(maxScoreIdx, aln.getFirst());
			updateAccumulativePairwiseScores(maxScoreIdx, accumulativeAlnScores, alns);

			alignedSeqs.put(maxScoreIdx2, aln.getSecond());
			updateAccumulativePairwiseScores(maxScoreIdx2, accumulativeAlnScores, alns);

			for (int numSeqsAln = 2; numSeqsAln < alns.length; numSeqsAln++) {
				// System.out.println("\n\nALN AFTER ALIGNING " + numSeqsAln + ":");
				// System.out.println(this);
				// To decide which seq to aln next, we sum at the aln scores of each
				// remaining unaligned seq with all aligned seqs

				maxScoreIdx = getNextBestUnalignedSeq(accumulativeAlnScores);

				addToExistingAlignment(maxScoreIdx, sequences[maxScoreIdx]);

				updateAccumulativePairwiseScores(maxScoreIdx, accumulativeAlnScores, alns);
			}
		}
		
		scoreSW();
	}

	/*
	 * This method assumes that there are no nulls in alignedSeqs and that the size of alignedSeqs is greater than 0
	 */
	private void scoreSW() {
		String sequence1 = alignedSeqs.get(alignedSeqs.firstKey());
		int alnLength = sequence1.length();
		scores = new int[alnLength];

		char[] tokensInCol = new char[numSeqs];
		for (int j = 0; j < numSeqs; j++) {
			tokensInCol[j] = alignedSeqs.get(j).charAt(0);
		}
		scores[0] = Math.max(0, ProgressiveMSA.scoreColumn(tokensInCol));
		for (int i = 1; i < alnLength; i++) {
			for (int j = 0; j < numSeqs; j++) {
				tokensInCol[j] = alignedSeqs.get(j).charAt(i);
			}
			scores[i] = Math.max(0, scores[i - 1] + ProgressiveMSA.scoreColumn(tokensInCol));
			if (scores[i] >= maxScore) {
				maxScore = scores[i];
				maxScoreAlnPos = i;
			}
		}
	}

	private void addToExistingAlignment(int alnIdx, String sequence) {
		String[] alignments = alignedSeqs.values().toArray(new String[0]);

		XSeqToAlnAlignment aln = new XSeqToAlnAlignment(alignments, sequence);

		String[] newAlignedSeqs = aln.getNewAlignedSeqs();
		int idx = 0;
		for (Integer key : alignedSeqs.keySet()) {
			alignedSeqs.put(key, newAlignedSeqs[idx++]);
		}

		String alignedLyricSeq = aln.getAlignedLyricSeq();
		alignedSeqs.put(alnIdx, alignedLyricSeq);

	}

	private int getNextBestUnalignedSeq(int[] accumulativeAlnScores) {
		int score, biggestScore = -1;
		int biggestScoreIdx = -1;
		for (int i = 0; i < accumulativeAlnScores.length; i++) {
			score = accumulativeAlnScores[i];
			if (score > biggestScore) {
				biggestScore = score;
				biggestScoreIdx = i;
			}
		}
		return biggestScoreIdx;
	}

	/*
	 * Each time a seq is added to the MSA, we need update which of the remaining seqs is most relevant. For each
	 * remaining seq we add its aln score with the most recently added seq.
	 */
	private void updateAccumulativePairwiseScores(int maxScoreIdx, int[] accumulativeAlnScores,
			XOptimizedBandedPairwiseAlignment[][] alns) {

		accumulativeAlnScores[maxScoreIdx] = -1;

		for (int i = 0; i < accumulativeAlnScores.length; i++) {
			if (accumulativeAlnScores[i] == -1)
				continue;

			accumulativeAlnScores[i] += alns[maxScoreIdx][i].getScore();
		}
	}

	public String toString() {
		StringBuilder str = new StringBuilder();

		for (Integer key : alignedSeqs.keySet()) {
			String string = alignedSeqs.get(key);
			str.append(key);
			str.append("\t&x");
			if (string != null) {
				str.append(StringUtils.countMatches(string, '&'));
				str.append("\t");
				str.append(string.replaceAll("\n", "#"));
			}
			str.append("\n");
		}
		str.append("Consensus:\t");
		str.append(getConsensus().replaceAll("\n", "#"));
		str.append("\n");
		if (scores != null)
			str.append("Starting from score " + scores[maxScoreAlnPos] + " at pos " + maxScoreAlnPos + "\n");

		str.append(Arrays.toString(scores));

		return str.toString();
	}

	public String getConsensus() {
		StringBuilder str = new StringBuilder();
		if (scores == null)
			return "N/A";

		char consensusAtPos;
		for (int i = maxScoreAlnPos; i >= 0 && scores[i] > 0; i--) {
			consensusAtPos = getConsensusAtPos(i);
			if (consensusAtPos != AlignmentBuilder.INDEL_CHAR)
				str.append(consensusAtPos);
		}

		return str.reverse().toString();
	}

	private char getConsensusAtPos(int i) {
		Map<Character, Integer> countMap = new LinkedHashMap<Character, Integer>();
		char aChar;
		Integer count;
		for (String alnSeq : alignedSeqs.values()) {
			aChar = alnSeq.charAt(i);
			// if (Character.isWhitespace(aChar))
			// whiteSpaceCount++;
			count = countMap.get(aChar);
			if (count == null)
				count = 1;
			else
				count += 1;
			countMap.put(aChar, count);
		}

		// For classes of characters, normalize to most frequent in class
		int allWhiteSpaceCount = 0;
		int maxWhiteSpaceCount = 0;
		int maxCharCount = 0;
		char maxWhiteSpaceChar = 0, maxCharChar = 0, oppositeCase;
		Integer oppositeCaseCount;
		for (Character c : countMap.keySet()) {
			count = countMap.get(c);
			if (count != null) {
				if (Character.isWhitespace(c)) {
					allWhiteSpaceCount += count;
					if (count > maxWhiteSpaceCount) {
						maxWhiteSpaceCount = count;
						maxWhiteSpaceChar = c;
					}
				} else {
					// Add counts for "y" and "Y" and keep the one with the higher count, but let the sum represent
					// them.
					// Maybe check if both are in the map and which has the higher count and put all the votes for that
					// and 0 for the other.
					// Akin to pretending all instances were the same as the maximally occurring instance
					if (Character.isUpperCase(c)) {
						oppositeCase = Character.toLowerCase(c);
					} else if (Character.isLowerCase(c)) {
						oppositeCase = Character.toUpperCase(c);
					} else {
						oppositeCase = '\0';
					}

					oppositeCaseCount = countMap.get(oppositeCase);
					if (oppositeCaseCount != null) {
						if (count < oppositeCaseCount) { // if this case has higher count
							c = oppositeCase;
						}
						count += oppositeCaseCount; // add its counts to this case
						countMap.put(oppositeCase, null); // remove the opposite case
					}

					if (count > maxCharCount || count == maxCharCount && !Character.isAlphabetic(maxCharChar)) {
						maxCharCount = count;
						maxCharChar = c;
					}
				}
			}
		}

		if (allWhiteSpaceCount > maxCharCount || allWhiteSpaceCount == maxCharCount && !Character.isAlphabetic(maxCharChar))
			return maxWhiteSpaceChar;
		return maxCharChar;
	}

	/**
	 * @param args
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException {
		Map<String, Map<String, List<LyricSheet>>> lyricSheets = RawDataLoader.loadLyricSheets();
		for (String key : lyricSheets.keySet()) {
			for (String key2 : lyricSheets.get(key).keySet()) {
				List<LyricSheet> tests = lyricSheets.get(key).get(key2);
				String[] lyrics = new String[tests.size()];
				for (int i = 0; i < tests.size(); i++) {
					LyricSheet lyric = tests.get(i);
					lyrics[i] = lyric.getLyrics();
				}
				XOldProgressiveMSA msa = new XOldProgressiveMSA(lyrics);
				if (tests.size() > 2) {
					System.out.println("\nFOR " + key + ":" + key2 + ":");
					System.out.println(msa);
					for (LyricSheet lyric : tests) {
						System.out.println(lyric.getURL());
					}
					Utils.promptEnterKey("");
				}
			}
		}
	}

}
