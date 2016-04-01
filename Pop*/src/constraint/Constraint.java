package constraint;
import condition.ConstraintCondition;

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
		str.append(getPositionString(position));
		str.append(" must ");
		if (!desiredConditionState) 
			str.append("not ");
		str.append("be a ");
		str.append(condition);
		
		return str.toString();
	}
	
	protected String getPositionString(int i)
	{
		String posStr = "the ";
		if (i == FINAL_POSITION)
		{
			posStr += "LAST";
		}
		else if (i == 0)
		{
			posStr += "FIRST";
		}
		else if (i == 1)
		{
			posStr += "SECOND";
		}
		else if (i == 2)
		{
			posStr += "THIRD";
		}
		else
		{
			posStr += i + "TH";
		}
		
		return posStr + " position";
	}
}
