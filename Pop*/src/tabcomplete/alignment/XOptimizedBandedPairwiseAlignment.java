package tabcomplete.alignment;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import tabcomplete.rawsheet.LyricSheet;
import tabcomplete.rawsheet.RawDataLoader;
import tabcomplete.utils.StopWatch;
import tabcomplete.utils.Utils;

public class XOptimizedBandedPairwiseAlignment extends XGenericPairwiseAlignment {

	private static final char DIAGONAL = 'D';
	private static final char UP = 'U';
	private static final char LEFT = 'L';
	private static final char INDEL_CHAR = '&';
	private static int MATCH_SCORE = 1;
	private static int MISMATCH_SCORE = -1;
	private static int GAP_OPEN_SCORE = -2;
	private static int GAP_EXTEND_SCORE = 0;

	String first;
	String second;
	int[] scores;

	private static double minPercentOverlap = 0.50;
	int rowsOverlap, colsOverlap;
	private int cols, matrixCols, matrixCols_cols, cols_colsOverlap;

	public XOptimizedBandedPairwiseAlignment(String lyric1, String lyric2) {

		int rows = lyric1.length() + 1;
		cols = lyric2.length() + 1;

		rowsOverlap = (int) (rows * minPercentOverlap);
		if (rowsOverlap >= rows) {
			rowsOverlap = rows - 1;
		} else if (rows - rowsOverlap > cols) {
			rowsOverlap = rows - cols;
		}

		colsOverlap = (int) (cols * minPercentOverlap);
		int ban_inverse_len = cols - colsOverlap;
		if (colsOverlap >= cols) {
			colsOverlap = cols - 1;
		} else if (ban_inverse_len > rows) {
			colsOverlap = cols - rows;
		}
		int colsOverlapPlus1 = colsOverlap + 1;
		matrixCols = Math.min(cols, rowsOverlap + colsOverlapPlus1);
		matrixCols_cols = matrixCols - cols;
		cols_colsOverlap = cols - colsOverlap;

		int[][] matrix = new int[rows][matrixCols];
		char[][] backtrack = new char[rows][matrixCols];

		matrix[0][0] = 0;
		backtrack[0][0] = 'S';
		if (rowsOverlap > 0) {
			matrix[1][0] = matrix[0][0] + GAP_EXTEND_SCORE;
			backtrack[1][0] = UP;
		}

		char[] currBackTrackRow = backtrack[0], prevBacktrackRow;
		int[] currMatrixRow = matrix[0], prevMatrixRow;
		if (colsOverlap > 0) {
			currMatrixRow[1] = currMatrixRow[0] + GAP_EXTEND_SCORE;
			currBackTrackRow[1] = LEFT;
		}

		for (int row = 1; row <= rowsOverlap; row++) {
			matrix[row][0] = matrix[row - 1][0] + GAP_EXTEND_SCORE;
			backtrack[row][0] = UP;
		}

		for (int col = 1; col <= colsOverlap; col++) {
			currMatrixRow[col] = currMatrixRow[col - 1] + GAP_EXTEND_SCORE;
			currBackTrackRow[col] = LEFT;
		}
		
		int i = rows - 1;
		int j = cols - 1;
		int left, diag, up, matrixCol, matrixColAbove, colStart, colEnd, row_1;
		boolean headerRow, headerCol;
		for (int row = 1; row < rows; row++) {
			colStart = Math.max(1, row - rowsOverlap);
			colEnd = Math.min(cols, row + colsOverlapPlus1);
			row_1 = row - 1;
			headerRow = row < ban_inverse_len;
			headerCol = row > rowsOverlap;
			currBackTrackRow = backtrack[row];
			prevBacktrackRow = backtrack[row_1];
			prevMatrixRow = matrix[row_1];
			currMatrixRow = matrix[row];
			for (int col = colStart; col < colEnd; col++) {
				matrixCol = getMatrixColumn(row, col);
				matrixColAbove = getMatrixColumn(row_1, col);

				diag = prevMatrixRow[matrixColAbove - 1]
						+ (TokenComparator.matchCharactersGenerally(lyric1.charAt(row_1),lyric2.charAt(col - 1)) ? MATCH_SCORE : MISMATCH_SCORE);

				if (headerCol && col == colStart)
					left = Integer.MIN_VALUE;
				else // insert gap into seq1
					left = currMatrixRow[matrixCol - 1]
							+ (currBackTrackRow[matrixCol - 1] == LEFT || row == i? GAP_EXTEND_SCORE : GAP_OPEN_SCORE);

				if (headerRow && col == colEnd - 1)
					up = Integer.MIN_VALUE;
				else {
					up = prevMatrixRow[matrixColAbove]
							+ (prevBacktrackRow[matrixColAbove] == UP || col == j? GAP_EXTEND_SCORE : GAP_OPEN_SCORE);
				}

				if (diag >= up) {
					if (diag >= left) {
						currMatrixRow[matrixCol] = diag;
						currBackTrackRow[matrixCol] = DIAGONAL;
					} else {
						currMatrixRow[matrixCol] = left;
						currBackTrackRow[matrixCol] = LEFT;
					}
				} else {
					if (up >= left) {
						currMatrixRow[matrixCol] = up;
						currBackTrackRow[matrixCol] = UP;
					} else {
						currMatrixRow[matrixCol] = left;
						currBackTrackRow[matrixCol] = LEFT;
					}
				}
			}
		}
//		Aligner.printMatrixX(matrix);

		StringBuilder first = new StringBuilder();
		StringBuilder second = new StringBuilder();

		matrixCol = getMatrixColumn(i, j);
		int[] scores = new int[i + j];
		int idx = 0;
		while (i > 0 || j > 0) {
			scores[idx++] = matrix[i][matrixCol];
			switch (backtrack[i][matrixCol]) {
			case DIAGONAL:
				i--;
				first.append(lyric1.charAt(i));
				j--;
				second.append(lyric2.charAt(j));
				matrixCol = getMatrixColumn(i, j);
				break;
			case UP:
				i--;
				first.append(lyric1.charAt(i));
				second.append(INDEL_CHAR);
				matrixCol = getMatrixColumn(i, j);
				break;
			case LEFT:
				first.append(INDEL_CHAR);
				j--;
				second.append(lyric2.charAt(j));
				matrixCol--;
				break;
			}
		}

		this.first = first.reverse().toString();
		this.second = second.reverse().toString();
		
		this.scores = new int[idx];
		idx--;
		for (int k = 0; k <= idx; k++) {
			this.scores[k] = scores[idx - k];
		}
	}

