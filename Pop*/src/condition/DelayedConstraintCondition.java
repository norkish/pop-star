package condition;

import java.util.List;

import composition.Measure;
import data.MusicXMLParser.Harmony;
import data.MusicXMLParser.Note;
import data.MusicXMLParser.NoteLyric;

public abstract class DelayedConstraintCondition<T> extends ConstraintCondition<T> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected T prevT = null;
	protected int prevMeasureNumber;
	protected double prevMeasureOffset;
	
	public DelayedConstraintCondition(int measure, double offset) {
		this.prevMeasureNumber = measure;
		this.prevMeasureOffset = offset;
	}

	/**
	 * A DelayedMatchConstraint is a constraint that cannot be fully defined until a previous
	 * line has been generated (i.e., the constraint depends on the value of a generated
	 * element)
	 * @param prevMeasureNumber the previous line from which to extract the constraint
	 * @param prevPos the position in the previous line from which to extract the match
	 * @param pos the position to constrain in the constrained line
	 * @param condition 
	 */
	
	public void setPrevT(T prevT)
	{
		this.prevT = prevT;
	}

	@SuppressWarnings("unchecked")
	public void reify(List<Measure> measures) {
		Measure prevMeasure = measures.get(prevMeasureNumber);
		if (prevT instanceof Note) {
			setPrevT((T) prevMeasure.getClosestNote(prevMeasureOffset));		
		} else if (prevT instanceof NoteLyric) {
			setPrevT((T) prevMeasure.getClosestNoteLyric(prevMeasureOffset));		
		} else if (prevT instanceof Harmony) {
			setPrevT((T) prevMeasure.getClosestHarmony(prevMeasureOffset));		
		}
		
	}
	
	public static final int PREV_VERSE = -124123;
	
	public String asString() {
		StringBuilder str = new StringBuilder();
		str.append(" with the ").append(prevT == null? "word" : prevT.getClass().getName());
		if(prevMeasureNumber == PREV_VERSE) {
			str.append(" in other instantiates of the segment");
		} else {
			str.append(" at measure ").append(prevMeasureNumber).append(", offset ").append(prevMeasureOffset).append(" (" + (prevT == null? "" : prevT)).append(")");
		}
		return str.toString(); 
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + prevMeasureNumber;
		long temp;
		temp = Double.doubleToLongBits(prevMeasureOffset);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((prevT == null) ? 0 : prevT.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof DelayedConstraintCondition))
			return false;
		@SuppressWarnings("rawtypes")
		DelayedConstraintCondition other = (DelayedConstraintCondition) obj;
		if (prevMeasureNumber != other.prevMeasureNumber)
			return false;
		if (Double.doubleToLongBits(prevMeasureOffset) != Double.doubleToLongBits(other.prevMeasureOffset))
			return false;
		if (prevT == null) {
			if (other.prevT != null)
				return false;
		} else if (!prevT.equals(other.prevT))
			return false;
		return true;
	}

	public int getReferenceMeasure() {
		return prevMeasureNumber;
	}
	
	public double getReferenceOffset() {
		return prevMeasureOffset;
	}

	public void setReferenceMeasure(int referenceMeasure) {
		this.prevMeasureNumber = referenceMeasure;
	}
}
