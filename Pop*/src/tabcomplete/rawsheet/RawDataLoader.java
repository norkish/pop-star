package tabcomplete.rawsheet;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import tabcomplete.main.TabDriver;
import tabcomplete.utils.Utils;

public class RawDataLoader {
	private static final boolean DEBUG = false;

	final static String[] lyricSites = new String[] { "lyricsnet", "metrolyrics", "songlyrics" };
	final static String[] chordSites = new String[] { "echords", "ultimate-guitar" };
	final static String raw_cvsv_dir = TabDriver.dataDir + "/" + (TabDriver.mini_data_set?"":"new_") +"raw_csvs";

	final static String filter = "";

	private static Map<String, Map<String, List<LyricSheet>>> lyricSheetsByArtist = new HashMap<String, Map<String, List<LyricSheet>>>();
	private static Map<String, Map<String, List<ChordSheet>>> chordSheetsByArtist = new HashMap<String, Map<String, List<ChordSheet>>>();

	public static Map<String, Map<String, List<LyricSheet>>> loadLyricSheets()
			throws FileNotFoundException, IOException {
		List<CSVRecord> csvRecords;
		for (int s = 0; s < lyricSites.length; s++){
			String lyricSite = lyricSites[s];
			// parse each lyric sheet, perhaps differently depending on the site
			csvRecords = loadCSVRecordsForSite(lyricSite);

			for (int i = 1; i < csvRecords.size(); i++) {
				loadLyricSheet(csvRecords.get(i),s);
				if (i % 20000 == 0) System.out.println("Loaded " + i + " records...");
			}
			if (DEBUG) Utils.promptEnterKey("Check " + lyricSite + " output...");
		}

		return lyricSheetsByArtist;
	}

	public static Map<String, Map<String, List<ChordSheet>>> loadChordSheets()
			throws FileNotFoundException, IOException {
		List<CSVRecord> csvRecords;
		for (String site : chordSites) {
			csvRecords = loadCSVRecordsForSite(site);
			boolean ug = (site == "ultimate-guitar");
			for (int i = 1; i < csvRecords.size(); i++) {
				loadChordSheet(csvRecords.get(i), ug);
				if (i % 20000 == 0) System.out.println("Loaded " + i + " records...");
			}
			if (DEBUG) Utils.promptEnterKey("Check " + site + " output...");
		}
		return chordSheetsByArtist;
	}

	private static void loadChordSheet(CSVRecord csvRecord, boolean ug) {
		String artistName = csvRecord.get(ChordSheet.ARTIST_COL);
		if (ug) {
			artistName = artistName.substring(0, artistName.length() - 5);
			if (csvRecord.get(ChordSheet.TITLE_COL).contains("Guitar Pro")) {
				return;
			}
		}
		artistName = Utils.removeParen(artistName).trim();
		String artistKey = artistName.toLowerCase().replaceAll("[^A-Za-z0-9 ]", "");

		if (filter(artistName)) {
			return;
		}
		
		ChordSheet newChordSheet = null;
		try {
			newChordSheet = new ChordSheet(csvRecord, artistName, ug);
		} catch (Exception e) {
			System.out.println(csvRecord.toString());
			System.out.println(csvRecord.get(ChordSheet.URL_COL));
			e.printStackTrace();
			System.exit(-1);
		}
		if (newChordSheet.hasNoLyrics())
			return;
		
		String songKey = newChordSheet.getTitle().toLowerCase().replaceAll("[^a-z0-9 ]", "");
		
		Map<String, List<ChordSheet>> artistChordSheets = chordSheetsByArtist.get(artistKey);
		if (artistChordSheets == null) {
			artistChordSheets = new HashMap<String, List<ChordSheet>>();
			chordSheetsByArtist.put(artistKey, artistChordSheets);
		}

		List<ChordSheet> artistChordSheetsBySong = artistChordSheets.get(songKey);
		if (artistChordSheetsBySong == null) {
			artistChordSheetsBySong = new ArrayList<ChordSheet>();
			artistChordSheets.put(songKey, artistChordSheetsBySong);
		}

		if (DEBUG) {
			System.out.println(newChordSheet);
		}
		artistChordSheetsBySong.add(newChordSheet);
	}

	public static List<CSVRecord> loadCSVRecordsForSite(String site) throws FileNotFoundException, IOException {
		System.out.print("Loading CSVs for " + site + "... ");
		FileReader csvFileReader = new FileReader(raw_cvsv_dir + "/" + site + ".csv");
		CSVParser csvParser = new CSVParser(csvFileReader, CSVFormat.RFC4180);
		List<CSVRecord> csvRecords = csvParser.getRecords();
		System.out.println(csvRecords.size() + " loaded");
		csvParser.close();

		return csvRecords;
	}

	public static void loadLyricSheet(CSVRecord csvRecord,int provider) {
		String artistName = Utils.removeParen(csvRecord.get(LyricSheet.ARTIST_COL)).trim();
		int idxFeat = artistName.indexOf(" Feat."); // Remove "featuring so and so" denoted by "Feat."
		if(idxFeat != -1)
		{
			artistName = artistName.substring(0, idxFeat);
		}
		
		String artistKey = artistName.toLowerCase().replaceAll("[^a-z0-9 ]", "");
		if (filter(artistName)) {
			return;
		}

		LyricSheet newLyricSheet = new LyricSheet(csvRecord, artistName,provider);
		if (newLyricSheet.hasNoLyrics())
			return;
		
		String songKey = newLyricSheet.getTitle().toLowerCase().replaceAll("[^a-z0-9 ]", "");

		Map<String, List<LyricSheet>> artistLyricSheets = lyricSheetsByArtist.get(artistKey);
		if (artistLyricSheets == null) {
			artistLyricSheets = new HashMap<String, List<LyricSheet>>();
			lyricSheetsByArtist.put(artistKey, artistLyricSheets);
		}

		List<LyricSheet> artistLyricSheetsBySong = artistLyricSheets.get(songKey);
		if (artistLyricSheetsBySong == null) {
			artistLyricSheetsBySong = new ArrayList<LyricSheet>();
			artistLyricSheets.put(songKey, artistLyricSheetsBySong);
		}

		if (DEBUG) System.out.println(newLyricSheet);
		artistLyricSheetsBySong.add(newLyricSheet);
	}

	private static boolean filter(String artistName) {
		if (filter.equals(""))
			return false;
		else
			return (!filter.equals(artistName));
	}

}
