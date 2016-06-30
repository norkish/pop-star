package tabcomplete.alignment;

public class MSAStringAlignment extends Alignment{
	
	private String alignedLyricSeq;
	private String[] newAlignedSeqs;

	public MSAStringAlignment(String[] newAlignedSeqs, String alignedLyricSeq, double[] scores) {
		super(scores);
		this.newAlignedSeqs = newAlignedSeqs;
		this.alignedLyricSeq = alignedLyricSeq;
	}

	@Override
	public String[] getFirst() {
		return newAlignedSeqs;
	}

	@Override
	public String getSecond() {
		// TODO Auto-generated method stub
		return alignedLyricSeq;
	}
}
