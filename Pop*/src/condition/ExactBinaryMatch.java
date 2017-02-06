package condition;

public class ExactBinaryMatch<T> extends DelayedConstraintCondition<T> {

	public ExactBinaryMatch(int measure, double offset) {
		super(measure,offset);
	}

	@Override
	public boolean isSatisfiedBy(T t) {
		return t.equals(prevT);
	}
}
