package condition;

import lyrics.Lyric;

public class Rhyme<T> extends DelayedConstraintCondition<Lyric> {

	public Rhyme(int line, int pos) {
		super(line,pos);
	}

	public boolean isSatisfiedBy(Lyric t) {
		return rhyme(t,prevT);
	}

	private boolean rhyme(Lyric t, Lyric s) {
		String tEnd = t.lastSyllable();
		String sEnd = s.lastSyllable();
		
		return tEnd.equals(sEnd);
	}

	@Override
	protected String asString() {
		return " as per spelling";
	}

}
