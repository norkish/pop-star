package constraint;
import java.util.HashSet;
import java.util.Set;

import condition.ConstraintCondition;
import markov.NHMM;
import markov.PositionedState;

public class UnaryConstraint<T> extends Constraint<T> {

	private int position;
	private ConstraintCondition<T> condition;
	
	public UnaryConstraint(int i, ConstraintCondition<T> condition, boolean desiredConditionState) {
		position = i;
		this.condition = condition;
		this.desiredConditionState = desiredConditionState;
	}

	@Override
	public void constrain(NHMM<T> nhmm) {
		Set<PositionedState> posStateToRemove = new HashSet<PositionedState>();
		T[] states = nhmm.getStates();
		for (int stateIndex = 0; stateIndex < states.length; stateIndex++) {
			// if the considered state satisfies/dissatisfies the condition contrary to what we wanted
			if(condition.isSatisfiedBy(states[stateIndex]) ^ desiredConditionState)
			{
				// remove it
				posStateToRemove.addAll(nhmm.removeState(position, stateIndex));
			}
		}
		
		while(!posStateToRemove.isEmpty())
		{
			PositionedState stateToRemove = posStateToRemove.iterator().next();
			posStateToRemove.remove(stateToRemove);
			posStateToRemove.addAll(nhmm.removeState(stateToRemove.getPosition(), stateToRemove.getStateIndex()));
		}
	}

	@Override
	protected String asString() {
		StringBuilder str = new StringBuilder();
		
		str.append("@pos");
		str.append(position);
		str.append(":");
		
		if(desiredConditionState)
			str.append(" NEGATE ");
		str.append(condition.toString());
		
		return str.toString();
	}

}
