package lyrics;

import globalstructure.SegmentType;
import inspiration.Inspiration;
import markov.SingleOrderMarkovModel;
import substructure.SegmentSubstructure;

public class NGramLyricEngineer extends LyricalEngineer {

	private SingleOrderMarkovModel<Lyric> mModel;

	@Override
	protected LyricSegment generateSegmentLyrics(Inspiration inspiration, SegmentSubstructure segmentSubstructures,
			SegmentType segmentKey) {
		//TODO: stuff
		
		
		return null;
	}

	static private SingleOrderMarkovModel<Lyric> trainLyricalMarkovModel() {
		
		SingleOrderMarkovModel<Lyric> newModel = null;
		
		//TODO: actually train model on data
		
		return newModel;
	}
}
