package harmony;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public enum HarmonySource {
	UNSET, MONOCHORD, PHRASE_DICT, SEGMENTSPECIFIC_HMM, TEST;

	private static final List<HarmonySource> VALUES = Collections.unmodifiableList(Arrays.asList(values()));
	private static final int SIZE = VALUES.size();
	private static final Random RAND = new Random();

	public static HarmonySource randomHarmonySource() {
		return VALUES.get(RAND.nextInt(SIZE));
	}

}
