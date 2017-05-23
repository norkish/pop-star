package tabcomplete.alignment;

public abstract class Alignment {
	
	double[] scores;
	double[][] matrix;

	public abstract Object getFirst();

	public abstract Object getSecond();

	public Alignment(double[] scores) {
		this.scores = scores;
	}
	
	public double[] getScores() {
		return scores;
	}

	public double getFinalScore() {
		return scores[scores.length-1];
	}
	
	public double[][] getMatrix() {
		return matrix;
	}
}
