package condition;

import harmony.Chord;

public class StrongResolution<T> extends ConstraintCondition<T> {

	@Override
	public boolean isSatisfiedBy(T t) {
		if (t instanceof Chord) {
			return ((Chord) t).toString().equals("C");
		}
		return false;
	}

	@Override
	protected String asString() {
		return "";
	}
}
