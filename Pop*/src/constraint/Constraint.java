package constraint;
import java.io.Serializable;
import java.util.List;

import composition.Measure;
import condition.ConstraintCondition;
import condition.DelayedConstraintCondition;

public class Constraint<T> implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final int FINAL_POSITION = -1;
	public static final double ALL_POSITIONS = -2.0;
	public static final String CONSTRAINT_ANNOTATIONS_DIR = "wikifonia_rhyme_annotations";
	
	public boolean getDesiredConditionState() {
		return desiredConditionState;
	}

	public ConstraintCondition<T> getCondition() {
		return condition;
	}

	// We allow for a constraint to enforce a condition or the negation of the condition
	private boolean desiredConditionState;
	protected ConstraintCondition<T> condition;

	public Constraint(ConstraintCondition<T> condition, boolean desiredConditionState) {
		this.condition = condition;
		this.desiredConditionState = desiredConditionState;
	}

	public static <T> void reifyConstraints(List<Constraint<T>> constraints, List<Measure> measures) {
		for (Constraint<T> constraint : constraints) {
			ConstraintCondition<T> condition = constraint.getCondition();
			if(condition instanceof DelayedConstraintCondition)
			{
				((DelayedConstraintCondition<T>) condition).reify(measures);
			}
		}
	}
	
	@Override
	public String toString() {
		StringBuilder str = new StringBuilder();
		
		str.append("token must ");
		if (!desiredConditionState) 
			str.append("not ");
		str.append("be a ");
		str.append(condition);
		
		return str.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((condition == null) ? 0 : condition.hashCode());
		result = prime * result + (desiredConditionState ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof Constraint))
			return false;
		@SuppressWarnings("unchecked")
		Constraint<T> other = (Constraint<T>) obj;
		if (condition == null) {
			if (other.condition != null)
				return false;
		} else if (!condition.equals(other.condition))
			return false;
		if (desiredConditionState != other.desiredConditionState)
			return false;
		return true;
	}
}