	/**
	 * @param rowsOverlap
	 * @param row
	 * @param col
	 * @return
	 */
	public int getMatrixColumn(int row, int col) {
		if (row <= rowsOverlap) {
			return col;
		} else if (row >= cols_colsOverlap) {
			return matrixCols_cols + col;
		} else {
			return col - row + rowsOverlap;
		}
	}

	public int[] getScores() {
		return scores;
	}

	public int getScore() {
		return scores[scores.length - 1];
	}

	public String getSecond() {
		return second;
	}

	public String getFirst() {
		return first;
	}

	public static void main(String[] args) throws FileNotFoundException, IOException {

		Map<String, Map<String, List<LyricSheet>>> lyricSheets = RawDataLoader.loadLyricSheets();

		StopWatch optimizedWatch = new StopWatch();
		StopWatch watch = new StopWatch();
		StopWatch unBandedWatch = new StopWatch();

		System.out.println(
				"minPercOverlap\tbanded_identity\tband_noband_identity\tunbandedTime\tbandedTime\toptimizedBandTime\ttotalAlns");
		// for (int i = 0; i < tests.length; i += 2) {
		for (double minPercOverlap = .03125; minPercOverlap <= 1.01; minPercOverlap *= 2) {
			double correct = 0.;
			double consistent = 0.;
			double total = 0.;
			XOptimizedBandedPairwiseAlignment.setMinPercOverlap(minPercOverlap);
			XBandedPairwiseAlignment.setMinPercOverlap(minPercOverlap);
			optimizedWatch.reset();
			watch.reset();
			unBandedWatch.reset();
			int iterations = 10;
			for (int iters = 0; iters < iterations; iters++) {
				for (String key : lyricSheets.keySet()) {
					// System.out.println(key + ":" + lyricSheets.get(key).size());
					for (String key2 : lyricSheets.get(key).keySet()) {
						List<LyricSheet> tests = lyricSheets.get(key).get(key2);
						// System.out.println("\t" + key2 + ":" + tests.size());
						for (int i = 0; i < tests.size() - 1; i++) {
							String lyrics1 = tests.get(i).getLyrics().toLowerCase();
							String lyrics2 = tests.get(i + 1).getLyrics().toLowerCase();
							optimizedWatch.start();
							XOptimizedBandedPairwiseAlignment aln = new XOptimizedBandedPairwiseAlignment(lyrics1,
									lyrics2);
							optimizedWatch.stop();
							watch.start();
							XBandedPairwiseAlignment aln2 = new XBandedPairwiseAlignment(lyrics1, lyrics2);
							watch.stop();
							unBandedWatch.start();
							XPairwiseAlignment aln3 = new XPairwiseAlignment(lyrics1, lyrics2);
							unBandedWatch.stop();

							boolean print = false;
							if (compareAlignments(aln, aln2))
								consistent++;
							else {
								System.err.println("WARNING: Difference between banded alns - ");
								print = true;
							}
							if (compareAlignments(aln, aln3))
								correct++;
							else if (minPercOverlap == 1.0) {
								Utils.promptEnterKey(
										"WARNING: Difference between banded (with full band) and unbanded alns (min%Overlap="
												+ minPercOverlap + ") - ");
								print = true;
							}

							if (print) {
								System.out.println("\t" + aln.getFirst().replaceAll("\n", "\\n"));
								System.out.println("\t" + aln.getSecond().replaceAll("\n", "\\n"));
								System.out.println("\t" + Arrays.toString(aln.getScores()));
								System.out.println("\t" + aln2.getFirst().replaceAll("\n", "\\n"));
								System.out.println("\t" + aln2.getSecond().replaceAll("\n", "\\n"));
								System.out.println("\t" + Arrays.toString(aln2.getScores()));
							}

							total++;
						}
					}
				}
			}
			System.out.print(minPercOverlap);
			System.out.print("\t" + (consistent / total));
			System.out.print("\t" + (correct / total));
			System.out.print("\t" + unBandedWatch.elapsedTime() / iterations);
			System.out.print("\t" + watch.elapsedTime() / iterations);
			System.out.print("\t" + optimizedWatch.elapsedTime() / iterations);
			System.out.print("\t" + total / iterations);
			System.out.println();
		}

	}

