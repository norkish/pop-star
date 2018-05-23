package main;

import java.util.Map;
import java.util.TreeMap;

import composition.Composition;
import config.SongConfiguration;
import manager.Manager;

public class Studio {

	public Composition generate() {

		Composition newComposition = null;

		Map<Integer,String> goodSeeds = new TreeMap<Integer,String>();
//		for (int i = 0; i < 100; i++) {
			SongConfiguration.randSeed = 76;
			try {
				newComposition = new Composition();
		
				Manager manager = new Manager();
		
				newComposition.generateInspiration(manager.getInspirationEngineer());
		
				newComposition.generateGlobalStructure(manager.getGlobalStructureEngineer());
				
				newComposition.generateSegmentStructure(manager.getSegmentStructureEngineer());
				
				newComposition.instantiateScoreWithSegmentStructure(manager.getSegmentStructureEngineer());
		
				newComposition.generateHarmony(manager.getHarmonyEngineer());
		
				newComposition.generateMelody(manager.getMelodyEngineer());
		
				newComposition.generateLyrics(manager.getLyricalEngineer());
//				goodSeeds.put(i,Arrays.toString(newComposition.getGlobalStructure().getGlobalStructure()));
//				break;
			} catch (Exception e) {
//				e.printStackTrace();
			}
//		}
//		for(Integer seed: goodSeeds.keySet()) {
//		System.out.println("GOOD SEED:" + seed + "\tGLOBAL STRUCTURE=" + goodSeeds.get(seed));
//		}
		
		return newComposition;
	}
	
}
