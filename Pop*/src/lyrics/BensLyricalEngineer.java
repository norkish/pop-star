package lyrics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import constraint.ConstraintBlock;
import data.BackedDistribution;
import data.DataLoader;
import globalstructure.SegmentType;
import inspiration.Inspiration;
import markov.SparseSingleOrderMarkovModel;
import substructure.SegmentSubstructure;
import utils.Utils;

public class BensLyricalEngineer extends LyricalEngineer {

	private Map<SegmentType, Map<Integer, BackedDistribution<ConstraintBlock<Lyric>>>> lyricConstraintsDistribution = DataLoader.getLyricConstraintsDistribution();
	private Map<SegmentType, SparseSingleOrderMarkovModel<Lyric>> mModel = DataLoader.getLyricMarkovModel();
	
	@Override
	protected void applyVariationToChorus(LyricSegment lyricSegment, Inspiration inspiration,
			SegmentSubstructure segmentSubstructures, SegmentType segmentKey, boolean isLast) {
		// TODO Auto-generated method stub

	}

	@Override
	protected LyricSegment generateSegmentLyrics(Inspiration inspiration, SegmentSubstructure segmentSubstructures,
			SegmentType segmentKey) {
		// SegmentKey is the type of stanza you're generating
		// segmentSubstructures are the constraints that you wanna satisfy
		// inspiration is the mood of the stanza
		List<List<Lyric>> stanza = new ArrayList<List<Lyric>>();
		
		SparseSingleOrderMarkovModel<Lyric> segmentSpecificMM = mModel.get(segmentKey);
				
		return new LyricSegment(stanza);
	}

	public static void main() {
		BensLyricalEngineer lEngineer = new BensLyricalEngineer();
		LyricSegment lyrics = lEngineer.generateSegmentLyrics(null, null, null);
		
		
		System.out.println(lyrics);
	}
	
}
