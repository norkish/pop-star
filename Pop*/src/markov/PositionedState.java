package markov;

public class PositionedState {

	private int stateIndex;
	private int position;

	public PositionedState(int position, int stateIndex) {
		this.position = position;
		this.stateIndex = stateIndex;
	}

	public int getPosition() {
		return this.position;
	}

	public int getStateIndex() {
		return this.stateIndex;
	}

}
