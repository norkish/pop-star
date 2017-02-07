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
import condition.ConstraintCondition;
import condition.DelayedConstraintCondition;
import constraint.Constraint;
import data.MusicXMLModel;
import data.MusicXMLModelLearner;
import data.MusicXMLParser.KeyMode;
import data.MusicXMLParser.Note;
import data.MusicXMLParser.NoteLyric;
import data.ParsedMusicXMLObject;
import globalstructure.SegmentType;
import utils.Triple;
import utils.Utils;

public class DistributionalSegmentStructureEngineer extends SegmentStructureEngineer {

	public static class DistributionalSegmentStructureEngineerMusicXMLModel extends MusicXMLModel {

		private Map<SegmentType,Map<Integer,Integer>> measureCountDistribution = new EnumMap<SegmentType, Map<Integer,Integer>>(SegmentType.class);
		private Map<SegmentType, Integer> measureCountDistributionTotals = new EnumMap<SegmentType, Integer>(SegmentType.class); 
		private Map<Integer, List<List<Triple<Integer, Double, Constraint<NoteLyric>>>>> lyricConstraintDistribution = new HashMap<Integer, List<List<Triple<Integer, Double, Constraint<NoteLyric>>>>>();
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
			SegmentType currSegType,prevSegType = null;
			List<Triple<Integer, Double, Constraint<NoteLyric>>> currentSegmentLyricConstraints = null;
			SortedMap<Integer, SortedMap<Integer, Note>> notesMap = musicXML.getNotesByPlayedMeasureAsMap();
			SortedMap<Integer, SortedMap<Double, List<Constraint<NoteLyric>>>> manuallyAnnotatedConstraints = musicXML.segmentStructure;
			
			int currentSegmentStartMeasure = 0;
			int measureIdxInSegment;
			for (int measure = 0; measure < musicXML.getMeasureCount(); measure++) {
				currSegType = globalStructure.get(measure);
				measureIdxInSegment = measure - currentSegmentStartMeasure;
				if (currSegType == null) {
					currSegType = prevSegType;
				} else {
					System.out.println("Training on segment " + currSegType);
					// new segment
					if (prevSegType != null) { // if not first segment, add previous one.
						List<List<Triple<Integer, Double, Constraint<NoteLyric>>>> rhymeConstraintDistributionForSegLen = lyricConstraintDistribution.get(measureIdxInSegment);
						if (rhymeConstraintDistributionForSegLen == null) {
							rhymeConstraintDistributionForSegLen = new ArrayList<List<Triple<Integer, Double, Constraint<NoteLyric>>>>();
							lyricConstraintDistribution.put(measureIdxInSegment, rhymeConstraintDistributionForSegLen);
						}
						rhymeConstraintDistributionForSegLen.add(currentSegmentLyricConstraints);
					}
					
					prevSegType = currSegType;
					currentSegmentStartMeasure = measure;
					measureIdxInSegment = 0;
					currentSegmentLyricConstraints = new ArrayList<Triple<Integer, Double, Constraint<NoteLyric>>>();
				}
				
				
				SortedMap<Integer, Note> notesForMeasure = notesMap.get(measure);
				if (notesForMeasure != null) { 
					for(Integer offsetInDivs: notesForMeasure.keySet()) {
						SortedMap<Double, List<Constraint<NoteLyric>>> constraintsForMeasure = manuallyAnnotatedConstraints.get(measure);
						if (constraintsForMeasure != null) {
							double offsetInBeats = musicXML.divsToBeats(offsetInDivs,measure);
							List<Constraint<NoteLyric>> constraintsForOffset = constraintsForMeasure.get(offsetInBeats);
							if (constraintsForOffset != null) {
								for (Constraint<NoteLyric> constraint : constraintsForOffset) {
									Constraint<NoteLyric> modifiedConstraint = (Constraint<NoteLyric>) Utils.deepCopy(constraint);
//									System.out.println("Original constraint was m " + measure + " b" + offsetInBeats + " " + constraint);
									ConstraintCondition<NoteLyric> modifiedConstraintCondition = modifiedConstraint.getCondition();
									if (modifiedConstraintCondition instanceof DelayedConstraintCondition<?>) {
										// need to cast
										DelayedConstraintCondition delayedModifiedConstraintCondition = (DelayedConstraintCondition) modifiedConstraintCondition;
										int oldReferenceMeasure = delayedModifiedConstraintCondition.getReferenceMeasure();
										if (oldReferenceMeasure != DelayedConstraintCondition.PREV_VERSE) {
											delayedModifiedConstraintCondition.setReferenceMeasure(oldReferenceMeasure- currentSegmentStartMeasure);
										}
										System.out.println("In measure " + measureIdxInSegment + ", beat " + offsetInBeats + ", " + modifiedConstraint);
									}
									currentSegmentLyricConstraints.add(new Triple<Integer, Double, Constraint<NoteLyric>>(measureIdxInSegment, offsetInBeats, modifiedConstraint));
									// add phrase end constraint if it's phrase end
								}
							}
						}
					}
				}
			}
			if (prevSegType != null) { // if not first segment, add previous one.
				measureIdxInSegment = musicXML.getMeasureCount() - currentSegmentStartMeasure;
				List<List<Triple<Integer, Double, Constraint<NoteLyric>>>> rhymeConstraintDistributionForSegLen = lyricConstraintDistribution.get(measureIdxInSegment);
				if (rhymeConstraintDistributionForSegLen == null) {
					rhymeConstraintDistributionForSegLen = new ArrayList<List<Triple<Integer, Double, Constraint<NoteLyric>>>>();
					lyricConstraintDistribution.put(measureIdxInSegment, rhymeConstraintDistributionForSegLen);
				}
				rhymeConstraintDistributionForSegLen.add(currentSegmentLyricConstraints);
			}
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

		public List<Triple<Integer, Double, Constraint<NoteLyric>>> sampleConstraints(int measureCount) {
			
			List<List<Triple<Integer, Double, Constraint<NoteLyric>>>> distForMeasureCount = lyricConstraintDistribution.get(measureCount);
			int offsetIntoDistribution = rand.nextInt(distForMeasureCount.size());
			List<Triple<Integer, Double, Constraint<NoteLyric>>> sampledLyricConstraints = distForMeasureCount.get(offsetIntoDistribution);
			
			return sampledLyricConstraints;
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
		
		List<Triple<Integer, Double, Constraint<NoteLyric>>> constraints = model.sampleConstraints(measureCount);
		for (Triple<Integer, Double, Constraint<NoteLyric>> triple : constraints) {
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
