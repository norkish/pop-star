package data;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;

public class BackedDistribution<T> {
	private final static Random rand = new Random();
	private int accumulatedCount = 0;
	private Map<T, List<Integer>> distribution;
	
	public BackedDistribution(Map<T, List<Integer>> distribution) {
		this.distribution = distribution;
		for (List<Integer> i : distribution.values()) {
			accumulatedCount += i.size();
		}
	}

	public T sampleAccordingToDistribution() {
		int target = rand.nextInt(accumulatedCount);
		int accumulation = 0;
		for (Map.Entry<T, List<Integer>> entry : distribution.entrySet())
		{
			accumulation += entry.getValue().size();
			
			if (accumulation > target) {
				return entry.getKey();
			}
		}
		
		return null;
	}
	
	public String toString() {
		StringBuilder bldr = new StringBuilder();
		
		for (Entry<T, List<Integer>> entry : distribution.entrySet())
		{
			bldr.append(entry);
			bldr.append('\n');
		}
		
		return bldr.toString();
	}
}
