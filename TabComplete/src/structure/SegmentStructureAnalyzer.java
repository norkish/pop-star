/**
 * 
 */
package structure;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import alignment.Aligner;
import alignment.Alignment;
import alignment.ChordSequencePair;
import alignment.SequencePair;
import alignment.StringPair;
import harmony.Chord;
import tabutils.Utils;
import utils.Pair;

/**
 * @author norkish
 *
 */
public class SegmentStructureAnalyzer {

	private static final int MIN_DIST_BETWEEN_CHORUSES = 1;
	private static final double SIMILARITY_THRESH = 0.9;
	private static final int CHORUS_HALO = 4;
	private static final double DISTRIBUTION_WEIGHT = 1.0;
	private static final double LENGTH_WEIGHT = 1.0;
	private static final double REPETITIONS_WEIGHT = 1.0;
	private static final int MIN_CHORUS_COUNT = 2;
	private static final int MAX_CHORUS_COUNT = 7;

	public static char[] extractSegmentStructure(List<String> words, List<SortedMap<Integer, Chord>> chords) {
		int lineCount = words.size();

		char[] structure = new char[lineCount];

		int firstLyricLine = extractIntro(words, structure);

		if (firstLyricLine == structure.length) {
			return structure;
		}


		// Find outro as all ending lines w/o lyrics
		int lastLyricLine = extractOutro(words, structure); 

		// Find chorus	
		extractChorusViaBinaryAlnAndCandidateScoring(words, structure, firstLyricLine, lastLyricLine);
		//		extractChorusViaBinaryAln(words, structure, firstLyricLine, lastLyricLine);
		//		extractChorusViaRawScoreAln(words, structure, firstLyricLine, lastLyricLine);
		System.out.println("IDENITIFYING VERSES");
		extractVerse(chords, structure, firstLyricLine, lastLyricLine);

		fillInBridgesInterludesAndIntroOutro(structure, words);

		return structure;
	}

	private static void fillInBridgesInterludesAndIntroOutro(char[] structure, List<String> words) {
		int i;
		for (i = 0; i < structure.length; i++) {
			if(structure[i] == '\0') {
				structure[i] = 'I';
			}
			if(structure[i] != 'I') {
				i++;
				break;
			}
		}

		int o;
		for (o = structure.length-1; o > i ; o--) {
			if(structure[o] == '\0') {
				structure[o] = 'O';
			}
			if(structure[o] != 'O') {
				break;
			}
		}

		for (int b = i; b < o; b++) {
			if(structure[b] == '\0') {
				if (words.get(b).length() > 0) {
					structure[b] = 'B';
				} else { 
					structure[b] = 'N';
				}
			}
		}
	}

