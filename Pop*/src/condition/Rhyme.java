package condition;

import java.util.List;

import data.MusicXMLParser.NoteLyric;
import tabcomplete.rhyme.Phonetecizer;
import tabcomplete.rhyme.RhymeStructureAnalyzer;
import tabcomplete.rhyme.StressedPhone;

public class Rhyme<T> extends DelayedConstraintCondition<NoteLyric> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final double MATCHING_LINE_THRESHOLD = .6;
	private boolean isPhraseEndingRhyme = false;

	public Rhyme(int measure, double offset) {
		super(measure,offset);
	}

	public boolean isSatisfiedBy(NoteLyric t) {
		return rhyme(t,prevT);
	}

	private boolean rhyme(NoteLyric t, NoteLyric s) {
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

	public void markPhraseEndingRhymeConstraint(boolean isPhraseEndingRhyme) {
		this.isPhraseEndingRhyme  = isPhraseEndingRhyme;
	}
	
	public boolean isPhraseEndingRhyme() {
		return isPhraseEndingRhyme;
	}
}
