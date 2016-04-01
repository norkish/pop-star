package pitch;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public enum PitchSource {
	UNSET, RANDOM, HMM, IDIOMS, TEST;

	private static final List<PitchSource> VALUES = Collections.unmodifiableList(Arrays.asList(values()));
	private static final int SIZE = VALUES.size();
	private static final Random RAND = new Random();

	public static PitchSource randomPitchSource() {
		return VALUES.get(RAND.nextInt(SIZE));
	}

}
