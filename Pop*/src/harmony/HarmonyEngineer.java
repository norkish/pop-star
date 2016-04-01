package harmony;

import java.util.HashMap;
import java.util.Map;

import globalstructure.SegmentType;
import inspiration.Inspiration;
import structure.Structure;
import substructure.SegmentSubstructure;
import utils.Utils;

public abstract class HarmonyEngineer {
	
	/**
	 * Generates a chord progression (harmony) according to one of several harmony generation
	 * models (optionally) dependent on previously generated components
	 * @param inspiration Emotion with which to generate chords
	 * @param structure Defines patterns of repetition by which to generate chords 
	 * @param lyrics Useful if the chord progression is generated to reflect lyrical elements
	 * @return a newly generated harmony
	 */
	public Harmony generateHarmony(Inspiration inspiration, Structure structure)
	{
		Harmony harmony = new Harmony();
		
		//This data structure contains, for each type of segment in the structure, a list of chord progressions
		// representing the varied progressions of that segment type (e.g., a list of verse progressions for the 
		// segment type verse). This is to allow for order-dependent variation between verses, choruses, etc. 
		Map<SegmentType, SegmentSubstructure[]> substructures = structure.getSubstructure();
		Map<SegmentType, ProgressionSegment[]> progressions = initProgressions(substructures);
		
		for (SegmentType segmentKey : progressions.keySet()) {
			ProgressionSegment[] segmentHarmonies = progressions.get(segmentKey);
			SegmentSubstructure[] segmentSubstructures = substructures.get(segmentKey);
			segmentHarmonies[0] = generateSegmentHarmony(inspiration, segmentSubstructures[0], segmentKey);
			for (int i = 1; i < segmentHarmonies.length; i++) {
				segmentHarmonies[i] = (ProgressionSegment) Utils.deepCopy(segmentHarmonies[i-1]);
				applyVariation(segmentHarmonies[i], segmentKey, i == (segmentHarmonies.length-1));
			}
		}
		
		harmony.setProgressions(progressions);
		
		return harmony;
	}
	
	protected abstract void applyVariation(ProgressionSegment segmentProgression, SegmentType segmentType, boolean isLast);
	
	/**
	 * Generates a chord progression (harmony) for a particular segment according to one 
	 * of several harmony generation models (optionally) dependent on previously generated components
	 * @param inspiration Emotion with which to generate chords
	 * @param segmentSubstructures Defines patterns of repetition by which to generate chords 
	 * @param segmentType The segment type for which chords are to be generated
	 * @return a newly generated harmony
	 */
	protected abstract ProgressionSegment generateSegmentHarmony(Inspiration inspiration, SegmentSubstructure segmentSubstructures, SegmentType segmentType);

	private Map<SegmentType, ProgressionSegment[]> initProgressions(Map<SegmentType, SegmentSubstructure[]> substructure) {
		Map<SegmentType, ProgressionSegment[]> harmony = new HashMap<SegmentType, ProgressionSegment[]>();
		for (SegmentType segment : substructure.keySet()) {
			harmony.put(segment, new ProgressionSegment[substructure.get(segment).length]);
		}
		
		return harmony;
	}
	
}
