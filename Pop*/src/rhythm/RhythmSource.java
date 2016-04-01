package rhythm;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public enum RhythmSource {
	UNSET, RANDOM, PHRASE_DICT, LYRICAL_STRESS_PATTERNS, TEST;

	private static final List<RhythmSource> VALUES = Collections.unmodifiableList(Arrays.asList(values()));
	private static final int SIZE = VALUES.size();
	private static final Random RAND = new Random();

	public static RhythmSource randomRhythmSource() {
		return VALUES.get(RAND.nextInt(SIZE));
	}

}
