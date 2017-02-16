package harmony;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.tc33.jheatchart.HeatChart;

import composition.Measure;
import composition.Score;
import constraint.Constraint;
import data.MusicXMLModel;
import data.MusicXMLModelLearner;
import data.MusicXMLParser.Harmony;
import data.ParsedMusicXMLObject;
import globalstructure.SegmentType;
import inspiration.Inspiration;
import markov.SparseNHMM;
import markov.SparseSingleOrderMarkovModel;
import utils.Pair;
import utils.Triple;
import utils.Utils;

public class SimpleSegmentSpecificHarmonyEngineer extends HarmonyEngineer {

	public static class SegmentSpecificHarmonyEngineerMusicXMLModel extends MusicXMLModel {

		SparseSingleOrderMarkovModel<Harmony> markovModel;
		Map<Harmony,Integer> statesByIndex = new HashMap<Harmony,Integer>();
		Map<Integer, Integer> priorCounts = new HashMap<Integer, Integer>();
		Map<Integer,Map<Integer,Integer>> transitionCounts = new HashMap<Integer,Map<Integer, Integer>>();
		
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
			markovModel = null;
			
			Integer prevHarmIdx = -1;
			
			// TODO: condition on duration, change to roman numeral chord names, condition on segment type
			for (Triple<Integer, Integer, Harmony> triple : musicXML.unoverlappingHarmonyByPlayedMeasure){
				Harmony harmony = triple.getThird();
				if (harmony == null)
					continue;
				
				Integer harmIdx = statesByIndex.get(harmony);
				if (harmIdx == null) {
					harmIdx = statesByIndex.size();
					statesByIndex.put(harmony, harmIdx);
				}
				
				if (prevHarmIdx == -1) {
					Utils.incrementValueForKey(priorCounts, harmIdx);
				} else {
					Utils.incrementValueForKeys(transitionCounts, prevHarmIdx, harmIdx);
				}
				
				prevHarmIdx = harmIdx;
			}
			
		}
		
		public String toString() {
			StringBuilder str = new StringBuilder();
			
			// string representation of model
			
			return str.toString();
		}

		public List<Harmony> sampleHarmonySequenceOfLength(int length, SegmentType type, List<Constraint<Harmony>> contextualConstraints) {
			// TODO Add constraints according to type, including constraints which depend on the harm sequence 
			if (markovModel == null) {
				markovModel = buildModel();
			}
			SparseNHMM<Harmony> constrainedModel = new SparseNHMM<Harmony>(markovModel,length,new ArrayList<Pair<Integer,Constraint<Harmony>>>());
			return constrainedModel.generate(length);
		}

		private SparseSingleOrderMarkovModel<Harmony> buildModel() {
			Map<Integer, Double> priors = computePriors(priorCounts);
			Map<Integer, Map<Integer, Double>> transitions = computeTransitionProbabilities(transitionCounts);
			
			return new SparseSingleOrderMarkovModel<Harmony>(statesByIndex, priors, transitions);
		}

		@Override
		public void toGraph() {
			Map<Integer, Double> priors = computePriors(priorCounts);
			Map<Integer, Map<Integer, Double>> transitions = computeTransitionProbabilities(transitionCounts);
			int maxXValues = 50;
			int maxYValues = 10;
			
			boolean sortByFrequency = true;
			//y-axis is from
			Map<Harmony, Integer> statesByIndexSorted = null;
			if (sortByFrequency) {
				statesByIndexSorted = new LinkedHashMap<Harmony,Integer>();
				Map<Integer, Integer> frequencyByID = new HashMap<Integer,Integer>();
				//initialize frequencies
				for (Integer harmonyID : statesByIndex.values()) {
					frequencyByID.put(harmonyID, 0);
				}
				
				// count frequencies
				for (Map<Integer, Integer> toMap : transitionCounts.values()) {
					for (Integer harmonyID : toMap.keySet()) {
						Integer count = toMap.get(harmonyID);
						frequencyByID.put(harmonyID, frequencyByID.get(harmonyID)+count);
					}
				}
				
				// sort by counts (and then by ID)
				List<Map.Entry<Integer, Integer>> harmonyIDSortedByFrequency = new ArrayList<Map.Entry<Integer, Integer>>(frequencyByID.entrySet());
				Collections.sort(harmonyIDSortedByFrequency, new ValueThenKeyComparator<Integer, Integer>());
				
				// get reverse map to create new Harmony-ID map that is sorted by frequency
				Harmony[] harmonyByID = new Harmony[statesByIndex.size()];
				for (Harmony harmony : statesByIndex.keySet()) {
					Integer harmonyID = statesByIndex.get(harmony);
					harmonyByID[harmonyID] = harmony;
				}
				
				for (Map.Entry<Integer, Integer> entry : harmonyIDSortedByFrequency) {
					statesByIndexSorted.put(harmonyByID[entry.getKey()], entry.getKey());
				}
				
			} else {
				statesByIndexSorted = statesByIndex;
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
	
	public SimpleSegmentSpecificHarmonyEngineer() {
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
				List<Harmony> harmSeq = model.sampleHarmonySequenceOfLength(length, type, null);
				addHarmonyBySegmentType(score, harmSeq, type);
			}
		}
		
	}
	private void addHarmonyBySegmentType(Score score, List<Harmony> harmSeq, SegmentType type) {
		SegmentType prevType = null;
		int counter = -1;
		for (Measure measure : score.getMeasures()) {
			if (measure.segmentType != prevType) {
				if (measure.segmentType == type) {
					counter = 0;
				}
				prevType = measure.segmentType;
			}
			
			if (measure.segmentType == type) {
				measure.addHarmony(0.0, harmSeq.get(counter));
				counter++;
			}
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
