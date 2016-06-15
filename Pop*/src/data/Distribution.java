package data;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

public class Distribution<T> {

	private Map<T, List<Integer>> distribution;
	private int accumulatedCount = 0;
	
	private static Random rand = new Random();

	public Distribution(Map<T, List<Integer>> distribution) {
		this.distribution = distribution;
		for (List<Integer> i : distribution.values()) {
			accumulatedCount += i.size();
		}
		
	}

	public T sampleRandomly() {
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
