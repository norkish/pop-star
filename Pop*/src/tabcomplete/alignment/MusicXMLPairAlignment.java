package tabcomplete.alignment;

import java.util.Arrays;

public class MusicXMLPairAlignment extends Alignment {

	int[] first,second;
	
	public MusicXMLPairAlignment(int[] firstArray, int[] secondArray, double[] scores) {
		super(scores);
		this.first = firstArray;
		this.second = secondArray;
	}

	@Override
	public int[] getFirst() {
		return first;
	}

	@Override
	public int[] getSecond() {
		return second;
	}

	public String toString() {
		return Arrays.toString(first) + "\n" + Arrays.toString(second) + "\n" + Arrays.toString(scores);
	}
}