	/**
	 * Identify and label lines that belong to verse segments
	 * @pre This function assumes that choruses have already been identified.
	 * @param chords
	 * @param structure
	 * @param firstLyricLine
	 * @param lastLyricLine
	 * @param scheme
	 */
	private static void extractVerse(List<SortedMap<Integer, Chord>> chords, char[] structure, int firstLyricLine,
			int lastLyricLine) {
		// TODO Auto-generated method stub
		int [][] binary_matrix = new int[(lastLyricLine+1-firstLyricLine-MIN_DIST_BETWEEN_CHORUSES)][]; 
		int [] currentRow;

		int line_i_idx, line_j_idx;
		double score;
		SortedMap<Integer, Chord> line_i, line_j;
		Alignment aln;

		SequencePair.setCosts(1, -1, -1, -1);

		for (int offset = 0; offset < binary_matrix.length; offset++) {
			currentRow = new int[(lastLyricLine+1-firstLyricLine-MIN_DIST_BETWEEN_CHORUSES) - offset];
			binary_matrix[offset] = currentRow;

			for (int idx = 0; idx < currentRow.length; idx++) {
				line_i_idx = idx + firstLyricLine; // we're not aligning intro or outro lines so we have to account for the offset due to the intro
				line_j_idx = line_i_idx + offset + MIN_DIST_BETWEEN_CHORUSES;

				if (structure[line_i_idx] != '\0' || structure[line_j_idx] != '\0') {
					continue;
				}

				line_i = chords.get(line_i_idx);
				line_j = chords.get(line_j_idx);

				if (line_i.size() == 0 || line_j.size() == 0) {
					continue;
				}

				aln = Aligner.alignNW(new ChordSequencePair(line_i,line_j));
				//				System.out.println(aln);
				score = aln.getFinalScore();

				score /= (double) Math.min(line_i.size(),line_j.size());

				if (score > SIMILARITY_THRESH) {
					currentRow[idx] = 1;
				}
			}
		}

		// Find all candidate verses
		SortedMap<Integer, SortedMap<Integer, SortedSet<Integer>>> candidateVerses = findAllRepeatedSegments(binary_matrix);
		if (candidateVerses.isEmpty()) {
			System.out.println("No candidates for verse");
			return;
		}
		Pair<SortedSet<Integer>, Integer> verse = judgeCandidates(candidateVerses, lastLyricLine+1-firstLyricLine);

		for (Integer startPos : candidateVerses.keySet()) {
			SortedMap<Integer, SortedSet<Integer>> candidatesAtStart = candidateVerses.get(startPos);

			for (Integer len : candidatesAtStart.keySet()) {
				SortedSet<Integer> poses = candidatesAtStart.get(len);
				System.out.print("Match pos " + (startPos+ firstLyricLine) + ", candidate of length " + len + " with score " + scoreCandidate(len,poses,lastLyricLine+1-firstLyricLine) + " at positions: ");
				List<Integer> posesList = new ArrayList<Integer>(poses);
				for(Integer i: posesList){
					System.out.print("\t" + (i + firstLyricLine));
				}
				System.out.println();
			}
		}

		System.out.println("Chose " + verse + " with score " + scoreCandidate(verse.getSecond(),verse.getFirst(),lastLyricLine+1-firstLyricLine));

		Integer chorusLen = verse.getSecond();
		for (Integer start : verse.getFirst()) {
			for (int i = 0; i < chorusLen; i++) {
				structure[start+i+firstLyricLine] = 'V';
			}
		}

		//		Utils.print2DMatrixInt(binary_matrix);
	}

	/**
	 * @param words
	 * @param structure
	 * @param firstLyricLine
	 * @param lastLyricLine
	 */
	private static void extractChorusViaBinaryAlnAndCandidateScoring(List<String> words, char[] structure, int firstLyricLine,
			int lastLyricLine) {
		int [][] binary_matrix = new int[(lastLyricLine+1-firstLyricLine-MIN_DIST_BETWEEN_CHORUSES)][]; 
		int [] currentRow;

		int line_i_idx, line_j_idx;
		double score;
		String line_i, line_j;
		Alignment aln;

		SequencePair.setCosts(1, -1, -1, 0);

		for (int offset = 0; offset < binary_matrix.length; offset++) {
			currentRow = new int[(lastLyricLine+1-firstLyricLine-MIN_DIST_BETWEEN_CHORUSES) - offset];
			binary_matrix[offset] = currentRow;

			for (int idx = 0; idx < currentRow.length; idx++) {
				line_i_idx = idx + firstLyricLine; // we're not aligning intro or outro lines so we have to account for the offset due to the intro
				line_j_idx = line_i_idx + offset + MIN_DIST_BETWEEN_CHORUSES;
				line_i = words.get(line_i_idx);
				line_j = words.get(line_j_idx);

				if (line_i.length() == 0 || line_j.length() == 0) {
					continue;
				}

				aln = Aligner.alignNW(new StringPair(line_i,line_j));
				score = aln.getFinalScore();

				score /= (double) Math.min(line_i.length(),line_j.length());

				if (score > SIMILARITY_THRESH) {
					currentRow[idx] = 1;
				}
			}
		}

		// Find all candidate choruses
		SortedMap<Integer, SortedMap<Integer, SortedSet<Integer>>> candidateChoruses = findAllRepeatedSegments(binary_matrix);
		if (candidateChoruses.isEmpty()) {
			System.out.println("No candidates for verse");
			return;
		}
		Pair<SortedSet<Integer>, Integer> chorus = judgeCandidates(candidateChoruses, lastLyricLine+1-firstLyricLine);

		for (Integer startPos : candidateChoruses.keySet()) {
			SortedMap<Integer, SortedSet<Integer>> candidatesAtStart = candidateChoruses.get(startPos);

			for (Integer len : candidatesAtStart.keySet()) {
				SortedSet<Integer> poses = candidatesAtStart.get(len);
				System.out.print("Match pos " + (startPos+ firstLyricLine) + ", candidate of length " + len + " with score " + scoreCandidate(len,poses,lastLyricLine+1-firstLyricLine) + " at positions: ");
				List<Integer> posesList = new ArrayList<Integer>(poses);
				for(Integer i: posesList){
					System.out.print("\t" + (i + firstLyricLine));
				}
				System.out.println();
			}
		}

		System.out.println("Chose " + chorus + " with score " + scoreCandidate(chorus.getSecond(),chorus.getFirst(),lastLyricLine+1-firstLyricLine));

		Integer chorusLen = chorus.getSecond();
		for (Integer start : chorus.getFirst()) {
			for (int i = 0; i < chorusLen; i++) {
				structure[start+i+firstLyricLine] = 'C';
			}
		}

		//		Utils.print2DMatrixInt(binary_matrix);
	}

