package substructure;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public enum SubstructureSource {
	UNSET, FIXED, DISTRIBUTION, HIERARCHICAL, TEST;

	private static final List<SubstructureSource> VALUES = Collections.unmodifiableList(Arrays.asList(values()));
	private static final int SIZE = VALUES.size();
	private static final Random RAND = new Random();

	public static SubstructureSource randomSubstructureSource() {
		return VALUES.get(RAND.nextInt(SIZE));
	}

}
