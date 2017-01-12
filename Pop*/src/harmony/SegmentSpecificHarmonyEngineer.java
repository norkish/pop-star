package harmony;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
import utils.Utils;

public class SegmentSpecificHarmonyEngineer extends HarmonyEngineer {

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
			if (musicXML.harmonyCount == 0) {
				return;
			}

			// the current markovModel is now obselete and will be regenerated when next sampled
			markovModel = null;
			
			Integer prevHarmIdx = -1;
			
			// TODO: condition on duration, change to roman numeral chord names, condition on segment type
			for (Entry<Integer, Map<Integer, Harmony>> measureEntry : musicXML.unoverlappingHarmonyByMeasure.entrySet()){
//				int measure = measureEntry.getKey();
				for (Entry<Integer, Harmony> offsetHarmony : measureEntry.getValue().entrySet()) {
//					int offset = offsetHarmony.getKey();
					Harmony harmony = offsetHarmony.getValue();
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
			SparseNHMM<Harmony> constrainedModel = new SparseNHMM<Harmony>(markovModel,length,new ArrayList<Constraint<Harmony>>());
			return constrainedModel.generate(length);
		}

		private SparseSingleOrderMarkovModel<Harmony> buildModel() {
			Map<Integer, Double> priors = new HashMap<Integer, Double>();
			Map<Integer,Map<Integer,Double>> transitions = new HashMap<Integer,Map<Integer, Double>>();
			
			double totalCount = 0;
			for (Integer count: priorCounts.values()) {
				totalCount += count;
			}
			for (Entry<Integer, Integer> entry : priorCounts.entrySet()) {
				priors.put(entry.getKey(), entry.getValue()/totalCount);
			}
			
			for (Entry<Integer, Map<Integer, Integer>> outerEntry : transitionCounts.entrySet()) {
				Integer fromIdx = outerEntry.getKey();
				Map<Integer, Integer> innerMap = outerEntry.getValue();

				Map<Integer, Double> newInnerMap = new HashMap<Integer,Double>();
				transitions.put(fromIdx, newInnerMap);

				totalCount = 0;
				for (Integer count: innerMap.values()) {
					totalCount += count;
				}
				for (Entry<Integer, Integer> entry : innerMap.entrySet()) {
					newInnerMap.put(entry.getKey(), entry.getValue()/totalCount);
				}
			}
			
			return new SparseSingleOrderMarkovModel<Harmony>(statesByIndex, priors, transitions);
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
