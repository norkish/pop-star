package structure;

import globalstructure.GlobalStructureEngineer;
import globalstructure.SegmentType;

import java.util.Map;

import globalstructure.GlobalStructure;
import substructure.Substructure;
import substructure.SubstructureEngineer;

public class StructureEngineer {

	private GlobalStructureEngineer globalStructureEngineer = null;
	private SubstructureEngineer substructureEngineer = null;

	public void setGlobalStructureEngineer(GlobalStructureEngineer globalStructureEngineer) {
		this.globalStructureEngineer  = globalStructureEngineer;
	}

	public void setSubstructureEngineer(SubstructureEngineer substructureEngineer) {
		this.substructureEngineer = substructureEngineer;
	}

	public Structure generateStructure() {
		Structure structure = new Structure();
		
		GlobalStructure globalStructure = globalStructureEngineer.generateStructure();
		structure.setGlobalStructure(globalStructure);
		
		Map<SegmentType, Substructure[]> substructure = substructureEngineer.defineSubstructure(globalStructure);
		structure.setSubstructure(substructure);
		
		return structure;
	}

}
