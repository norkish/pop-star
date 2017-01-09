package condition;

import data.MusicXMLParser.Harmony;

public class StrongResolution<T> extends ConstraintCondition<T> {

	@Override
	public boolean isSatisfiedBy(T t) {
		if (t instanceof Harmony) {
			return ((Harmony) t).toString().equals("C");
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
