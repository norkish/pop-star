package lyrics;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public enum LyricalSource {
	UNSET, TEMPLATE, LYRICAL_NGRAM, NON_LYRICAL_NGRAM, TEST;


	private static final List<LyricalSource> VALUES = Collections.unmodifiableList(Arrays.asList(values()));
	private static final int SIZE = VALUES.size();
	private static final Random RAND = new Random();

	public static LyricalSource randomLyricalSource() {
		return VALUES.get(RAND.nextInt(SIZE));
	}

}
