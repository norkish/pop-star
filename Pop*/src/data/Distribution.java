package data;

import java.util.Map;
import java.util.Random;

public class Distribution<T> {

	private Map<T, Integer> distribution;
	private int accumulatedCount;
	
	private static Random rand = new Random();

	public Distribution(Map<T, Integer> distribution, int accumulatedCount) {
		this.distribution = distribution;
		this.accumulatedCount = accumulatedCount;
	}

	public T sampleRandomly() {
		int target = rand.nextInt(accumulatedCount);
		int accumulation = 0;
		for (Map.Entry<T, Integer> entry : distribution.entrySet())
		{
			accumulation += entry.getValue();
			
			if (accumulation > target) {
				return entry.getKey();
			}
		}
		
		assert(false);
		return null;
	}
	
	public String toString() {
		StringBuilder bldr = new StringBuilder();
		
		for (Map.Entry<T, Integer> entry : distribution.entrySet())
		{
			bldr.append(entry);
			bldr.append('\n');
		}
		
		return bldr.toString();
	}
}
