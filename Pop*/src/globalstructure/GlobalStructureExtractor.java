package globalstructure;

import java.util.SortedMap;
import java.util.TreeMap;

import data.ParsedMusicXMLObject;
import tabcomplete.alignment.Aligner;
import tabcomplete.alignment.Alignment;
import tabcomplete.alignment.MusicXMLPair;
import tabcomplete.alignment.SequencePair;

public class GlobalStructureExtractor {

	public static void annotateGlobalStructure(ParsedMusicXMLObject musicXML) {
		
		if (musicXML.unoverlappingHarmonyByMeasure.isEmpty() || musicXML.notesByMeasure.isEmpty()) {
			musicXML.globalStructure = null;
			return;
		}

//		TreeMap<Integer, SegmentType> globalStructure = annotateGlobalStructureNaively(musicXML);
//		TreeMap<Integer, SegmentType> globalStructure = annotateGlobalStructureUsingAlignment(musicXML);
		SortedMap<Integer, SegmentType> globalStructure = annotateGlobalStructureUsingFixed(musicXML);
		musicXML.globalStructure = globalStructure;		
	}

	private static SortedMap<Integer, SegmentType> annotateGlobalStructureUsingFixed(ParsedMusicXMLObject musicXML) {
		SortedMap<Integer, SegmentType> structure = new TreeMap<Integer, SegmentType>();
		
		structure.put(0, SegmentType.INTRO);
		structure.put(4, SegmentType.VERSE);
		structure.put(32, SegmentType.CHORUS);
		structure.put(36, SegmentType.INTERLUDE);
		structure.put(38, SegmentType.VERSE);
		structure.put(66, SegmentType.CHORUS);
		structure.put(70, SegmentType.INTERLUDE);
		structure.put(72, SegmentType.BRIDGE);
		structure.put(0, SegmentType.INTRO);
		structure.put(0, SegmentType.INTRO);
		
		return structure;
	}

	private static SortedMap<Integer, SegmentType> annotateGlobalStructureUsingAlignment(ParsedMusicXMLObject musicXML) {

		SequencePair.setCosts(1, -10, -10, -10);

		// Align the composition against itself, disallowing it to align identity-wise, and using lyrics
		Alignment aln = Aligner.alignSW(new MusicXMLPair(musicXML, musicXML, 0.0, 1.0, 1.0, 1.0));
		double score = aln.getFinalScore();
		System.err.println(aln.toString());

//		if (DEBUG) tabcomplete.utils.Utils.print2DMatrixInt(binary_matrix);
		
		
		return null;
	}

	private static SortedMap<Integer, SegmentType> annotateGlobalStructureNaively(ParsedMusicXMLObject musicXML) {
		// store start measures and segment types 
		TreeMap<Integer,SegmentType> globalStructure = new TreeMap<Integer, SegmentType>();
		
		// TODO:		
		return globalStructure;
	}

}
