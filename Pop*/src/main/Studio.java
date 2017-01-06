package main;

import composition.Composition;
import manager.Manager;

public class Studio {

	public Composition generate() {

		Composition newComposition = new Composition();

		Manager manager = new Manager();

		newComposition.generateInspiration(manager.getInspirationEngineer());

		newComposition.generateGlobalStructure(manager.getGlobalStructureEngineer());
		
		newComposition.generateSegmentStructure(manager.getSegmentStructureEngineer());
		
		newComposition.instantiateScoreWithSegmentStructure(manager.getSegmentStructureEngineer());

		newComposition.generateHarmony(manager.getHarmonyEngineer());

		newComposition.generateMelody(manager.getMelodyEngineer());

		newComposition.generateLyrics(manager.getLyricalEngineer());
		
		return newComposition;
	}
	
}
