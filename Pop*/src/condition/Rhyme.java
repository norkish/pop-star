package condition;

import java.util.List;

import lyrics.Lyric;
import rhyme.Phonetecizer;
import rhyme.RhymeStructureAnalyzer;
import rhyme.StressedPhone;

public class Rhyme<T> extends DelayedConstraintCondition<Lyric> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public Rhyme(int line, int pos) {
		super(line,pos);
	}

	public boolean isSatisfiedBy(Lyric t) {
		return rhyme(t,prevT);
	}

	private boolean rhyme(Lyric t, Lyric s) {
		List<StressedPhone[]> tPhones = Phonetecizer.getPhonesForXLastSyllables(t.toString(), 1);
		List<StressedPhone[]> sPhones = Phonetecizer.getPhonesForXLastSyllables(s.toString(), 1);
		
		for(StressedPhone[] line1Phone:tPhones) {
			for(StressedPhone[] line2Phone: sPhones) {
				if (RhymeStructureAnalyzer.scoreRhymeByPatsRules(line1Phone, line2Phone) > RhymeStructureAnalyzer.MATCHING_LINE_THRESHOLD) {
					return true;
				}
			}
		}
	
		return false;
	}
}