	private static Pair<SortedSet<Integer>,Integer> judgeCandidates(SortedMap<Integer, SortedMap<Integer, SortedSet<Integer>>> candidateChoruses, int wordsLength) {
		SortedSet<Integer> bssf = null;
		double bssfScore = -1;
		double score;
		int bssfLen = -1;
		SortedSet<Integer> candidate;
		for (SortedMap<Integer, SortedSet<Integer>> candidatesByStart : candidateChoruses.values()) {
			for (Integer len : candidatesByStart.keySet()) {
				candidate = candidatesByStart.get(len);
				score = scoreCandidate(len, candidate, wordsLength);

				if (score < bssfScore) {
					continue;
				} else 	if (score > bssfScore || len > bssfLen || len == bssfLen && candidate.size() > bssf.size()) {
					// new best
					bssf = candidate;
					bssfScore = score;
					bssfLen = len;
				}
			}
		}

		return new Pair<SortedSet<Integer>,Integer>(bssf,bssfLen);
	}

	private static double scoreCandidate(int len, SortedSet<Integer> candidate, int wordsLength) {
		double score = 0;
		double coverage = 0.;

		// distribution score
		int prev = 0;
		boolean first = true;
		for (Integer pos : candidate) {
			if (first) {
				coverage += Math.min(CHORUS_HALO/2.0, pos - prev);
				first = false;
			} else {
				coverage += Math.min(CHORUS_HALO, pos - prev);
			}
			coverage += len;
			prev = pos + len;
		}
		coverage += Math.min(CHORUS_HALO/2.0, wordsLength - candidate.last() - len);
		score += DISTRIBUTION_WEIGHT * coverage/wordsLength;

		// length score
		score += LENGTH_WEIGHT * (len-1) / (double) wordsLength;

		// repetitions score

		int repetitions = candidate.size();
		double repetitionScore = 0;

		if (repetitions == MIN_CHORUS_COUNT || repetitions == MAX_CHORUS_COUNT) {
			repetitionScore = .5;
		} else if (repetitions > MIN_CHORUS_COUNT && repetitions < MAX_CHORUS_COUNT) {
			repetitionScore = 1.0; 
		}

		score += REPETITIONS_WEIGHT * repetitionScore;

		return score;
	}

