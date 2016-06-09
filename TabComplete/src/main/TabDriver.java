package main;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import raw.ChordSheet;
import raw.LyricSheet;
import raw.RawDataLoader;
import tab.CompletedTab;
import utils.Serializer;
import validate.TabValidator;

public class TabDriver {
	
	private static boolean deserialize = true;
	private static boolean serialize = false;
	
	private static String serializedDataDir = "/Users/norkish/Archive/2015_BYU/ComputationalCreativity/data/ser";
	private static String serializedLyrics = serializedDataDir + "/lyrics.ser";
	private static String serializedTabs = serializedDataDir + "/tabs.ser";
	private static String serializedCompleteTabs = serializedDataDir + "/complete_tabs.ser";
	
	public static void main(String[] args) throws IOException {
		
		loadValidatedTabs();
	}

	/**
	 * @return 
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public static List<CompletedTab> loadValidatedTabs() {
		Map<String, Map<String, List<LyricSheet>>> lyricSheets = null;
		Map<String, Map<String, List<ChordSheet>>> chordSheets = null;
		List<CompletedTab> validatedTabs = null;
		try {
			lyricSheets = (deserialize? (Map<String, Map<String, List<LyricSheet>>>) Serializer.load(serializedLyrics): RawDataLoader.loadLyricSheets());
			if (lyricSheets == null) return null;
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
			
			chordSheets = (deserialize? (Map<String, Map<String, List<ChordSheet>>>) Serializer.load(serializedTabs): RawDataLoader.loadChordSheets());
			if (chordSheets == null) return null;
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
			
			validatedTabs = (deserialize? (List<CompletedTab>) Serializer.load(serializedCompleteTabs) : TabValidator.validateTabs(lyricSheets, chordSheets));
			if(!deserialize && serialize) {
				Serializer.serialize(validatedTabs, serializedCompleteTabs);
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
		return validatedTabs;
	}
}