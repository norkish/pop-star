package condition;

import java.util.HashSet;
import java.util.Set;

public class ExactUnaryMatch<T> extends ConstraintCondition<T> {

	private Set<T> acceptableStates;

	public ExactUnaryMatch(T[] acceptableStates) {
		this.acceptableStates = new HashSet<T>();
		for (int j = 0; j < acceptableStates.length; j++) {
			this.acceptableStates.add(acceptableStates[j]);
		}
	}

	@Override
	public boolean isSatisfiedBy(T t) {
		return acceptableStates.contains(t);
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
