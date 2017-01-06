package segmentstructure;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public enum SegmentStructureSource {
	UNSET, FIXED, DISTRIBUTION, HIERARCHICAL, TEST;

	private static final List<SegmentStructureSource> VALUES = Collections.unmodifiableList(Arrays.asList(values()));
	private static final int SIZE = VALUES.size();
	private static final Random RAND = new Random();

	public static SegmentStructureSource randomSubstructureSource() {
		return VALUES.get(RAND.nextInt(SIZE));
	}

}
