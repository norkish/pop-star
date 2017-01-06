package melody;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public enum MelodySource {
	UNSET, TEST, RANDOM;

	private static final List<MelodySource> VALUES = Collections.unmodifiableList(Arrays.asList(values()));
	private static final int SIZE = VALUES.size();
	private static final Random RAND = new Random();

	public static MelodySource randomMelodySource() {
		return VALUES.get(RAND.nextInt(SIZE));
	}
}
