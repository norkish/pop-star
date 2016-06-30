package tabcomplete.alignment;

import tabcomplete.rhyme.Phonetecizer;
import tabcomplete.rhyme.StressedPhone;

public class StressedPhonePairAlignment extends Alignment {

	private StressedPhone[] aln1;
	private StressedPhone[] aln2;

	public StressedPhonePairAlignment(StressedPhone[] aln1, StressedPhone[] aln2, double[] scores) {
		super(scores);
		this.aln1 = aln1;
		this.aln2 = aln2;
	}

	@Override
	public StressedPhone[] getFirst() {
		return aln1;
	}

	@Override
	public StressedPhone[] getSecond() {
		return aln2;
	}
	
	public String toString() {
		StringBuilder str = new StringBuilder();
		StressedPhone sp;
		
		for (int i = 0; i < aln1.length; i++) {
			sp = aln1[i];
			str.append(sp == null? '&':Phonetecizer.intToString(sp.phone));
			str.append("\t");
		}
		str.append("\n");

		for (int i = 0; i < aln2.length; i++) {
			sp = aln2[i];
			str.append(sp == null? '&':Phonetecizer.intToString(sp.phone));
			str.append("\t");
		}
		str.append("\n");
		
		for (int i = 0; i < scores.length; i++) {
			str.append(scores[i]);
			str.append("\t");
		}
		str.append("\n");
		
		return str.toString();
	}
}
