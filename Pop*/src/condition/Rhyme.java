package condition;

import java.util.List;

import lyrics.Lyric;
import tabcomplete.rhyme.Phonetecizer;
import tabcomplete.rhyme.RhymeStructureAnalyzer;
import tabcomplete.rhyme.StressedPhone;

public class Rhyme<T> extends DelayedConstraintCondition<Lyric> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final double MATCHING_LINE_THRESHOLD = .6;

	public Rhyme(int line, int pos) {
		super(line,pos);
	}

	public boolean isSatisfiedBy(Lyric t) {
		return rhyme(t,prevT);
	}

	private boolean rhyme(Lyric t, Lyric s) {
		if (t.equals(s)) {
			return false;
		}
		List<StressedPhone[]> tPhones = Phonetecizer.getPhonesForXLastSyllables(t.toString(), 1);
		List<StressedPhone[]> sPhones = Phonetecizer.getPhonesForXLastSyllables(s.toString(), 1);
		
		for(StressedPhone[] line1Phone:tPhones) {
			for(StressedPhone[] line2Phone: sPhones) {
				if (RhymeStructureAnalyzer.scoreRhymeByPatsRules(line1Phone, line2Phone) > MATCHING_LINE_THRESHOLD) {
					return true;
				}
			}
		}
	
		return false;
	}
}
