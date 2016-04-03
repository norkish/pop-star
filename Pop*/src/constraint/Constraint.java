package constraint;
import condition.ConstraintCondition;
import utils.Utils;

public class Constraint<T> {

	public static final int FINAL_POSITION = -1;
	
	public boolean getDesiredConditionState() {
		return desiredConditionState;
	}

	public int getPosition() {
		return position;
	}

	public ConstraintCondition<T> getCondition() {
		return condition;
	}

	// We allow for a constraint to enforce a condition or the negation of the condition
	private boolean desiredConditionState;
	private int position;
	protected ConstraintCondition<T> condition;

	public Constraint(int i, ConstraintCondition<T> condition, boolean desiredConditionState) {
		this.position = i;
		this.condition = condition;
		this.desiredConditionState = desiredConditionState;
	}

	@Override
	public String toString() {
		StringBuilder str = new StringBuilder();
		
		str.append("token in ");
		str.append(Utils.getPositionString(position));
		str.append(" must ");
		if (!desiredConditionState) 
			str.append("not ");
		str.append("be a ");
		str.append(condition);
		
		return str.toString();
	}
}
