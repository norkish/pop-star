package constraint;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ConstraintBlock<T> implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	List<List<Constraint<T>>> constraints = new ArrayList<List<Constraint<T>>>();
	List<Integer> lengthConstraints = new ArrayList<Integer>();
	
	@Override
	public int hashCode() {
		int c = 1;
		if(constraints != null) {
			for (List<Constraint<T>> list : constraints) {
				for (Constraint<T> constraint : list) {
					c = 31*c + (constraint==null ? 0 : constraint.hashCode());
				}
				c = 31*c;
			}
		}
		return 31 + c;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof ConstraintBlock))
			return false;
		@SuppressWarnings("unchecked")
		ConstraintBlock<T> other = (ConstraintBlock<T>) obj;
		if (constraints.size() != other.constraints.size())
			return false;
		else {
			List<Constraint<T>> thisLineOfConstraints, otherLineOfConstraints;
			Constraint<T> thisConstraint, otherConstraint;
			for (int i = 0; i < constraints.size(); i++) {
				thisLineOfConstraints = constraints.get(i);
				otherLineOfConstraints = other.constraints.get(i);
				if (thisLineOfConstraints.size() != otherLineOfConstraints.size())
					return false;
				for (int j = 0; j < thisLineOfConstraints.size(); j++) {
					thisConstraint = thisLineOfConstraints.get(j);
					otherConstraint = otherLineOfConstraints.get(j);
					if (thisConstraint == null) {
						if (otherConstraint != null) {
							return false;
						}
					} else if (otherConstraint == null) {
						return false;
					}
					if (!thisConstraint.equals(otherConstraint))
						return false;
				}
			}
		}
		return true;
	}

	public void addLineConstraints(List<Constraint<T>> rhymeConstraintsForLine) {
		constraints.add(rhymeConstraintsForLine);
	}

	public void addConstraint(int lineNum, Constraint<T> constraint) {
		constraints.get(lineNum).add(constraint);
	}

	public List<Constraint<T>> getConstraintsForLine(int lineNum) {
		return constraints.get(lineNum);
	}

	public void merge(ConstraintBlock<T> otherConstraintBlock) {
		assert(constraints.size() == otherConstraintBlock.constraints.size());
		for (int i = 0; i < constraints.size(); i++) {
			constraints.get(i).addAll(otherConstraintBlock.constraints.get(i));
		}
	}

	public void addLengthConstraint(int len) {
		lengthConstraints.add(len);
	}

	public List<Integer> getLengthConstraints() {
		return lengthConstraints;
	}

	public void addLengthConstraints(List<Integer> newLengthConstraints) {
		lengthConstraints = newLengthConstraints;
	}

	public int getLengthConstraint(int i) {
		return lengthConstraints.get(i);
	}
	
	
}
