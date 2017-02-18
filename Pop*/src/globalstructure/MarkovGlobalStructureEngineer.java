package globalstructure;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;

import org.tc33.jheatchart.HeatChart;

import condition.ExactUnaryMatch;
import constraint.Constraint;
import data.MusicXMLModel;
import data.MusicXMLModelLearner;
import data.ParsedMusicXMLObject;
import markov.SparseNHMM;
import markov.SparseSingleOrderMarkovModel;
import utils.Pair;
import utils.Triple;
import utils.Utils;

public class MarkovGlobalStructureEngineer extends GlobalStructureEngineer {
	public static class MarkovGlobalStructureEngineerMusicXMLModel extends MusicXMLModel {

		private Map<Integer,Integer> segmentCountPerStructureDistribution = new TreeMap<Integer,Integer>();
		private int structureTotal = 0;
		
		private SparseSingleOrderMarkovModel<SegmentType> markovModel;
		private Map<SegmentType,Integer> statesByIndex = new HashMap<SegmentType,Integer>();
		private Map<Integer, Integer> priorCounts = new HashMap<Integer, Integer>();
		private Map<Integer,Map<Integer,Integer>> transitionCounts = new HashMap<Integer,Map<Integer, Integer>>();
		private Map<Integer,Integer> transitionTotals = new HashMap<Integer, Integer>();
		
		@Override
		public void trainOnExample(ParsedMusicXMLObject musicXML) {
			markovModel = null;
			
			SortedMap<Integer, Triple<SegmentType, Integer, Double>> globalStructureByFormStart = musicXML.getGlobalStructureByFormStart();

			Utils.incrementValueForKey(segmentCountPerStructureDistribution,globalStructureByFormStart.size()+1); // add one for "end" token
			structureTotal++;
			
			Integer prevTypeIdx = -1;
			for (Triple<SegmentType, Integer, Double> segmentTypeTriple : globalStructureByFormStart.values()) {
				SegmentType type = segmentTypeTriple.getFirst();
				
				Integer typeIdx = statesByIndex.get(type);
				if (typeIdx == null) {
					typeIdx = statesByIndex.size();
					statesByIndex.put(type, typeIdx);
				}
				
				if (prevTypeIdx == -1) {
					Utils.incrementValueForKey(priorCounts, typeIdx);
				} else {
					Utils.incrementValueForKeys(transitionCounts, prevTypeIdx, typeIdx);
					Utils.incrementValueForKey(transitionTotals, prevTypeIdx);
				}
				
				prevTypeIdx = typeIdx;
			}
			
			//add end token
			SegmentType type = null;
			
			Integer typeIdx = statesByIndex.get(type);
			if (typeIdx == null) {
				typeIdx = statesByIndex.size();
				statesByIndex.put(type, typeIdx);
			}
			
			if (prevTypeIdx == -1) {
				Utils.incrementValueForKey(priorCounts, typeIdx);
			} else {
				Utils.incrementValueForKeys(transitionCounts, prevTypeIdx, typeIdx);
				Utils.incrementValueForKey(transitionTotals, prevTypeIdx);
			}
		}

		Random rand = new Random();
		public int sampleMeasureCountForSegmentType() {
			int offsetIntoDistribution = rand.nextInt(structureTotal);
			
			int accum = 0;
			for (Entry<Integer, Integer> entry : segmentCountPerStructureDistribution.entrySet()) {
				accum += entry.getValue();
				if (accum >= offsetIntoDistribution) {
					return entry.getKey();
				}
			}
			
			throw new RuntimeException("Should never reach here");
		}
		
		public GlobalStructure generateStructure() {
			int structureLength = sampleMeasureCountForSegmentType();
			if (markovModel == null) {
				markovModel = buildModel();
			}
			ArrayList<Pair<Integer,Constraint<SegmentType>>> constraints = new ArrayList<Pair<Integer,Constraint<SegmentType>>>();
			// last token must be an, i.e., null
			constraints.add(new Pair<Integer,Constraint<SegmentType>>(structureLength-1, new Constraint<SegmentType>(new ExactUnaryMatch<SegmentType>(new SegmentType[]{null}), true))); 
			SparseNHMM<SegmentType> constrainedModel = new SparseNHMM<SegmentType>(markovModel,structureLength,constraints);
			List<SegmentType> segmentTypeSequence = constrainedModel.generate(structureLength);
			assert segmentTypeSequence.get(structureLength-1) == null : "sequence didn't end with expected null token";
			segmentTypeSequence = segmentTypeSequence.subList(0, segmentTypeSequence.size()-1); // remove "end" token, i.e. null
			return new GlobalStructure(segmentTypeSequence.toArray(new SegmentType[0]));
		}

		private SparseSingleOrderMarkovModel<SegmentType> buildModel() {
			Map<Integer, Double> priors = computePriors(priorCounts);
			Map<Integer, Map<Integer, Double>> transitions = computeTransitionProbabilities(transitionCounts);
			
			return new SparseSingleOrderMarkovModel<SegmentType>(statesByIndex, priors, transitions);
		}

		@Override
		public void toGraph() {
			segmentTransitionProbabilitiesDistributionToGraph();
			segmentCountPerStructureDistributionToGraph();
		}