	private static SortedMap<Integer, SortedMap<Integer, SortedSet<Integer>>> findAllRepeatedSegments(int[][] binary_matrix) {
		SortedMap<Integer, SortedMap<Integer, SortedSet<Integer>>> candidateRepeats = new TreeMap<Integer,SortedMap<Integer,SortedSet<Integer>>>();
		SortedMap<Integer, SortedSet<Integer>> candidatesAtStart;
		//Start from the end in a dynamic programming approach
		for (int startPos = binary_matrix[0].length; startPos >= 0 ; startPos--) {
			candidatesAtStart = findCandidateRepeatsStartingAt(startPos, binary_matrix, candidateRepeats);
			if (candidatesAtStart.isEmpty()) {
				continue; // no candidates
			} else if (candidatesAtStart.size() > 1) {
				// remove redundancy at same position (e.g., perfectly overlapping candidates of length 1, 2, 3, 4, etc)
				List<Integer> lengths = new ArrayList<Integer>(candidatesAtStart.keySet());
				for(int i=lengths.size()-1; i>0;i--){
					if (candidatesAtStart.get(lengths.get(i)).size() == candidatesAtStart.get(lengths.get(i-1)).size()) {
						candidatesAtStart.remove(lengths.get(i-1));
						lengths.remove(i-1);
					}
				}
			}
			candidateRepeats.put(startPos, candidatesAtStart);
		}
		return candidateRepeats;
	}

	/** Each value v (associated with key k) in the returned map will be a candidate chorus which starts at startPos and has a length of k lines
	 * A candidate chorus is simply a list of start/stop line numbers where the several instances of the candidate chorus occur
	 * 
	 * NOTE: this method takes everything that matches A or everything that matches
	 * B, etc. 
	 * 
	 * @param startPos
	 * @param binary_matrix
	 * @param candidateChoruses 
	 * @return
	 */
	private static SortedMap<Integer,SortedSet<Integer>> findCandidateRepeatsStartingAt(int startPos, int[][] binary_matrix, SortedMap<Integer, SortedMap<Integer, SortedSet<Integer>>> allCandidateChoruses) {
		SortedMap<Integer, SortedSet<Integer>> candidateRepeats = new TreeMap<Integer,SortedSet<Integer>>();

		boolean prevLineWasARepeat = false; // keep track of if we've skipped a line that wasn't marked as repetitive
		int[] currentRow;
		int matchingLoc;
		SortedMap<Integer, SortedSet<Integer>> candidateRepeatsForMatchingLoc;
		SortedSet<Integer> candidateRepeatsForMatchingLocOfSameSize;
		SortedSet<Integer> candidateRepeatsForLen;
		// for each row of the matrix
		for (int offset = 0; offset < binary_matrix.length; offset++) {
			currentRow = binary_matrix[offset];
			if (currentRow.length <= startPos) { // if the startPos can't repeat in the song given the offset under consideration
				break;
			} else if (currentRow[startPos] != 1) { // can't be a candidate at this offset because lines at this offset don't match
				continue;
			}


			// for each potential chorus length
			for (int candidateLen = 1; candidateLen <= currentRow.length - startPos; candidateLen++) {
				if (currentRow[startPos+candidateLen-1] != 1) { // not a repeat
					if (prevLineWasARepeat) { // we allow one non-repeat line between repeat lines
						prevLineWasARepeat = false;
						continue;
					} else { // two-consecutive non-repeat lines precludes chorus candidacy
						break;
					}
				} else {
					prevLineWasARepeat = true;
				}

				candidateRepeatsForLen = candidateRepeats.get(candidateLen);
				if (candidateRepeatsForLen == null) {
					candidateRepeatsForLen = new TreeSet<Integer>();
					candidateRepeats.put(candidateLen, candidateRepeatsForLen);
					candidateRepeatsForLen.add(startPos);
				} else {
					candidateRepeatsForLen = candidateRepeats.get(candidateLen);
				}

				matchingLoc = startPos+offset + 1;
				// add the new instance of the repetition
				// add the matching location
				candidateRepeatsForLen.add(matchingLoc);

				// assume that the matching location doesn't know about this match
				candidateRepeatsForMatchingLoc = allCandidateChoruses.get(matchingLoc);
				if (candidateRepeatsForMatchingLoc == null) {
					candidateRepeatsForMatchingLoc = new TreeMap<Integer, SortedSet<Integer>>(); 
					allCandidateChoruses.put(matchingLoc, candidateRepeatsForMatchingLoc);
				}
				candidateRepeatsForMatchingLocOfSameSize = candidateRepeatsForMatchingLoc.get(candidateLen);
				if (candidateRepeatsForMatchingLocOfSameSize == null) {
					candidateRepeatsForMatchingLocOfSameSize = new TreeSet<Integer>();
					candidateRepeatsForMatchingLoc.put(candidateLen, candidateRepeatsForMatchingLocOfSameSize);
					candidateRepeatsForMatchingLocOfSameSize.add(matchingLoc);
				}
				candidateRepeatsForMatchingLocOfSameSize.add(startPos);

			}

		}

		return candidateRepeats;
	}

