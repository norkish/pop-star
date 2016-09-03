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

	//private Map<SegmentType, Map<Integer, BackedDistribution<ConstraintBlock<Lyric>>>> lyricConstraintsDistribution = DataLoader.getLyricConstraintsDistribution();
	//private Map<SegmentType, SparseSingleOrderMarkovModel<Lyric>> mModel = DataLoader.getLyricMarkovModel();
	
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
		List<Lyric> l1 = new ArrayList<Lyric>();
		List<Lyric> l2 = new ArrayList<Lyric>();
		List<Lyric> l3 = new ArrayList<Lyric>();
		l1.add(new Lyric("It's"));
		l1.add(new Lyric("a"));
		l1.add(new Lyric("terrible"));
		l1.add(new Lyric("love"));
		l1.add(new Lyric("that"));
		l1.add(new Lyric("I'm"));
		l1.add(new Lyric("walking"));
		l1.add(new Lyric("with"));
		l1.add(new Lyric("spiders"));
		l2.add(new Lyric("It's"));
		l2.add(new Lyric("a"));
		l2.add(new Lyric("terrible"));
		l2.add(new Lyric("love"));
		l2.add(new Lyric("that"));
		l2.add(new Lyric("I'm"));
		l2.add(new Lyric("walking"));
		l2.add(new Lyric("with"));
		l3.add(new Lyric("It's"));
		l3.add(new Lyric("quiet"));
		l3.add(new Lyric("company"));
		stanza.add(l1);
		stanza.add(l2);
		stanza.add(l1);
		stanza.add(l2);
		stanza.add(l3);
		stanza.add(l3);
		
		//SparseSingleOrderMarkovModel<Lyric> segmentSpecificMM = mModel.get(segmentKey);
				
		return new LyricSegment(stanza);
	}

	public static void main(String[] args) {
		BensLyricalEngineer lEngineer = new BensLyricalEngineer();
		LyricSegment lyrics = lEngineer.generateSegmentLyrics(null, null, null);
		
		
		print(lyrics);
	}
	
	private static void print(LyricSegment lyrics) {
		List<List<Lyric>> lines = lyrics.getLines();
		for (int i = 0; i < lines.size(); i++) {
			List<Lyric> line = lines.get(i);
			for (int j = 0; j < line.size(); j++) {
				Lyric lyric = line.get(j);
				System.out.print(lyric.toString() + " ");
			}
			System.out.print("\n");
		}
	}
	
}
