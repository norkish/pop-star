package lyrics;

import composition.Score;
import inspiration.Inspiration;

public class BensLyricalEngineer extends LyricalEngineer {

	@Override
	public void addLyrics(Inspiration inspiration, Score score) {
		// TODO Auto-generated method stub
		
	}
	
	// Legacy Code
//	private Map<SegmentType, Map<Integer, BackedDistribution<ConstraintBlock<Lyric>>>> lyricConstraintsDistribution = DataLoader.getLyricConstraintsDistribution();
//	private Map<SegmentType, SparseSingleOrderMarkovModel<Lyric>> mModel = DataLoader.getLyricMarkovModel();
//	
//	@Override
//	protected void applyVariationToChorus(LyricSegment lyricSegment, Inspiration inspiration,
//			SegmentStructure segmentSubstructures, SegmentType segmentKey, boolean isLast) {
//		// TODO Auto-generated method stub
//
//	}
//
//	@Override
//	protected LyricSegment generateSegmentLyrics(Inspiration inspiration, SegmentStructure segmentSubstructures,
//			SegmentType segmentKey) {
//		// SegmentKey is the type of stanza you're generating
//		// segmentSubstructures are the constraints that you wanna satisfy
//		// inspiration is the mood of the stanza
//		List<List<Lyric>> stanza = new ArrayList<List<Lyric>>();
//		
//		SparseSingleOrderMarkovModel<Lyric> segmentSpecificMM = mModel.get(segmentKey);
//				
//		return new LyricSegment(stanza);
//	}
//
//	public static void main() {
//		BensLyricalEngineer lEngineer = new BensLyricalEngineer();
//		LyricSegment lyrics = lEngineer.generateSegmentLyrics(null, null, null);
//		
//		
//		System.out.println(lyrics);
//	}
//
//	
	
}
