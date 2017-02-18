package harmony;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.SortedMap;

import org.tc33.jheatchart.HeatChart;

import composition.Measure;
import composition.Score;
import constraint.Constraint;
import data.MusicXMLModel;
import data.MusicXMLModelLearner;
import data.MusicXMLParser.Harmony;
import data.MusicXMLParser.Time;
import data.ParsedMusicXMLObject;
import globalstructure.SegmentType;
import inspiration.Inspiration;
import markov.SparseSingleOrderMarkovModel;
import utils.Pair;
import utils.Triple;
import utils.Utils;

public class SegmentSpecificHarmonyEngineer extends HarmonyEngineer {

	public static class SegmentSpecificHarmonyEngineerMusicXMLModel extends MusicXMLModel {

		// condition first on segment type, then beat position
		Map<Time, Map<SegmentType,Map<Boolean,SparseSingleOrderMarkovModel<Harmony>>>> chordMarkovModelsByTimeBySegmentByOffset = new HashMap<Time, Map<SegmentType,Map<Boolean,SparseSingleOrderMarkovModel<Harmony>>>>();
		Map<Harmony,Integer> chordStatesByIndex = new HashMap<Harmony,Integer>();
		Map<Time, Map<SegmentType,Map<Boolean,Map<Integer, Integer>>>> chordPriorCountsByTimeBySegmentByOffset = new HashMap<Time, Map<SegmentType,Map<Boolean,Map<Integer, Integer>>>>();
		Map<Time, Map<SegmentType, Map<Boolean,Map<Integer,Map<Integer,Integer>>>>> chordTransitionCountsByTimeBySegmentByOffset = new HashMap<Time, Map<SegmentType, Map<Boolean, Map<Integer,Map<Integer, Integer>>>>>();
		
