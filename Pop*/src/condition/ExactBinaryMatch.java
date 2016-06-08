package condition;

public class ExactBinaryMatch<T> extends DelayedConstraintCondition<T> {

	public ExactBinaryMatch(int line, int pos) {
		super(line,pos);
	}

	@Override
	public boolean isSatisfiedBy(T t) {
		return t.equals(prevT);
	}
}