	/** Each value v (associated with key k) in the returned map will be a candidate chorus which starts at startPos and has a length of k lines
	 * A candidate chorus is simply a list of start/stop line numbers where the several instances of the candidate chorus occur
	 * 
	 * NOTE: PROBLEM this method assumes that if A matches B and B matches C that A should match C, rather than just taking everything that matches A or everything that matches
	 * B, etc. 
	 * 
	 * @param startPos
	 * @param binary_matrix
	 * @param candidateChoruses 
	 * @return
	 */
	private static Map<Integer,Set<Integer>> findCandidateChorusesViaCircularizationStartingAt(int startPos, int[][] binary_matrix, Map<Integer, Map<Integer, Set<Integer>>> allCandidateChoruses) {
		Map<Integer, Set<Integer>> candidateChoruses = new TreeMap<Integer,Set<Integer>>();

		boolean prevLineWasARepeat = false; // keep track of if we've skipped a line that wasn't marked as repetitive
		int[] currentRow;
		int matchingLoc;
		Map<Integer, Set<Integer>> candidateChorusesForMatchingLoc;
		Set<Integer> candidateChorusesForMatchingLocOfSameSize;
		Set<Integer> candidateChorusForLen;
		// for each row of the matrix
		for (int offset = 0; offset < binary_matrix.length; offset++) {
			currentRow = binary_matrix[offset];
			if (currentRow.length <= startPos) { // if the startPos can't repeat in the song given the offset under consideration
				break;
			} else if (currentRow[startPos] != 1) { // can't be a candidate at this offset because lines at this offset don't match
				continue;
			}

			// for each potential chorus length
			for (int candidateLen = 1; candidateLen <= currentRow.length - startPos; candidateLen++) {
				if (currentRow[startPos+candidateLen-1] != 1) { // not a repeat
					if (prevLineWasARepeat) { // we allow one non-repeat line between repeat lines
						prevLineWasARepeat = false;
						continue;
					} else { // two-consecutive non-repeat lines precludes chorus candidacy
						break;
					}
				} else {
					prevLineWasARepeat = true;
				}

				candidateChorusForLen = candidateChoruses.get(candidateLen);
				if (candidateChorusForLen == null) {
					candidateChorusForLen = new HashSet<Integer>();
					candidateChoruses.put(candidateLen, candidateChorusForLen);
					candidateChorusForLen.add(startPos);
				} else {
					candidateChorusForLen = candidateChoruses.get(candidateLen);
				}
				matchingLoc = startPos+offset + 1;
				// add the new instance of the repetition

				// and if the matching location already is a matching candidate chorus of the same length
				candidateChorusesForMatchingLoc = allCandidateChoruses.get(matchingLoc);
				candidateChorusesForMatchingLocOfSameSize = null;
				if (candidateChorusesForMatchingLoc != null) {
					candidateChorusesForMatchingLocOfSameSize = candidateChorusesForMatchingLoc.get(candidateLen);
				}
				if (candidateChorusesForMatchingLocOfSameSize != null) {
					// add all of the downstream instances of the repetition which have already been processed
					candidateChorusForLen.addAll(candidateChorusesForMatchingLocOfSameSize);
				} else {
					// otherwise just add the matching location
					candidateChorusForLen.add(matchingLoc);
				}

			}
		}

		return candidateChoruses;
	}

