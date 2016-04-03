package condition;

public class ExactBinaryMatch<T> extends DelayedConstraintCondition<T> {

	public ExactBinaryMatch(int line, int pos) {
		super(line,pos);
	}

	@Override
	public boolean isSatisfiedBy(T t) {
		return t.equals(prevT);
	}

	@Override
	protected String asString() {
		return "";
//		StringBuilder str = new StringBuilder();
//		
//		str.append("[");
//		boolean first = true;
//		for (T t : acceptableStates) {
//			if(!first)
//			{
//				str.append(',');
//			}
//			else
//			{
//				first = false;
//			}
//			str.append(t);
//		}
//		
//		str.append("]");
//		
//		return str.toString();
	}
}
