package alignment;

import java.util.Arrays;

public class XBandedPairwiseAlignment extends XGenericPairwiseAlignment{

	private static final char DIAGONAL = 'D';
	private static final char UP = 'U';
	private static final char LEFT = 'L';
	private static final char INDEL_CHAR = '&';
	private static int MATCH_SCORE = 1;
	private static int MISMATCH_SCORE = -1;
	private static int GAP_OPEN_SCORE = -1;
	private static int GAP_EXTEND_SCORE = 0;
	
	String first;
	String second;
	int[] scores;
	int score;
	
	private static double minPercentOverlap = 0.00;
	
	public XBandedPairwiseAlignment(String lyric1, String lyric2) {
		boolean swap = false;
		
		if (lyric1.length() < lyric2.length()){
			swap = true;
			String tmp = lyric1;
			lyric1 = lyric2;
			lyric2 = tmp;
		}
		int rows = lyric1.length()+1;
		int cols = lyric2.length()+1;
		
		int rowsOverlap = (int) (rows * minPercentOverlap);
		if (rowsOverlap >= rows)
		{
			rowsOverlap = rows-1;
		} else if (rows-rowsOverlap > cols){
			rowsOverlap = rows - cols;
		}
		
		int colsOverlap = (int) (cols * minPercentOverlap);
		if (colsOverlap >= cols){
			colsOverlap = cols-1;
		} else if (cols - colsOverlap > rows){
			colsOverlap = cols - rows;
		}
		
		int[][] matrix = new int[rows][cols];
		char[][] backtrack = new char[rows][cols];
		
		matrix[0][0] = 0;
		backtrack[0][0] = 'S';
		for (int row = 1; row <= rowsOverlap; row++) {
			matrix[row][0] = matrix[row - 1][0]
					+ (backtrack[row - 1][0] == UP ? GAP_EXTEND_SCORE : GAP_OPEN_SCORE);
			backtrack[row][0] = UP;
		}
		
		for (int col = 1; col <= colsOverlap; col++) {
			matrix[0][col] = matrix[0][col - 1] + (backtrack[0][col - 1] == LEFT ? GAP_EXTEND_SCORE : GAP_OPEN_SCORE);
			backtrack[0][col] = LEFT;
		}
//		System.out.println("rowsOverlap:"+rowsOverlap);
//		System.out.println("colsOverlap:"+colsOverlap);
		
		int left, diag, up;
		for (int row = 1; row < rows; row++) {
			int colStart = Math.max(1, row-rowsOverlap);
			int colEnd = Math.min(cols, row+colsOverlap+1);
			for (int col = colStart; col < colEnd; col++) {
				diag = matrix[row-1][col-1] + (lyric1.charAt(row-1) == lyric2.charAt(col-1)? MATCH_SCORE : MISMATCH_SCORE);
				char leftBacktrack = backtrack[row][col - 1];
				left = (leftBacktrack == '\0' ? Integer.MIN_VALUE : matrix[row][col-1] + (leftBacktrack == LEFT ? GAP_EXTEND_SCORE : GAP_OPEN_SCORE));
				char upBacktrack = backtrack[row - 1][col];
				up = (upBacktrack == '\0' ? Integer.MIN_VALUE : matrix[row-1][col] + (upBacktrack == UP ? GAP_EXTEND_SCORE : GAP_OPEN_SCORE));
				if (diag >= up){
					if (diag >= left){
						matrix[row][col] = diag;
						backtrack[row][col] = DIAGONAL;
					} else {
						matrix[row][col] = left;
						backtrack[row][col] = LEFT;
					}
				} else {
					if (up >= left) {
						matrix[row][col] = up;
						backtrack[row][col] = UP;
					} else {
						matrix[row][col] = left;
						backtrack[row][col] = LEFT;
					}
				}
			}
		}
		
//		printMatrixX(backtrack);
		StringBuilder first = new StringBuilder();
		StringBuilder second = new StringBuilder();
		
		int i = lyric1.length();
		int j = lyric2.length();
		score = matrix[i][j];
		int[] scores = new int[i+j];
		int idx = 0;
		while(i > 0 && j > 0){
			scores[idx++] = matrix[i][j];
			switch(backtrack[i][j]){
			case DIAGONAL:
				i--;
				first.append(lyric1.charAt(i));
				j--;
				second.append(lyric2.charAt(j));
				break;
			case UP:
				i--;
				first.append(lyric1.charAt(i));
				second.append(INDEL_CHAR);
				break;
			case LEFT:
				first.append(INDEL_CHAR);
				j--;
				second.append(lyric2.charAt(j));
				break;
			}
		}
		
		if(swap){
			this.first = second.reverse().toString();
			this.second = first.reverse().toString();
		} else {
			this.first = first.reverse().toString();
			this.second = second.reverse().toString();
		}
		this.scores = new int[this.first.length()];
		idx--;
		for(int k = 0; k <= idx; k++)
		{
			this.scores[k] = scores[idx-k];
		}
	}

	public void printMatrixX(char[][] matrix) {
		System.out.println("___________");
		for (char[] row : matrix) {
			System.out.print("|");
			for (char col : row) {
				System.out.print(col == '\0' ? ' ' : col);
			}
			System.out.println("|");
		}
		System.out.println("___________");
	}
	
	public int getScore() {
		return score;
	}

	public String getSecond() {
		return second;
	}

	public String getFirst() {
		return first;
	}

	public static void main(String[] args){
		XBandedPairwiseAlignment aln = new XBandedPairwiseAlignment("Hello","Real-o");
		
		System.out.println(aln.getFirst());
		System.out.println(aln.getSecond());
		System.out.println(Arrays.toString(aln.getScores()));
	}

	public int[] getScores() {
		return scores;
	}

	public static void setMinPercOverlap(double minPercOverlap) {
		minPercentOverlap = minPercOverlap; 
	}
}
