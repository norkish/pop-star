package segmentstructure;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.SortedMap;

import composition.Measure;
import constraint.Constraint;
import data.MusicXMLModel;
import data.MusicXMLModelLearner;
import data.MusicXMLParser.KeyMode;
import data.ParsedMusicXMLObject;
import globalstructure.SegmentType;
import lyrics.Lyric;
import utils.Triple;
import utils.Utils;

public class DistributionalSegmentStructureEngineer extends SegmentStructureEngineer {

	public static class DistributionalSegmentStructureEngineerMusicXMLModel extends MusicXMLModel {

		private Map<SegmentType,Map<Integer,Integer>> measureCountDistribution = new EnumMap<SegmentType, Map<Integer,Integer>>(SegmentType.class);
		private Map<SegmentType, Integer> measureCountDistributionTotals = new EnumMap<SegmentType, Integer>(SegmentType.class); 
		private Map<Integer, List<Constraint<Lyric>>> rhymeConstraintDistribution = new HashMap<Integer, List<Constraint<Lyric>>>();
		private Random rand = new Random();
		
		public DistributionalSegmentStructureEngineerMusicXMLModel() {
			for(SegmentType segType: SegmentType.values()) {
				measureCountDistribution.put(segType, new HashMap<Integer, Integer>());
				measureCountDistributionTotals.put(segType, 0);
			}
		}
		
		@Override
		public void trainOnExample(ParsedMusicXMLObject musicXML) {
			
			// train measure count distribution
			int prevMsr = 0;
			SegmentType prevType = null;
			final SortedMap<Integer, SegmentType> globalStructure = musicXML.globalStructure;
			for(Integer msr : globalStructure.keySet()) {
				if (prevType != null) {
					Utils.incrementValueForKey(measureCountDistribution.get(prevType), (msr-prevMsr));
					measureCountDistributionTotals.put(prevType, measureCountDistributionTotals.get(prevType)+1);
				}
				
				prevType = globalStructure.get(msr);
				prevMsr = msr;
			}

			if (prevType != null) {
				Utils.incrementValueForKey(measureCountDistribution.get(prevType), (musicXML.getMeasureCount()-prevMsr));
				measureCountDistributionTotals.put(prevType, measureCountDistributionTotals.get(prevType)+1);
			}
			
			// train constraint distribution
		}

		public int sampleMeasureCountForSegmentType(SegmentType segmentType) {
			int offsetIntoDistribution = rand.nextInt(measureCountDistributionTotals.get(segmentType));
			
			Map<Integer, Integer> distForType = measureCountDistribution.get(segmentType);
			
			int accum = 0;
			for (Entry<Integer, Integer> entry : distForType.entrySet()) {
				accum += entry.getValue();
				if (accum >= offsetIntoDistribution) {
					return entry.getKey();
				}
			}
			
			throw new RuntimeException("Should never reach here");
		}

		public List<Triple<Integer, Double, Constraint>> sampleConstraints(int measureCount) {
			List<Triple<Integer, Double, Constraint>> constraints = new ArrayList<Triple<Integer, Double, Constraint>>();
			
//			constraints.add(new Triple<Integer, Double, Constraint>(2, Constraint.ALL_POSITIONS, new Constraint<Harmony>(2345, new ExactBinaryMatch<Harmony>(0, Constraint.ALL_POSITIONS), true));

			return constraints;
		}
	}

	DistributionalSegmentStructureEngineerMusicXMLModel model;
	
	public DistributionalSegmentStructureEngineer() {
		model = (DistributionalSegmentStructureEngineerMusicXMLModel) MusicXMLModelLearner.getTrainedModel(this.getClass());
	}
	
	@Override
	public SegmentStructure defineSegmentStructure(SegmentType segmentType) {
		int measureCount = model.sampleMeasureCountForSegmentType(segmentType);
		
		SegmentStructure segmentStructure = new SegmentStructure(measureCount, segmentType);
		segmentStructure.addDivisionsPerQuarterNote(0,4);
		segmentStructure.addKey(0,0,KeyMode.MAJOR);
		segmentStructure.addTime(0,4,4);
		
		List<Triple<Integer, Double, Constraint>> constraints = model.sampleConstraints(measureCount);
		for (Triple<Integer, Double, Constraint> triple : constraints) {
			segmentStructure.addConstraint(triple.getFirst(), triple.getSecond(), triple.getThird());
		}
		
		return segmentStructure;
	}

	@Override
	public List<Measure> instantiateSegmentStructure(SegmentType segmentType, SegmentStructure segmentStructure,
			boolean lastOfKind, boolean lastSegment) {
		return instantiateExactSegmentStructure(segmentType, segmentStructure, lastOfKind, lastSegment);
	}
}
