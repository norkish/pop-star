package alignment;

import java.util.Arrays;

public class StringPairAlignment extends Alignment {

	String first;
	String second;
	
	public StringPairAlignment(String string, String string2, double[] scores) {
		super(scores);
		this.first = string;
		this.second = string2;
	}

	@Override
	public String getFirst() {
		return first;
	}
	
	@Override
	public String getSecond() {
		return second;
	}
	
	public String toString() {
		return first + "\n" + second + "\n" + Arrays.toString(scores);
	}
}