		Map<Time, Map<SegmentType,Map<Double,Map<Double, Integer>>>> durationPriorCountsByTimeBySegmentByOffset = new HashMap<Time, Map<SegmentType,Map<Double,Map<Double, Integer>>>>();
		Map<Time, Map<SegmentType, Map<Double, Integer>>> durationTotalCountsByTimeBySegmentByOffset = new HashMap<Time, Map<SegmentType, Map<Double, Integer>>>();
		@Override
		/**
		 * Only trains on examples with harmony 
		 */
		public void trainOnExample(ParsedMusicXMLObject musicXML) {
			// if doesn't have harmony, return
			if (musicXML.unoverlappingHarmonyByPlayedMeasure.isEmpty()) {
				return;
			}

			// the current markovModel is now obselete and will be regenerated when next sampled
			
			Integer prevHarmIdx = -1;
			SegmentType currType = null;
			SegmentType prevType = null;
			Time currTime = null;
			Time prevTime = null;
			SortedMap<Integer, Triple<SegmentType, Integer, Double>> globalStructureByFormStart = musicXML.getGlobalStructureByFormStart();
					
			int prevMeasure = -1;
			double prevOffsetInBeats = -1.;
			for (Triple<Integer, Integer, Harmony> triple : musicXML.unoverlappingHarmonyByPlayedMeasure){
				Harmony harmony = triple.getThird();
				if (harmony == null)
					continue;
				
				int measure = triple.getFirst();
				int offsetInDivs = triple.getSecond();
				double offsetInBeats = musicXML.divsToBeats(offsetInDivs, measure);
				currTime = musicXML.getTimeForMeasure(measure);
				Triple<SegmentType, Integer, Double> segmentWithOffset = globalStructureByFormStart.get(measure);
				SegmentType type = segmentWithOffset == null ? null : segmentWithOffset.getFirst();
				if (type != null && type != prevType) {
					currType = type;
					prevHarmIdx = -1;
				}
				
				Integer harmIdx = chordStatesByIndex.get(harmony);
				if (harmIdx == null) {
					harmIdx = chordStatesByIndex.size();
					chordStatesByIndex.put(harmony, harmIdx);
				}
				
				Map<SegmentType, Map<Boolean, SparseSingleOrderMarkovModel<Harmony>>> markovModelsBySegmentByOffset = chordMarkovModelsByTimeBySegmentByOffset.get(currTime);
				if (markovModelsBySegmentByOffset != null) {
					markovModelsBySegmentByOffset.remove(currType); // reset these models since the underlying data is being modified 
				}
				
				if (prevHarmIdx == -1) {
					Map<SegmentType, Map<Boolean, Map<Integer, Integer>>> chordPriorCountsBySegmentByOffset = chordPriorCountsByTimeBySegmentByOffset.get(currTime);
					if (chordPriorCountsBySegmentByOffset == null) {
						chordPriorCountsBySegmentByOffset = new EnumMap<SegmentType, Map<Boolean, Map<Integer, Integer>>>(SegmentType.class);
						chordPriorCountsByTimeBySegmentByOffset.put(currTime, chordPriorCountsBySegmentByOffset);
					}
					
					Map<Boolean, Map<Integer, Integer>> chordPriorCountsByOffset = chordPriorCountsBySegmentByOffset.get(currType);
					if (chordPriorCountsByOffset == null) {
						chordPriorCountsByOffset = new HashMap<Boolean, Map<Integer, Integer>>();
						chordPriorCountsBySegmentByOffset.put(currType, chordPriorCountsByOffset);
					}
				
					Map<Integer, Integer> chordPriorCounts = chordPriorCountsByOffset.get(offsetInBeats);
					if (chordPriorCounts == null) {
						chordPriorCounts = new HashMap<Integer, Integer>();
						chordPriorCountsByOffset.put(offsetInBeats == 0.0, chordPriorCounts);
					}
				
					Utils.incrementValueForKey(chordPriorCounts, harmIdx);
				} else {
					Map<SegmentType, Map<Boolean, Map<Integer, Map<Integer, Integer>>>> chordTransitionCountsBySegmentByOffset = chordTransitionCountsByTimeBySegmentByOffset.get(currTime);
					if (chordTransitionCountsBySegmentByOffset == null) {
						chordTransitionCountsBySegmentByOffset = new EnumMap<SegmentType, Map<Boolean, Map<Integer, Map<Integer,Integer>>>>(SegmentType.class);
						chordTransitionCountsByTimeBySegmentByOffset.put(currTime, chordTransitionCountsBySegmentByOffset);
					}
					
					Map<Boolean, Map<Integer, Map<Integer, Integer>>> chordTransitionCountsByOffset = chordTransitionCountsBySegmentByOffset.get(currType);
					if (chordTransitionCountsByOffset == null) {
						chordTransitionCountsByOffset = new HashMap<Boolean, Map<Integer, Map<Integer,Integer>>>();
						chordTransitionCountsBySegmentByOffset.put(currType, chordTransitionCountsByOffset);
					}
				
					Map<Integer, Map<Integer, Integer>> chordTransitionCounts = chordTransitionCountsByOffset.get(offsetInBeats == 0.0);
					if (chordTransitionCounts == null) {
						chordTransitionCounts = new HashMap<Integer, Map<Integer,Integer>>();
						chordTransitionCountsByOffset.put(offsetInBeats == 0.0, chordTransitionCounts);
					}
				
					Utils.incrementValueForKeys(chordTransitionCounts, prevHarmIdx, harmIdx);

				}

				if (prevTime != null) {
					updateDistributionForLastDuration(musicXML, prevType, prevTime, prevMeasure, prevOffsetInBeats,
						measure, offsetInBeats);
				}
				
				prevHarmIdx = harmIdx;
				prevMeasure = measure;
				prevOffsetInBeats = offsetInBeats;
				prevType = currType;
				prevTime = currTime;
			}
			if (prevTime != null) {
				updateDistributionForLastDuration(musicXML, prevType, prevTime, prevMeasure, prevOffsetInBeats,
						musicXML.getMeasureCount(), 0.0);
			}
		}

