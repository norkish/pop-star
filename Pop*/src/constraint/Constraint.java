package constraint;
import markov.NHMM;

public abstract class Constraint<T> {

	public static final int LAST = -1;
	public static final int ALL_POSITIONS = Integer.MAX_VALUE;
	
	// We allow for a constraint to enforce a condition or the negation of the condition
	protected boolean desiredConditionState;
	
	abstract public void constrain(NHMM<T> nhmm);

	abstract protected String asString();
	
	public String toString()
	{
		StringBuilder str = new StringBuilder();
		
		str.append(this.getClass());
		str.append(" - ");
		str.append(this.asString());
		
		return str.toString();
	}

}