	/**
	 * @param words
	 * @param structure
	 * @param firstLyricLine
	 * @param lastLyricLine
	 */
	private static void extractChorusViaBinaryAln(List<String> words, char[] structure, int firstLyricLine,
			int lastLyricLine) {
		int [][] binary_matrix = new int[(lastLyricLine+1-firstLyricLine-MIN_DIST_BETWEEN_CHORUSES)][]; 
		int [] currentRow;

		int longest_diagonal = 1;
		Set<Integer> end_pts = new HashSet<Integer>();

		int curr_diagonal, line_i_idx, line_j_idx;
		double score;
		String line_i, line_j;
		Alignment aln;

		SequencePair.setCosts(1, -1, -1, 0);

		for (int offset = 0; offset < binary_matrix.length; offset++) {
			currentRow = new int[(lastLyricLine+1-firstLyricLine-MIN_DIST_BETWEEN_CHORUSES) - offset];
			binary_matrix[offset] = currentRow;

			curr_diagonal = 0;
			for (int idx = 0; idx < currentRow.length; idx++) {
				line_i_idx = idx + firstLyricLine; // we're not aligning intro or outro lines so we have to account for the offset due to the intro
				line_j_idx = line_i_idx + offset + MIN_DIST_BETWEEN_CHORUSES;
				line_i = words.get(line_i_idx);
				line_j = words.get(line_j_idx);
				//				System.out.println("LINE i: " + line_i_idx + " " + line_i);
				//				System.out.println("LINE j: " + line_j_idx + " " + line_j);

				if (line_i.length() == 0 || line_j.length() == 0) {
					curr_diagonal ++;
					continue;
				}

				aln = Aligner.alignNW(new StringPair(line_i,line_j));
				score = aln.getFinalScore();

				score /= (double) Math.min(line_i.length(),line_j.length());

				if (score > SIMILARITY_THRESH) {
					currentRow[idx] = 1;
					curr_diagonal++;
					if (curr_diagonal == longest_diagonal) {
						end_pts.add(line_i_idx);
						end_pts.add(line_j_idx);
					} else if (curr_diagonal > longest_diagonal) {
						longest_diagonal = curr_diagonal;
						end_pts.clear();
						end_pts.add(line_i_idx);
						end_pts.add(line_j_idx);
					}
				} else {
					curr_diagonal = 0;
				}
			}
		}

		for (Integer end_pt : end_pts) {
			end_pt++;
			for (int idx = 0; idx < longest_diagonal; idx++) {
				structure[end_pt - longest_diagonal + idx] = 'C';
			}
		}

		Utils.print2DMatrixInt(binary_matrix);
	}

