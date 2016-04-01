package condition;

import lyrics.Lyric;

public class RhymeCondition<T> extends ConstraintCondition<Lyric> {

	private double rhymeThreshold;

	public RhymeCondition(double threshold) {
		this.rhymeThreshold = threshold;
	}

	@Override
	public boolean isSatisfiedBy(Lyric t) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected String asString() {
		// TODO Auto-generated method stub
		return null;
	}

}
