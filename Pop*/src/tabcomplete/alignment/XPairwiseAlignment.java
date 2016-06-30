package tabcomplete.alignment;

import java.util.Arrays;

public class XPairwiseAlignment extends XGenericPairwiseAlignment {

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
	
	double minPercentOverlap = 0.0;
	
	public XPairwiseAlignment(String lyric1, String lyric2) {
		boolean swap = false;
		
		if (lyric1.length() < lyric2.length()){
			swap = true;
			String tmp = lyric1;
			lyric1 = lyric2;
			lyric2 = tmp;
		}
		int rows = lyric1.length()+1;
		int cols = lyric2.length()+1;
		
		int[][] matrix = new int[rows][cols];
		char[][] backtrack = new char[rows][cols];
		
		matrix[0][0] = 0;
		for (int row = 1; row < rows; row++) {
			matrix[row][0] = matrix[row - 1][0]
					+ (backtrack[row - 1][0] == UP ? GAP_EXTEND_SCORE : GAP_OPEN_SCORE);
			backtrack[row][0] = UP;
		}
		
		for (int col = 1; col < cols; col++) {
			matrix[0][col] = matrix[0][col - 1] + (backtrack[0][col - 1] == LEFT ? GAP_EXTEND_SCORE : GAP_OPEN_SCORE);
			backtrack[0][col] = LEFT;
		}
		
		
		int left, diag, up;
		for (int row = 1; row < rows; row++) {
			for (int col = 1; col < cols; col++) {
				diag = matrix[row-1][col-1] + (lyric1.charAt(row-1) == lyric2.charAt(col-1)? MATCH_SCORE : MISMATCH_SCORE);
				left = matrix[row][col-1] + (backtrack[row][col - 1] == LEFT ? GAP_EXTEND_SCORE : GAP_OPEN_SCORE);
				up = matrix[row-1][col] + (backtrack[row - 1][col] == UP ? GAP_EXTEND_SCORE : GAP_OPEN_SCORE);
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
		XPairwiseAlignment aln = new XPairwiseAlignment("I've got a lovely bunch of coconuts", "Hives hot need gloves with lunch o' donuts");
		
		System.out.println(aln.getFirst());
		System.out.println(aln.getSecond());
		System.out.println(Arrays.toString(aln.getScores()));
	}

	public int[] getScores() {
		return scores;
	}
}
