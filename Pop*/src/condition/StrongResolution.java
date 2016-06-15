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

	@Override
	public boolean equals(Object other) {
		return true;
	}

	@Override
	public int hashCode() {
		return 31;
	}
}
