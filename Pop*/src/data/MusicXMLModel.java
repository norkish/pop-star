package data;

import java.awt.Font;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public abstract class MusicXMLModel {

	public static final Font CHART_LABEL_FONT = new Font("Times", Font.BOLD, 28);
	public static final Font CHART_AXIS_FONT = new Font("Times", Font.PLAIN, 24);

	public abstract void trainOnExample(ParsedMusicXMLObject musicXML);

	protected final String GRAPH_DIR = "model_summaries";

	public abstract void toGraph();
	
	protected Map<Integer, Map<Integer, Double>> computeTransitionProbabilities(Map<Integer, Map<Integer,Integer>> transitionCounts) {
		double totalCount;
		Map<Integer,Map<Integer,Double>> transitions = new HashMap<Integer,Map<Integer, Double>>();
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
		return transitions;
	}

	protected Map<Integer, Double> computePriors(Map<Integer, Integer> priorCounts) {
		Map<Integer, Double> priors = new HashMap<Integer, Double>();
		if (priorCounts == null) return priors;
		
		double totalCount = 0;
		for (Integer count: priorCounts.values()) {
			totalCount += count;
		}
		for (Entry<Integer, Integer> entry : priorCounts.entrySet()) {
			priors.put(entry.getKey(), entry.getValue()/totalCount);
		}
		return priors;
	}

	public static class ValueThenKeyComparator<K extends Comparable<? super K>, V extends Comparable<? super V>>
			implements Comparator<Map.Entry<K, V>> {

		public int compare(Map.Entry<K, V> a, Map.Entry<K, V> b) {
			int cmp1 = -a.getValue().compareTo(b.getValue());
			if (cmp1 != 0) {
				return cmp1;
			} else {
				return a.getKey().compareTo(b.getKey());
			}
		}

	}

}
