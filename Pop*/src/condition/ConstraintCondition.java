package condition;

public abstract class ConstraintCondition<T> {

	public abstract boolean isSatisfiedBy(T t);
	
	public String toString()
	{
		StringBuilder str = new StringBuilder();
		
		str.append(this.getClass().getSimpleName());
		str.append(this.asString());
		
		return str.toString();	
	}

	abstract protected String asString();

	abstract public boolean equals(Object other);
	abstract public int hashCode();
}
