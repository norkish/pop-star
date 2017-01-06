package composition;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import globalstructure.GlobalStructure;
import globalstructure.GlobalStructureEngineer;
import globalstructure.SegmentType;
import harmony.HarmonyEngineer;
import inspiration.Inspiration;
import inspiration.InspirationEngineer;
import lyrics.LyricalEngineer;
import melody.MelodyEngineer;
import segmentstructure.SegmentStructure;
import segmentstructure.SegmentStructureEngineer;

public class Composition {

	private String title = "BSSF";
	private String composer = "Pop*";
	private Inspiration inspiration;
	private GlobalStructure globalStructure;
	Map<SegmentType, SegmentStructure> indexedSegmentStructures;
	
	Score score;

	public void generateInspiration(InspirationEngineer inspirationEngineer) {
		this.inspiration = inspirationEngineer.generateInspiration();
	}

	private static String loadXMLTemplate() {
		String text = null;
		try {
			text = new String(Files.readAllBytes(Paths.get("template.xml")), StandardCharsets.UTF_8);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return text;
	}

	public void generateGlobalStructure(GlobalStructureEngineer globalStructureEngineer) {
		this.globalStructure = globalStructureEngineer.generateStructure();		
	}
	
	public void generateSegmentStructure(SegmentStructureEngineer segmentStructureEngineer) {
		// Substructure engineer designs a blueprint for each of the segments established by the global structure
		// Instantiations of that blueprint may vary, but it is the blueprint we are defining as substructure
		indexedSegmentStructures = new HashMap<SegmentType,SegmentStructure>();
		
		for (SegmentType segmentType : globalStructure) {
			if (!indexedSegmentStructures.containsKey(segmentType)) {
				indexedSegmentStructures.put(segmentType, segmentStructureEngineer.defineSegmentStructure(segmentType));
			}
		}
	}

	public void instantiateScoreWithSegmentStructure(SegmentStructureEngineer segmentStructureEngineer) {
		score = new Score();
		
		for (int i = 0; i < globalStructure.size(); i++) {
			// gather data relevant to instantiation
			SegmentType segmentType = globalStructure.get(i);
			boolean lastSegment = (i == globalStructure.size()-1);
			boolean lastOfKind = (globalStructure.lastIndexOf(segmentType) == i);
			
			SegmentStructure segmentStructure = indexedSegmentStructures.get(segmentType);
			List<Measure> instantiatedMeasures = segmentStructureEngineer.instantiateSegmentStructure(segmentType, segmentStructure, lastOfKind, lastSegment);
			score.addMeasures(instantiatedMeasures);
		}
	}

	public void generateHarmony(HarmonyEngineer harmonyEngineer) {
		harmonyEngineer.addHarmony(inspiration, score);
	}

	public void generateMelody(MelodyEngineer melodyEngineer) {
		melodyEngineer.addMelody(inspiration, score);
	}

	public void generateLyrics(LyricalEngineer lyricalEngineer) {
		lyricalEngineer.addLyrics(inspiration, score);
	}
	
	public String toString(){
		String xml = loadXMLTemplate();
		
		xml = xml.replaceFirst("TITLE-PLACEHOLDER", title);
		xml = xml.replaceFirst("COMPOSER-PLACEHOLDER", composer);
		xml = xml.replaceFirst("LYRICIST-PLACEHOLDER", StringUtils.capitalize(inspiration.getMaxEmotion()));
		xml = xml.replaceFirst("PART-PLACEHOLDER\n", score.toXML(2));
		
		return xml;
	}
}
