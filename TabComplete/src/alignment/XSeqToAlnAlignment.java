package alignment;

import java.util.LinkedHashMap;
import java.util.Map;

public class XSeqToAlnAlignment {

	String[] newAlignedSeqs;
	String alignedLyricSeq;

	private static final char DIAGONAL = 'D';
	private static final char UP = 'U';
	private static final char LEFT = 'L';
	private static final char INDEL_CHAR = '&';
	private static int MATCH_SCORE = 1;
	private static int MISMATCH_SCORE = -1;
	private static int GAP_OPEN_SCORE = -2;
	private static int GAP_EXTEND_SCORE = 0;

	private static double minPercentOverlap = 0.50;
	int rowsOverlap, colsOverlap;
	private int cols, matrixCols, matrixCols_cols, cols_colsOverlap;

	public XSeqToAlnAlignment(String[] alignedSeqs, String lyric) {
		int numSeqs = alignedSeqs.length;
		newAlignedSeqs = new String[numSeqs];

		int rows = alignedSeqs[0].length() + 1;
		cols = lyric.length() + 1;

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
		int left, diag, up, matrixCol, matrixColAbove, colStart, colEnd, row_1, cost, gapCharCount, nonGapCharCount;
		char charA, charB;
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

				cost = 0;
				gapCharCount = 0;
				charB = lyric.charAt(col-1);
				for (String alnSeq: alignedSeqs) {
					charA = alnSeq.charAt(row_1);
					if (charA == INDEL_CHAR){
						gapCharCount++;
					} else if (TokenComparator.matchCharactersGenerally(charA, charB)) {
						cost += MATCH_SCORE;
					} else {
						cost += MISMATCH_SCORE;
					}
				}
				
				// score for matching is the match/mismatch costs + #gaps * gap extend ost
				diag = prevMatrixRow[matrixColAbove - 1] + cost + gapCharCount * GAP_EXTEND_SCORE;

				// can't go up from the last column in the band (unless it's a row where the band overlaps the right boundary)
				if (headerRow && col == colEnd - 1)
					up = Integer.MIN_VALUE;
				else {// score for inserting gap into new lyric is #nongaps * GAP_EXTEND_SCORE
					// in other words, we only penalize adding a gap to align with a nongap 
					up = prevMatrixRow[matrixColAbove] + (numSeqs - gapCharCount) * 
							(prevBacktrackRow[matrixColAbove] == UP || col == j? GAP_EXTEND_SCORE : GAP_OPEN_SCORE);
				}
				
				if (headerCol && col == colStart)
					left = Integer.MIN_VALUE;
				else { // score for inserting gap into array of aligned lyrics...
					// gotta look back to see if we're opening a gap or extending one
					// could be a gap we created during this aln or a gap created during a prev one
					
					// if we prev inserted gaps during this alignment at the previous step
					// or if we've already aligned all of the new lyric characters
					if (currBackTrackRow[matrixCol - 1] == LEFT || row == i) {
						//Definitely gap extend for all of them
						cost = numSeqs * GAP_EXTEND_SCORE;
					} else { // otherwise
						gapCharCount = 0;
						for (String alnSeq: alignedSeqs) {
							charA = alnSeq.charAt(row);
							if (charA == INDEL_CHAR){
								gapCharCount++;
							}
						}
						// gotta check if the previous character for each aligned seq was a gap char
						// if so, then it is a gap extend
						// if not, then it is a gap open
						cost = GAP_EXTEND_SCORE * gapCharCount + GAP_OPEN_SCORE * (numSeqs - gapCharCount); 
					}
					left = currMatrixRow[matrixCol - 1] + cost;
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

		StringBuilder[] newAlnSeqBldrs = new StringBuilder[numSeqs];
		for (int k = 0; k < newAlnSeqBldrs.length; k++) {
			newAlnSeqBldrs[k] = new StringBuilder();
		}
		StringBuilder lyrSeqBldr = new StringBuilder();

		matrixCol = getMatrixColumn(i, j);
		
		while (i > 0 || j > 0) {
			switch (backtrack[i][matrixCol]) {
			case DIAGONAL:
				i--;
				for (int k = 0; k < numSeqs; k++) {
					newAlnSeqBldrs[k].append(alignedSeqs[k].charAt(i));
				}
				j--;
				lyrSeqBldr.append(lyric.charAt(j));
				matrixCol = getMatrixColumn(i, j);
				break;
			case UP:
				i--;
				for (int k = 0; k < numSeqs; k++) {
					newAlnSeqBldrs[k].append(alignedSeqs[k].charAt(i));
				}
				lyrSeqBldr.append(INDEL_CHAR);
				matrixCol = getMatrixColumn(i, j);
				break;
			case LEFT:
				for (int k = 0; k < numSeqs; k++) {
					newAlnSeqBldrs[k].append(INDEL_CHAR);
				}
				j--;
				lyrSeqBldr.append(lyric.charAt(j));
				matrixCol--;
				break;
			}
		}

		for (int k = 0; k < numSeqs; k++) {
			newAlignedSeqs[k] = newAlnSeqBldrs[k].reverse().toString();
		}
		alignedLyricSeq = lyrSeqBldr.reverse().toString();
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

	public String[] getNewAlignedSeqs() {
		return newAlignedSeqs;
	}

	public String getAlignedLyricSeq() {
		return alignedLyricSeq;
	}
	
	public static void main(String[] args){
//		String[] alns = new String[]{"y#(woah)##They","y#(woah)##They"};
//		String newSeq = "y#And they";
		String[] alns = new String[]{"u've ","u&&& ","u&&& ","u&&& "};
		String newSeq = "u've ";
		XSeqToAlnAlignment.setMinPercOverlap(.75);
		XSeqToAlnAlignment aln = new XSeqToAlnAlignment(alns, newSeq);
		
		for(String s: aln.getNewAlignedSeqs())
			System.out.println(s);
		System.out.println(aln.getAlignedLyricSeq());
		
		Map<Character, Integer> test = new LinkedHashMap<Character, Integer>();
		
		test.put('&', 1);
		test.put('i', 1);
		test.put('&', 2);
		test.put('i', 2);

		for(Character key: test.keySet()){
			System.out.println(key);
		}
		
//		System.out.println("GROUP");
//		System.out.println(Integer.toBinaryString(Character.getType('a')));
//		System.out.println(Character.getType('a'));
//		System.out.println(Character.getType('A'));
//		
//		System.out.println("GROUP");
//		System.out.println(Integer.toBinaryString(Character.getType('1')));
//		System.out.println(Character.getType('1'));
//		System.out.println(Character.getType('2'));
//		System.out.println(Character.getType('3'));
//		System.out.println(Character.getType('0'));
//
//		System.out.println("GROUP");
//		System.out.println(Integer.toBinaryString(Character.getType('.')));
//		System.out.println(Integer.toBinaryString(Character.getType(')')));
//		System.out.println(Character.getType('.'));
//		System.out.println(Character.getType(','));
//		System.out.println(Character.getType(';'));
//		System.out.println(Character.getType(')'));
//
//		System.out.println("GROUP");
//		System.out.println(Integer.toBinaryString(Character.getType(' ')));
//		System.out.println(Character.getType(' '));
//		System.out.println(Character.isWhitespace(' '));
//		System.out.println(Integer.toBinaryString(Character.getType('\n')));
//		System.out.println(Character.getType('\n'));
//		System.out.println(Character.isWhitespace('\n'));
//		System.out.println(Character.getType('\t'));
//		System.out.println(Character.isWhitespace('\t'));
	}

	static void setMinPercOverlap(double d) {
		minPercentOverlap = d;
	}
}
