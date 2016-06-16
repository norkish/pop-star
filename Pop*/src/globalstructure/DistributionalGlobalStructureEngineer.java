package globalstructure;

import data.DataLoader;
import data.BackedDistribution;

public class DistributionalGlobalStructureEngineer extends GlobalStructureEngineer {

	private BackedDistribution<String> globalStructDistribution = DataLoader.getGlobalStructureDistribution();
	
	@Override
	public GlobalStructure generateStructure() {
		return new GlobalStructure(globalStructDistribution.sampleRandomly());
	}

}