	public static void setMinPercOverlap(double minPercOverlap) {
		minPercentOverlap = minPercOverlap;
	}

	/**
	 * @param aln
	 * @param aln3
	 * @return
	 */
	public static boolean compareAlignments(XGenericPairwiseAlignment aln, XGenericPairwiseAlignment aln3) {
		String alnFirst = aln.getFirst();
		String alnSecond = aln.getSecond();
		int[] alnScores = aln.getScores();
		String aln2First = aln3.getFirst();
		String aln2Second = aln3.getSecond();
		int[] aln2Scores = aln3.getScores();

		boolean same = true;

		same &= (alnFirst.length() == aln2First.length() && alnSecond.length() == aln2Second.length());

		for (int i = 0; same && i < alnFirst.length(); i++) {
			same &= (alnFirst.charAt(i) == aln2First.charAt(i));
			same &= (alnSecond.charAt(i) == aln2Second.charAt(i));
			same &= (alnScores[i] == aln2Scores[i]);
		}

		// if (same) {
		// System.out.println("Same");
		// } else {
		// System.out.println(alnFirst);
		// System.out.println(alnSecond);
		// System.out.println(Arrays.toString(alnScores));
		// System.out.println(aln2First);
		// System.out.println(aln2Second);
		// System.out.println(Arrays.toString(aln2Scores));
		// }
		return same;
	}

	public static void printMatrixS(int[][] matrix) {
		System.out.println("___________");
		for (int[] row : matrix) {
			System.out.print("|");
			for (int col : row) {
				System.out.print(col + "\t");
			}
			System.out.println("|");
		}
		System.out.println("___________");		
	}
}
