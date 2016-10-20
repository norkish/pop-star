package tabcomplete.alignment;

import java.util.Arrays;

public class TemporalSequencePairAlignment extends Alignment{

	private Double[] first, second;

	public TemporalSequencePairAlignment(Double[] first, Double[] second, double[] scores) {
		super(scores);
		this.first = first;
		this.second = second;
	}

	@Override
	public Double[] getFirst() {
		return first;
	}

	@Override
	public Double[] getSecond() {
		return second;
	}

	public String toString() {
		return Arrays.toString(first) + "\n" + Arrays.toString(second) + "\n" + Arrays.toString(scores);
	}
}
