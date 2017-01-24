package globalstructure;

import java.util.TreeMap;

import data.ParsedMusicXMLObject;

public class GlobalStructureExtractor {

	public static void annotateGlobalStructure(ParsedMusicXMLObject musicXML) {
		// store start measures and segment types 
		TreeMap<Integer,SegmentType> globalStructure = new TreeMap<Integer, SegmentType>();
		
		// loop over measures, deciding where new segments start
		
		// note that verses and choruses have to match to some minimal extent
		// TODO:		
		musicXML.globalStructure = globalStructure;		
	}

}
