package condition;

public class BinaryMatch<T> extends DelayedConstraintCondition<T> {

	public BinaryMatch(int measure, double offset) {
		super(measure,offset);
	}

	@Override
	public boolean isSatisfiedBy(T t) {
		return t.equals(prevT);
	}
}