		private void updateDistributionForLastDuration(ParsedMusicXMLObject musicXML, SegmentType prevType,
				Time currTime, int prevMeasure, double prevOffsetInBeats, int measure, double offsetInBeatsOfEnd) {
			// update distribution for duration of last harmony
			double duration = 0.0;
			
			if (prevMeasure == measure) {
				duration = offsetInBeatsOfEnd - prevOffsetInBeats;
			} else {
				// the rest of the last measure
				duration = musicXML.getTimeForMeasure(prevMeasure).beats - prevOffsetInBeats;
				// and whatever is in this measure
				duration += offsetInBeatsOfEnd;
				// and the beats for whatever came in between this measure and the last measure
				for (int i = prevMeasure+1; i < measure; i++) {
					duration += musicXML.getTimeForMeasure(i).beats;
				}
			}
			
			Map<SegmentType, Map<Double, Map<Double, Integer>>> durationPriorCountsBySegmentByOffset = durationPriorCountsByTimeBySegmentByOffset.get(currTime);
			Map<SegmentType, Map<Double, Integer>> durationTotalCountsBySegmentByOffset = durationTotalCountsByTimeBySegmentByOffset.get(currTime);
			if (durationPriorCountsBySegmentByOffset == null) {
				durationPriorCountsBySegmentByOffset = new EnumMap<SegmentType, Map<Double, Map<Double, Integer>>>(SegmentType.class);
				durationPriorCountsByTimeBySegmentByOffset.put(currTime, durationPriorCountsBySegmentByOffset);
				durationTotalCountsBySegmentByOffset = new EnumMap<SegmentType, Map<Double, Integer>>(SegmentType.class);
				durationTotalCountsByTimeBySegmentByOffset.put(currTime, durationTotalCountsBySegmentByOffset);
			}
			
			Map<Double, Map<Double, Integer>> durationPriorCountsByOffset = durationPriorCountsBySegmentByOffset.get(prevType);
			Map<Double, Integer> durationTotalCountsByOffset = durationTotalCountsBySegmentByOffset.get(prevType);
			if (durationPriorCountsByOffset == null) {
				durationPriorCountsByOffset = new HashMap<Double, Map<Double, Integer>>();
				durationPriorCountsBySegmentByOffset.put(prevType, durationPriorCountsByOffset);
				durationTotalCountsByOffset = new HashMap<Double,Integer>();
				durationTotalCountsBySegmentByOffset.put(prevType, durationTotalCountsByOffset);
			}
			
			Map<Double, Integer> durationPriorCounts = durationPriorCountsByOffset.get(prevOffsetInBeats);
			if (durationPriorCounts == null) {
				durationPriorCounts = new HashMap<Double, Integer>();
				durationPriorCountsByOffset.put(prevOffsetInBeats, durationPriorCounts);
			}
			
			Utils.incrementValueForKey(durationPriorCounts, duration);
			Utils.incrementValueForKey(durationTotalCountsByOffset, prevOffsetInBeats);
		}
		
		public String toString() {
			StringBuilder str = new StringBuilder();
			
			// string representation of model
			
			return str.toString();
		}

		public List<Triple<Harmony, Integer, Double>> sampleHarmonySequenceOfLength(int length, Time currTime, SegmentType type, List<Constraint<Harmony>> contextualConstraints) {
			// TODO Add constraints according to type, including constraints which depend on the harm sequence 
			Map<SegmentType, Map<Boolean, SparseSingleOrderMarkovModel<Harmony>>> markovModelsBySegmentByOffset = chordMarkovModelsByTimeBySegmentByOffset.get(currTime);
			Map<Boolean, SparseSingleOrderMarkovModel<Harmony>> markovModelsByOffset = null;
			
			List<Triple<Harmony, Integer, Double>> harmoniesWithDuration = new ArrayList<Triple<Harmony, Integer,Double>>();
			
			if (markovModelsBySegmentByOffset == null || !markovModelsBySegmentByOffset.get(type).containsKey(type)) {
				buildAllModels();
			} 
			
			markovModelsBySegmentByOffset = chordMarkovModelsByTimeBySegmentByOffset.get(currTime);
			markovModelsByOffset = markovModelsBySegmentByOffset.get(type); 

			Map<Double, Map<Double, Integer>> durationPriorCountsByOffset = durationPriorCountsByTimeBySegmentByOffset.get(currTime).get(type);
			Map<Double,Integer> durationTotalCountsByOffset = durationTotalCountsByTimeBySegmentByOffset.get(currTime).get(type);
			
			int currMeasure = 0;
			double currBeatOffset = 0.0;
			
			//sample first chord from prior
			SparseSingleOrderMarkovModel<Harmony> markovModel = markovModelsByOffset.get(currBeatOffset == 0.0);
			Harmony prevHarmony = markovModel.sampleStartState();
			double nextHarmonyDuration = sampleNextDuration(durationPriorCountsByOffset, durationTotalCountsByOffset, currBeatOffset);
			harmoniesWithDuration.add(new Triple<Harmony, Integer, Double>(prevHarmony, currMeasure, currBeatOffset));
			currBeatOffset += nextHarmonyDuration;
			while (currBeatOffset >= currTime.beats) {
				currMeasure++;
				currBeatOffset -= currTime.beats;
			}
			
			//sample while not done
			Harmony nextHarmony = null;
			while(currMeasure < length) {
				try {
					try {
						markovModel = markovModelsByOffset.get(currBeatOffset==0.0);
						nextHarmony = markovModel.sampleNextState(prevHarmony);
					} catch (Exception ex) {
						nextHarmony = sampleNextStateForAnyOffset(markovModelsByOffset,prevHarmony);
					}
				} catch (Exception ex) {
					nextHarmony = sampleNextStateForAnyOffsetForAnySegmentType(markovModelsBySegmentByOffset,prevHarmony);
				}
				nextHarmonyDuration = sampleNextDuration(durationPriorCountsByOffset, durationTotalCountsByOffset, currBeatOffset);
				
				harmoniesWithDuration.add(new Triple<Harmony, Integer, Double>(nextHarmony, currMeasure, currBeatOffset));
				
				currBeatOffset += nextHarmonyDuration;
				
				while (currBeatOffset >= currTime.beats) {
					currMeasure++;
					currBeatOffset -= currTime.beats;
				}
				
				prevHarmony = nextHarmony;
			}
			
			return harmoniesWithDuration;
		}

