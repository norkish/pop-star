package data;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

import main.TabDriver;
import tab.CompletedTab;
import utils.Utils;

public class DataLoader {

	private static Distribution<String> gStructDistribution;

	static {
		loadDistribution();
	}

	private static void loadDistribution() {
		
		Map<String, Integer> gStructureDistribution = new HashMap<String, Integer>();
		
		List<CompletedTab> tabs = TabDriver.loadValidatedTabs();

		String gStructure;
		Integer count;
		int accumulatedCount = 0;
		for (CompletedTab completedTab : tabs) {
			
			// extract structure from tab
			gStructure = extractStructure(completedTab);
			count = gStructureDistribution.get(gStructure);
			if (count == null) {
				gStructureDistribution.put(gStructure, 1);
			} else {
				gStructureDistribution.put(gStructure, count + 1);
			}
			
			// extract other?
			
			accumulatedCount++;
		}
		
		gStructDistribution = new Distribution<String>(Utils.sort(gStructureDistribution, false), accumulatedCount);
	}

	private static String extractStructure(CompletedTab completedTab) {
		StringBuilder bldr = new StringBuilder();
		
		char prev = '\0';
		for (char c : completedTab.structure) {
			if (c != prev) {
				bldr.append(c);
				prev = c;
			}
		}
		
		return bldr.toString();
	}
	
	private static Distribution<String> getGlobalStructureDistribution() {
		return gStructDistribution;
	}
	
	public static void main(String[] args) {
		Distribution<String> gStructDist = DataLoader.getGlobalStructureDistribution();
		
		System.out.println(gStructDist);
		
		for (int i = 0; i < 10; i++) {
			System.out.println(gStructDist.sampleRandomly());
		}
	}
}
