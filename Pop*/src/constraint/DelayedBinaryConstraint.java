package constraint;

import condition.ConstraintCondition;
import markov.NHMM;

public class DelayedBinaryConstraint<T> extends Constraint<T> {

	private int line;
	private int prevPos;
	private int pos;

	/**
	 * A DelayedMatchConstraint is a constraint that cannot be fully defined until a previous
	 * line has been generated (i.e., the constraint depends on the value of a generated
	 * element)
	 * @param line the previous line from which to extract the constraint
	 * @param prevPos the position in the previous line from which to extract the match
	 * @param pos the position to constrain in the constrained line
	 * @param condition 
	 */
	public DelayedBinaryConstraint(int line, int prevPos, int pos, ConstraintCondition<T> condition, boolean desiredConditionState) {
		this.line = line;
		this.prevPos = prevPos;
		this.pos = pos;
		this.desiredConditionState = desiredConditionState;
	}

	@Override
	public void constrain(NHMM<T> nhmm) {
		// TODO Auto-generated method stub

	}

	@Override
	protected String asString() {
		// TODO Auto-generated method stub
		return null;
	}

}
