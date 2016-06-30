package tabcomplete.alignment;

public abstract class XGenericPairwiseAlignment {

	public static final char INDEL_CHAR = '&';
	
	abstract String getFirst();

	abstract String getSecond();

	abstract int[] getScores();

	abstract int getScore();
	
	/**
	 * @param charA
	 * @param charB
	 * @return
	 */
	

}