		private Harmony sampleNextStateForAnyOffsetForAnySegmentType(
				Map<SegmentType, Map<Boolean, SparseSingleOrderMarkovModel<Harmony>>> markovModelsBySegmentByOffset,
				Harmony prevHarmony) {
			for (SegmentType type : markovModelsBySegmentByOffset.keySet()) {
				Map<Boolean, SparseSingleOrderMarkovModel<Harmony>> markovModelsByOffset = markovModelsBySegmentByOffset.get(type);
				for (Boolean isZeroOffset : markovModelsByOffset.keySet()) {
					SparseSingleOrderMarkovModel<Harmony> markovModel = markovModelsByOffset.get(isZeroOffset);
					Harmony nextHarmony;
					try {
						nextHarmony = markovModel.sampleNextState(prevHarmony);
						return nextHarmony;
					} catch (ArrayIndexOutOfBoundsException ex) {
					}
				}
			}
			throw new RuntimeException("No next state for any segment for any offset from " + prevHarmony);
		}

		private Harmony sampleNextStateForAnyOffset(
				Map<Boolean, SparseSingleOrderMarkovModel<Harmony>> markovModelsByOffset, Harmony prevHarmony) {
			for (Boolean isZeroOffset : markovModelsByOffset.keySet()) {
				SparseSingleOrderMarkovModel<Harmony> markovModel = markovModelsByOffset.get(isZeroOffset);
				Harmony nextHarmony;
				try {
					nextHarmony = markovModel.sampleNextState(prevHarmony);
					return nextHarmony;
				} catch (ArrayIndexOutOfBoundsException ex) {
				}
			}
			throw new RuntimeException("No next state for any offset from " + prevHarmony);
		}

		private static Random rand = new Random();
		private Double sampleNextDuration(Map<Double, Map<Double, Integer>> durationPriorCountsByOffset,
				Map<Double, Integer> durationTotalCountsByOffset, double currBeatOffset) {
			Map<Double, Integer> durationPriorCountsForOffset;
			durationPriorCountsForOffset = durationPriorCountsByOffset.get(currBeatOffset);
			Integer totalCountsForOffset = durationTotalCountsByOffset.get(currBeatOffset);
			int distributionOffset = rand.nextInt(totalCountsForOffset);
			int total = 0;
			for (Double duration : durationPriorCountsForOffset.keySet()) {
				total += durationPriorCountsForOffset.get(duration);
				if (total > distributionOffset) {
					return duration;
				}
			}
			return null;
		}
		

