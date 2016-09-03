package tabcomplete.main;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import tabcomplete.rawsheet.ChordSheet;
import tabcomplete.rawsheet.LyricSheet;
import tabcomplete.rawsheet.RawDataLoader;
import tabcomplete.tab.CompletedTab;
import tabcomplete.utils.Serializer;
import tabcomplete.validate.TabValidator;

public class TabDriver {
	
	private static boolean deserialize = false;
	private static boolean serialize = true;
	public static boolean mini_data_set = false;
	
	public final static String dataDir = "../data";
	private final static String serializedDataDir = dataDir + "/ser";
	private static String serializedLyrics = serializedDataDir + "/" + (mini_data_set?"":"new_") +"lyrics.ser";
	private static String serializedTabs = serializedDataDir + "/" + (mini_data_set?"":"new_") +"tabs.ser";
	private static String serializedCompleteTabs = serializedDataDir + "/" + (mini_data_set?"":"new_") +"complete_tabs.ser";
	
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