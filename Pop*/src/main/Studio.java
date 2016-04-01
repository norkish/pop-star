package main;

import composition.Composition;
import harmony.Harmony;
import harmony.HarmonyEngineer;
import inspiration.Inspiration;
import inspiration.InspirationEngineer;
import lyrics.LyricalEngineer;
import lyrics.Lyrics;
import manager.Manager;
import melody.Melody;
import melody.MelodyEngineer;
import structure.Structure;
import structure.StructureEngineer;

public class Studio {

	public Composition generate() {

		Composition newComposition = new Composition();

		Manager manager = new Manager();

		InspirationEngineer inspirationEngineer = manager.getInspirationEngineer();
		Inspiration inspiration = inspirationEngineer.generateInspiration();
		newComposition.setInspiration(inspiration);

		StructureEngineer structureEngineer = manager.getStructureEngineer();
		Structure structure = structureEngineer.generateStructure();
		newComposition.setStructure(structure);

		LyricalEngineer lyircalEngineer = manager.getLyricalEngineer();
		Lyrics lyrics = lyircalEngineer.generateLyrics(inspiration, structure);
		newComposition.setLyrics(lyrics);

		HarmonyEngineer harmonyEngineer = manager.getHarmonyEngineer();
		Harmony harmony = harmonyEngineer.generateHarmony(inspiration, structure);
		newComposition.setHarmony(harmony);
		
		MelodyEngineer melodyEngineer = manager.getMelodyEngineer();
		Melody melody = melodyEngineer.generateMelody(inspiration, structure, lyrics, harmony);
		newComposition.setMelody(melody);
		
		return newComposition;
	}
	
}