		private void segmentTransitionProbabilitiesDistributionToGraph() {
			Map<Integer, Double> priors = computePriors(priorCounts);
			Map<Integer, Map<Integer, Double>> transitions = computeTransitionProbabilities(transitionCounts);
			
			boolean sortByFrequency = true;
			//y-axis is from
			Map<SegmentType, Integer> statesByIndexSorted = null;
			if (sortByFrequency) {
				statesByIndexSorted = new LinkedHashMap<SegmentType,Integer>();
				Map<Integer, Integer> frequencyByID = new HashMap<Integer,Integer>();
				//initialize frequencies
				for (Integer typeID : statesByIndex.values()) {
					frequencyByID.put(typeID, 0);
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
				SegmentType[] typeByID = new SegmentType[statesByIndex.size()];
				for (SegmentType type : statesByIndex.keySet()) {
					Integer typeID = statesByIndex.get(type);
					typeByID[typeID] = type;
				}
				
				for (Map.Entry<Integer, Integer> entry : harmonyIDSortedByFrequency) {
					statesByIndexSorted.put(typeByID[entry.getKey()], entry.getKey());
				}
				
			} else {
				statesByIndexSorted = statesByIndex;
			}
			
			// +1 for priors
			int chartXDimension = statesByIndexSorted.size(); 
			int chartYDimension = statesByIndexSorted.size(); 

			String[] xValues = new String[chartXDimension];
			String[] yValues = new String[chartYDimension];
			double[][] chartValues = new double[chartYDimension][chartXDimension];
			
			// set axis labels
			yValues[0] = "START";
			xValues[chartXDimension-1] = "END";
			
			int i = 0;
			for (SegmentType type : statesByIndexSorted.keySet()) {
				if (type == null) continue;
				Integer typeID = statesByIndexSorted.get(type);
				if (i >= chartXDimension && i >= chartYDimension)
					break;
				
				if (i < chartXDimension) {
					xValues[i] = type.toString();
					Double prior = priors.get(typeID);
					chartValues[0][i] = prior == null ? 0.0 : prior;
				}
				if (i < chartYDimension) {
					yValues[i+1] = type.toString();
				}
				i++;
			}
			
			// populate heatchart
			
			int y = 0;
			for (SegmentType yType : statesByIndexSorted.keySet()) {
				if (yType == null) continue;
				if (y >= chartYDimension) {
					break;
				}
				Integer yTypeId = statesByIndexSorted.get(yType);
				int x = 0;
				for (SegmentType xType : statesByIndexSorted.keySet()) {
					if (x >= chartXDimension) {
						break;
					}
					Integer xTypeId = statesByIndexSorted.get(xType);
					Double prob = null; 
					Map<Integer, Double> probsFromHarmony = transitions.get(yTypeId);
					if (probsFromHarmony != null) {
						prob = probsFromHarmony.get(xTypeId);
					}
					if (xType != null) { // putting end token at end of map
						chartValues[y+1][x] = prob == null ? 0.0 : prob;
						x++;
					} else {
						chartValues[y+1][chartXDimension-1] = prob == null ? 0.0 : prob;
					}
				}
				y++;
			}
			
			Utils.normalizeByFirstDimension(chartValues);

			HeatChart chart = new HeatChart(chartValues);
			chart.setHighValueColour(Color.RED);
			chart.setLowValueColour(Color.BLUE);
			
			chart.setYAxisLabel("Previous Segment Type");
			chart.setXAxisLabel("Next Segment Type");
			chart.setXValues(xValues);
			chart.setYValues(yValues);
			
			try {
				chart.saveToFile(new File(GRAPH_DIR + "/segment_transitions.jpeg"));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}		}

		private void segmentCountPerStructureDistributionToGraph() {
//			Map<Integer, Integer> segmentCountPerStructureDistribution;
			int maxLength = 0;
			for (Integer measureCount : segmentCountPerStructureDistribution.keySet()) {
				if (measureCount > maxLength) {
					maxLength = measureCount;
				}
			}
			
			// +1 for priors
			int chartXDimension = maxLength; 
			int chartYDimension = 1; 

			double[][] chartValues = new double[chartYDimension][chartXDimension];
			
			// set axis labels
			// x-axis labels set automatically 0 to n-1
			
			// populate heatchart
			
			for (int j = 0; j < chartXDimension; j++) {
				Integer distributionValue = segmentCountPerStructureDistribution.get(j);
				chartValues[0][j] = distributionValue == null? 0.0: distributionValue;
			}
			
			Utils.normalizeByFirstDimension(chartValues);

			HeatChart chart = new HeatChart(chartValues);
			chart.setHighValueColour(Color.RED);
			chart.setLowValueColour(Color.BLUE);
			
			chart.setShowYAxisValues(false);
			chart.setXAxisLabel("Segment Count");
			
			try {
				chart.saveToFile(new File(GRAPH_DIR + "/segment_count_per_song.jpeg"));
			} catch (IOException e) {
				e.printStackTrace();
			}			
		}
	}

	private MarkovGlobalStructureEngineerMusicXMLModel model;

	public MarkovGlobalStructureEngineer() {
		this.model = (MarkovGlobalStructureEngineerMusicXMLModel) MusicXMLModelLearner.getTrainedModel(this.getClass());
	}
	
	@Override
	public GlobalStructure generateStructure() {
		return model.generateStructure();
	}
}