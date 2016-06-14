package globalstructure;

import data.DataLoader;
import data.Distribution;

public class DistributionalGlobalStructureEngineer extends GlobalStructureEngineer {

	private Distribution<String> globalStructDistribution = DataLoader.getGlobalStructureDistribution();
	
	@Override
	public GlobalStructure generateStructure() {
		return new GlobalStructure(globalStructDistribution.sampleRandomly());
	}

}
