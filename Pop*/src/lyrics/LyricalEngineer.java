package lyrics;

import java.util.HashMap;
import java.util.Map;

import globalstructure.SegmentType;
import inspiration.Inspiration;
import structure.Structure;
import substructure.SegmentSubstructure;
import utils.Utils;

public abstract class LyricalEngineer {

	public Lyrics generateLyrics(Inspiration inspiration, Structure structure)
	{
		Lyrics lyrics = new Lyrics();
		
		//This data structure contains, for each type of segment in the structure, a list of chord progressions
		// representing the varied progressions of that segment type (e.g., a list of verse progressions for the 
		// segment type verse). This is to allow for order-dependent variation between verses, choruses, etc. 
		Map<SegmentType, SegmentSubstructure[]> substructures = structure.getSubstructure();
		Map<SegmentType, LyricSegment[]> lyricsBySegment = initLyricSegments(substructures);
		
		for (SegmentType segmentKey : lyricsBySegment.keySet()) {
			LyricSegment[] lyricSegments = lyricsBySegment.get(segmentKey);
			SegmentSubstructure[] segmentSubstructures = substructures.get(segmentKey);
			lyricSegments[0] = generateSegmentLyrics(inspiration, segmentSubstructures[0], segmentKey);
			for (int i = 1; i < lyricSegments.length; i++) {
				if (segmentKey == SegmentType.CHORUS) {
					lyricSegments[i] = (LyricSegment) Utils.deepCopy(lyricSegments[i-1]);
					applyVariationToChorus(lyricSegments[i], inspiration, segmentSubstructures[i], segmentKey, i == (lyricSegments.length-1));
				} else { 
					lyricSegments[i] = generateSegmentLyrics(inspiration, segmentSubstructures[i], segmentKey);
				}
			}
		}
		
		lyrics.setLyricsBySegment(lyricsBySegment);
		
		return lyrics;
	}
	
	protected abstract void applyVariationToChorus(LyricSegment lyricSegment, Inspiration inspiration, SegmentSubstructure segmentSubstructures, SegmentType segmentKey, boolean isLast);

	protected abstract LyricSegment generateSegmentLyrics(Inspiration inspiration, SegmentSubstructure segmentSubstructures, SegmentType segmentKey);

	private Map<SegmentType, LyricSegment[]> initLyricSegments(Map<SegmentType, SegmentSubstructure[]> substructure) {
		Map<SegmentType, LyricSegment[]> lyrics = new HashMap<SegmentType, LyricSegment[]>();
		for (SegmentType segment : substructure.keySet()) {
			lyrics.put(segment, new LyricSegment[substructure.get(segment).length]);
		}
		
		return lyrics;
	}
	
}
