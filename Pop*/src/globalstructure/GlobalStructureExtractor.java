package globalstructure;

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
		TreeMap<Integer, SegmentType> globalStructure = annotateGlobalStructureUsingAlignment(musicXML);
		musicXML.globalStructure = globalStructure;		
	}

	private static TreeMap<Integer, SegmentType> annotateGlobalStructureUsingAlignment(ParsedMusicXMLObject musicXML) {

		SequencePair.setCosts(1, -1, -1, 0);

		// Align the composition against itself, disallowing it to align identity-wise, and using lyrics
		Alignment aln = Aligner.alignSW(new MusicXMLPair(musicXML, musicXML, 0.0, 1.0, 1.0, 1.0));
		double score = aln.getFinalScore();


//		if (DEBUG) tabcomplete.utils.Utils.print2DMatrixInt(binary_matrix);
		
		
		return null;
	}

	private static TreeMap<Integer, SegmentType> annotateGlobalStructureNaively(ParsedMusicXMLObject musicXML) {
		// store start measures and segment types 
		TreeMap<Integer,SegmentType> globalStructure = new TreeMap<Integer, SegmentType>();
		
		// TODO:		
		return globalStructure;
	}

}