		private void buildAllModels() {
			for(Time currTime : chordPriorCountsByTimeBySegmentByOffset.keySet()){
				Map<SegmentType, Map<Boolean, Map<Integer, Integer>>> chordPriorCountsBySegmentByOffset = chordPriorCountsByTimeBySegmentByOffset.get(currTime);
				Map<SegmentType, Map<Boolean, SparseSingleOrderMarkovModel<Harmony>>> modelsBySegmentByOffset = new EnumMap<SegmentType, Map<Boolean, SparseSingleOrderMarkovModel<Harmony>>>(SegmentType.class);
				for(SegmentType type: chordPriorCountsBySegmentByOffset.keySet()){
					Map<Boolean, Map<Integer, Integer>> chordPriorCountsByOffset = chordPriorCountsBySegmentByOffset.get(type);
					Map<Boolean, Map<Integer, Map<Integer, Integer>>> chordTransitionCountsByOffset = chordTransitionCountsByTimeBySegmentByOffset.get(currTime).get(type);
					
					Map<Boolean, SparseSingleOrderMarkovModel<Harmony>> modelsByOffset = new HashMap<Boolean, SparseSingleOrderMarkovModel<Harmony>>();
					
					for (Boolean isZeroOffset : chordTransitionCountsByOffset.keySet()) {
						Map<Integer, Double> priors = computePriors(chordPriorCountsByOffset.get(isZeroOffset));
						Map<Integer, Map<Integer, Double>> transitions = computeTransitionProbabilities(chordTransitionCountsByOffset.get(isZeroOffset));
						
						modelsByOffset.put(isZeroOffset, new SparseSingleOrderMarkovModel<Harmony>(chordStatesByIndex, priors, transitions));
					}
					modelsBySegmentByOffset.put(type, modelsByOffset);
				}
				chordMarkovModelsByTimeBySegmentByOffset.put(currTime, modelsBySegmentByOffset);
			}
		}

