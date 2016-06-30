package tabcomplete.alignment;

import java.util.Arrays;

import harmony.Chord;

public class ChordSequencePairAlignment extends Alignment {

	Chord[] first, second;
	
	public ChordSequencePairAlignment(Chord[] first, Chord[] second, double[] scores) {
		super(scores);
		this.first = first;
		this.second = second;
	}

	@Override
	public Chord[] getFirst() {
		return first;
	}

	@Override
	public Chord[] getSecond() {
		return second;
	}

	public String toString() {
		return Arrays.toString(first) + "\n" + Arrays.toString(second) + "\n" + Arrays.toString(scores);
	}
}
