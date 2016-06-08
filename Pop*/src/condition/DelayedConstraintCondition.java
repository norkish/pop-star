package condition;

import java.util.List;

import constraint.Constraint;
import utils.Utils;

public abstract class DelayedConstraintCondition<T> extends ConstraintCondition<T> {
	protected T prevT = null;
	protected int prevLineNumber;
	protected int prevPos;
	
	public DelayedConstraintCondition(int line, int pos) {
		this.prevLineNumber = line;
		this.prevPos = pos;
	}

	/**
	 * A DelayedMatchConstraint is a constraint that cannot be fully defined until a previous
	 * line has been generated (i.e., the constraint depends on the value of a generated
	 * element)
	 * @param prevLineNumber the previous line from which to extract the constraint
	 * @param prevPos the position in the previous line from which to extract the match
	 * @param pos the position to constrain in the constrained line
	 * @param condition 
	 */
	
	public void setPrevT(T prevT)
	{
		this.prevT = prevT;
	}

	public void reify(List<List<T>> tokenLines) {
		List<T> prevLine = tokenLines.get(prevLineNumber);
		if (prevPos == Constraint.FINAL_POSITION){
			setPrevT(prevLine.get(prevLine.size()-1));		
		}
		else{
			setPrevT(prevLine.get(prevPos));		
		}
	}
	
	public String asString() {
		return " with the token at line " + prevLineNumber + ", " + Utils.getPositionString(prevPos) + " (" + (prevT == null? "" : prevT) + ")";
	}
}
