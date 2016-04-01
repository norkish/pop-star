package globalstructure;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public enum StructureSource {
	UNSET, FIXED, DISTRIBUTION, MARKOV, TEST;

	private static final List<StructureSource> VALUES = Collections.unmodifiableList(Arrays.asList(values()));
	private static final int SIZE = VALUES.size();
	private static final Random RAND = new Random();

	public static StructureSource randomStructureSource() {
		return VALUES.get(RAND.nextInt(SIZE));
	}

}
