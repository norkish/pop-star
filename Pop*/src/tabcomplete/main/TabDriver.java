package tabcomplete.main;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import tabcomplete.rawsheet.ChordSheet;
import tabcomplete.rawsheet.LyricSheet;
import tabcomplete.rawsheet.RawDataLoader;
import tabcomplete.tab.CompletedTab;
import tabcomplete.utils.Serializer;
import tabcomplete.validate.TabValidator;

public class TabDriver {
	
	private static boolean deserializeLyrics = true;
	private static boolean serializeLyrics = false;
	private static boolean deserializeChords = false;
	private static boolean serializeChords = true;
	private static boolean deserializeValidatedTabs = false;
	private static boolean serializeValidatedTabs = true;

	public static boolean mini_data_set = false;
	private static boolean test_accuracy = true;
	
	public final static String dataDir = "../../data";
	private final static String serializedDataDir = dataDir + "/ser";
	private static String serializedLyrics = serializedDataDir + "/" + (mini_data_set?"":"new_") +"lyrics.ser";
	private static String serializedTabs = serializedDataDir + "/" + (mini_data_set?"":"new_") +"tabs.ser";
	private static String serializedCompleteTabs = serializedDataDir + "/" + (mini_data_set?"":"new_") +"complete_tabs.ser";
	private static String correctTabs = dataDir + "/complete_tabs";
	
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
			lyricSheets = (deserializeLyrics? (Map<String, Map<String, List<LyricSheet>>>) Serializer.load(serializedLyrics): RawDataLoader.loadLyricSheets());
			if (lyricSheets == null) return null;
			if(!deserializeLyrics && serializeLyrics) {
				Serializer.serialize(lyricSheets, serializedLyrics);
			}
			System.out.println(LyricSheet.parseSummary());
			
	//		for(String key:lyricSheets.keySet())
	//		{
	//			System.out.println(key + ":" + lyricSheets.get(key).size());
	//			for (String key2: lyricSheets.get(key).keySet())
	//				System.out.println("\t" + key2 + ":" + lyricSheets.get(key).get(key2).size());
	//		}
			
			chordSheets = (deserializeChords? (Map<String, Map<String, List<ChordSheet>>>) Serializer.load(serializedTabs): RawDataLoader.loadChordSheets());
			if (chordSheets == null) return null;
			if(!deserializeChords && serializeChords) {
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
			
			validatedTabs = (deserializeValidatedTabs? (List<CompletedTab>) Serializer.load(serializedCompleteTabs) : TabValidator.validateTabs(lyricSheets, chordSheets));
			if(!deserializeValidatedTabs && serializeValidatedTabs) {
				Serializer.serialize(validatedTabs, serializedCompleteTabs);
			}
			
			if (test_accuracy) {
				Set<CompletedTab> correctTabs = new HashSet<CompletedTab>();
				
				Random rand = new Random();
				while(correctTabs.size() < 1000 && correctTabs.size() < validatedTabs.size()) {
					correctTabs.add(validatedTabs.get(rand.nextInt(validatedTabs.size())));
				}
				
				for (CompletedTab completedTab : correctTabs) {
					PrintWriter writer  = new PrintWriter(correctTabs + "/" + completedTab.tabURL);
					
					writer.println(completedTab);
					
					writer.close();
				}
				
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
		
		return validatedTabs;
	}
}