	/**
	 * @param words
	 * @param structure
	 * @param firstLyricLine
	 * @param lastLyricLine
	 */
	private static void extractChorusViaRawScoreAln(List<String> words, char[] structure, int firstLyricLine,
			int lastLyricLine) {
		int [][] binary_matrix = new int[(lastLyricLine+1-firstLyricLine-MIN_DIST_BETWEEN_CHORUSES)][]; 
		int [] currentRow;

		int highest_diagonal_score = 0;
		double score;
		SortedMap<Integer, Pair<Integer, Integer>> candidate_chorus_end_indices = new TreeMap<Integer, Pair<Integer, Integer>>();

		int curr_diagonal_score, curr_diagonal_length, line_i_idx, line_j_idx;
		String line_i, line_j;
		Alignment aln;

		SequencePair.setCosts(1, -1, -1, -1);
		Aligner.setMinPercOverlap(.5);

		for (int offset = 0; offset < binary_matrix.length; offset++) {
			currentRow = new int[(lastLyricLine+1-firstLyricLine-MIN_DIST_BETWEEN_CHORUSES) - offset];
			binary_matrix[offset] = currentRow;

			curr_diagonal_score = 0;
			curr_diagonal_length = 0;
			for (int idx = 0; idx < currentRow.length; idx++) {
				line_i_idx = idx + firstLyricLine; // we're not aligning intro or outro lines so we have to account for the offset due to the intro
				line_j_idx = line_i_idx + offset + MIN_DIST_BETWEEN_CHORUSES;
				line_i = words.get(line_i_idx);
				line_j = words.get(line_j_idx);
				//				System.out.println("LINE i: " + line_i_idx + " " + line_i);
				//				System.out.println("LINE j: " + line_j_idx + " " + line_j);

				if (line_i.length() == 0 || line_j.length() == 0) {
					curr_diagonal_length++;
					continue;
				}

				aln = Aligner.alignNW(new StringPair(line_i,line_j));
				score = aln.getFinalScore();
				System.out.println(aln);

				curr_diagonal_score += score;
				curr_diagonal_length++;
				if (curr_diagonal_score < 0) {
					curr_diagonal_score = 0;
					curr_diagonal_length = 0;
				}

				currentRow[idx] = curr_diagonal_score;

				if (curr_diagonal_score > highest_diagonal_score * SIMILARITY_THRESH) {
					if (curr_diagonal_score > highest_diagonal_score) {
						System.out.println("NEW LONGEST");
						highest_diagonal_score = curr_diagonal_score;
					}
					if (!candidate_chorus_end_indices.containsKey(line_i_idx) ||
							candidate_chorus_end_indices.get(line_i_idx).getFirst() < curr_diagonal_score) {
						candidate_chorus_end_indices.put(line_i_idx, new Pair<Integer, Integer>(curr_diagonal_score, curr_diagonal_length));
					}

					if (!candidate_chorus_end_indices.containsKey(line_j_idx) ||
							candidate_chorus_end_indices.get(line_j_idx).getFirst() < curr_diagonal_score) {
						candidate_chorus_end_indices.put(line_j_idx, new Pair<Integer, Integer>(curr_diagonal_score, curr_diagonal_length));
					}
					System.out.println("CANDIDATE FOR LONGEST");
				}

			}
		}

		Pair<Integer, Integer> score_length;
		int length;
		for (Integer end_idx : candidate_chorus_end_indices.keySet()) {
			score_length = candidate_chorus_end_indices.get(end_idx);
			score = score_length.getFirst();
			length = score_length.getSecond();
			end_idx++;
			System.out.println("Candidate Score: " + score + " " + length + " " + end_idx);
			if (score > 0){//highest_diagonal_score * SIMILARITY_THRESH) {
				for (int idx = 0; idx < length; idx++) {
					structure[end_idx - length + idx] = 'C';
				}
			}
		}
		System.out.println("High Score: " + highest_diagonal_score);

		Utils.print2DMatrixInt(binary_matrix);
	}

	/**
	 * @param words
	 * @param structure
	 * @return
	 */
	private static int extractOutro(List<String> words, char[] structure) {
		int lastLyricLine;
		for (lastLyricLine = structure.length-1; lastLyricLine >= 0; lastLyricLine--) {
			if (words.get(lastLyricLine).length() == 0) {
				structure[lastLyricLine] = 'O';
			} else {
				break;
			}
		}
		return lastLyricLine;
	}

	/**
	 * @param words
	 * @param structure
	 */
	private static int extractIntro(List<String> words, char[] structure) {
		// Find intro as all opening lines w/o lyrics
		// TODO: intro should actually be a function of where the first verse or chorus starts
		int firstLyricLine;
		for (firstLyricLine = 0; firstLyricLine < structure.length; firstLyricLine++) {
			if (words.get(firstLyricLine).length() == 0) {
				structure[firstLyricLine] = 'I';
			} else {
				break;
			}
		}
		return firstLyricLine;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
