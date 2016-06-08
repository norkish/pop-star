package main;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import alignment.TabValidator;
import raw.ChordSheet;
import raw.LyricSheet;
import raw.RawDataLoader;
import utils.Serializer;

public class Driver {
	
	private static boolean deserialize = true;
	private static boolean serialize = false;
	
	private static String serializedDataDir = "/Users/norkish/Archive/2015_BYU/ComputationalCreativity/data/ser";
	private static String serializedLyrics = serializedDataDir + "/lyrics.ser";
	private static String serializedTabs = serializedDataDir + "/tabs.ser";
	
	public static void main(String[] args) throws IOException {
		
		Map<String, Map<String, List<LyricSheet>>> lyricSheets = (deserialize? (Map<String, Map<String, List<LyricSheet>>>) Serializer.load(serializedLyrics): RawDataLoader.loadLyricSheets());
		if (lyricSheets == null) return;
		if(!deserialize && serialize) {
			Serializer.serialize(lyricSheets, serializedLyrics);
		}
		System.out.println(LyricSheet.parseSummary());
		
//		for(String key:lyricSheets.keySet())
//		{
//			System.out.println(key + ":" + lyricSheets.get(key).size());
//			for (String key2: lyricSheets.get(key).keySet())
//				System.out.println("\t" + key2 + ":" + lyricSheets.get(key).get(key2).size());
//		}
		
		Map<String, Map<String, List<ChordSheet>>> chordSheets = (deserialize? (Map<String, Map<String, List<ChordSheet>>>) Serializer.load(serializedTabs): RawDataLoader.loadChordSheets());
		if (chordSheets == null) return;
		if(!deserialize && serialize) {
			Serializer.serialize(chordSheets, serializedTabs);
		}
		System.out.println(ChordSheet.parseSummary());
//		for(String key:chordSheets.keySet())
//		{
//			System.out.println(key + ":" + chordSheets.get(key).size());
//			for (String key2: chordSheets.get(key).keySet())
//				System.out.println("\t" + key2 + ":" + chordSheets.get(key).get(key2).size());
//		}
//		Utils.promptEnterKey("");
		
//		TabValidator.validateTabs(lyricSheets, chordSheets);
	}
}