		@Override
		public void toGraph() {
			Map<Integer, Double> priors = computePriors(chordPriorCountsByTimeBySegmentByOffset.get(Time.FOUR_FOUR).get(SegmentType.VERSE).get(true));
			Map<Integer, Map<Integer, Integer>> chordTransitionCounts = chordTransitionCountsByTimeBySegmentByOffset.get(Time.FOUR_FOUR).get(SegmentType.VERSE).get(true);
			Map<Integer, Map<Integer, Double>> transitions = computeTransitionProbabilities(chordTransitionCounts);
			int maxXValues = 10;
			int maxYValues = 10;
			
			boolean sortByFrequency = true;
			//y-axis is from
			Map<Harmony, Integer> statesByIndexSorted = null;
			if (sortByFrequency) {
				statesByIndexSorted = new LinkedHashMap<Harmony,Integer>();
				Map<Integer, Integer> frequencyByID = new HashMap<Integer,Integer>();
				//initialize frequencies
				for (Integer harmonyID : chordStatesByIndex.values()) {
					frequencyByID.put(harmonyID, 0);
				}
				
				// count frequencies
				for (Map<Integer, Integer> toMap : chordTransitionCounts.values()) {
					for (Integer harmonyID : toMap.keySet()) {
						Integer count = toMap.get(harmonyID);
						frequencyByID.put(harmonyID, frequencyByID.get(harmonyID)+count);
					}
				}
				
				// sort by counts (and then by ID)
				List<Map.Entry<Integer, Integer>> harmonyIDSortedByFrequency = new ArrayList<Map.Entry<Integer, Integer>>(frequencyByID.entrySet());
				Collections.sort(harmonyIDSortedByFrequency, new ValueThenKeyComparator<Integer, Integer>());
				
				// get reverse map to create new Harmony-ID map that is sorted by frequency
				Harmony[] harmonyByID = new Harmony[chordStatesByIndex.size()];
				for (Harmony harmony : chordStatesByIndex.keySet()) {
					Integer harmonyID = chordStatesByIndex.get(harmony);
					harmonyByID[harmonyID] = harmony;
				}
				
				for (Map.Entry<Integer, Integer> entry : harmonyIDSortedByFrequency) {
					statesByIndexSorted.put(harmonyByID[entry.getKey()], entry.getKey());
				}
				
			} else {
				statesByIndexSorted = chordStatesByIndex;
			}
			
			
			// +1 for priors
			int chartXDimension = Math.min(maxXValues, statesByIndexSorted.size()); 
			int chartYDimension = Math.min(maxYValues, statesByIndexSorted.size()); 

			String[] xValues = new String[chartXDimension];
			String[] yValues = new String[chartYDimension + 1];
			double[][] chartValues = new double[chartYDimension + 1][chartXDimension];
			
			// set axis labels
			yValues[0] = "START";
			
			int i = 0;
			for (Harmony harmony : statesByIndexSorted.keySet()) {
				Integer harmonyId = statesByIndexSorted.get(harmony);
				if (i >= chartXDimension && i >= chartYDimension)
					break;
				
				if (i < chartXDimension) {
					xValues[i] = harmony.toString();
					Double prior = priors.get(harmonyId);
					chartValues[0][i] = prior == null ? 0.0 : prior;
				}
				if (i < chartYDimension) {
					yValues[i+1] = harmony.toString();
				}
				i++;
			}
			
			// populate heatchart
			
			int y = 0;
			for (Harmony yHarmony : statesByIndexSorted.keySet()) {
				if (y >= chartYDimension) {
					break;
				}
				Integer yHarmonyId = statesByIndexSorted.get(yHarmony);
				int x = 0;
				for (Harmony xHarmony : statesByIndexSorted.keySet()) {
					if (x >= chartXDimension) {
						break;
					}
					Integer xHarmonyId = statesByIndexSorted.get(xHarmony);
					Double prob = null; 
					Map<Integer, Double> probsFromHarmony = transitions.get(yHarmonyId);
					if (probsFromHarmony != null) {
						prob = probsFromHarmony.get(xHarmonyId);
					}
					chartValues[y+1][x] = prob == null ? 0.0 : prob;
					x++;
				}
				y++;
			}
			
			Utils.normalizeByFirstDimension(chartValues);

			HeatChart chart = new HeatChart(chartValues);
			chart.setHighValueColour(Color.RED);
			chart.setLowValueColour(Color.BLUE);
			
			chart.setYAxisLabel("Previous Chord");
			chart.setXAxisLabel("Next Chord");
			chart.setXValues(xValues);
			chart.setYValues(yValues);
			
			try {
				chart.saveToFile(new File(GRAPH_DIR + "/harmony.jpeg"));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private SegmentSpecificHarmonyEngineerMusicXMLModel model;
	
	public SegmentSpecificHarmonyEngineer() {
		this.model = (SegmentSpecificHarmonyEngineerMusicXMLModel) MusicXMLModelLearner.getTrainedModel(this.getClass());
	}
	@Override
	public void addHarmony(Inspiration inspiration, Score score) {
		List<Pair<SegmentType,Integer>> segmentsLengths = score.getSegmentsLengths();
		
		for (SegmentType type : new SegmentType[]{SegmentType.CHORUS, SegmentType.VERSE, SegmentType.BRIDGE,SegmentType.INTRO,
				SegmentType.OUTRO,SegmentType.INTERLUDE}) {
			int length = getMinLengthForSegmentType(segmentsLengths, type);
			// TODO: may want to generate new sequences for every repetition if length varies, probably easiest that way rather than adapting existing seq
			if (length != Integer.MAX_VALUE) {
				List<Triple<Harmony, Integer, Double>> harmSeq = model.sampleHarmonySequenceOfLength(length, Time.FOUR_FOUR, type, null);
				addHarmonyBySegmentType(score, harmSeq, type);
			}
		}
		
	}
	private void addHarmonyBySegmentType(Score score, List<Triple<Harmony, Integer, Double>> harmSeq, SegmentType type) {
		SegmentType prevType = null;
		
		int measureNumber = 0;
		for (Measure measure : score.getMeasures()) {
			if (measure.segmentType != prevType) {
				if (measure.segmentType == type) {
					for (Triple<Harmony, Integer, Double> triple : harmSeq) {
						score.addHarmony(measureNumber + triple.getSecond(), triple.getThird(), triple.getFirst());
					}
				}
				prevType = measure.segmentType;
			}
			measureNumber++;
		}
	}
	
	private int getMinLengthForSegmentType(List<Pair<SegmentType, Integer>> segmentsLengths, SegmentType type) {
		int length = Integer.MAX_VALUE;
		for (Pair<SegmentType, Integer> pair : segmentsLengths) {
			Integer segmentLength = pair.getSecond();
			if (pair.getFirst() == type && segmentLength < length) {
				length = segmentLength;
			}
		}
		return length;
	}
}
