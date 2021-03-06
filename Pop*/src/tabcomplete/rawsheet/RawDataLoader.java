package tabcomplete.rawsheet;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import tabcomplete.main.TabDriver;
import tabcomplete.utils.Utils;

public class RawDataLoader {
	private static final boolean DEBUG = false;

	final static String[] lyricSites = new String[] { "lyricsnet", "metrolyrics", "songlyrics" };
//	final static String[] chordSites = new String[] { "echords", "ultimate-guitar" };
	final static String[] chordSites = new String[] { "ultimate-guitar" };
	final static String raw_cvsv_dir = TabDriver.dataDir + "/" + (TabDriver.mini_data_set?"":"new_") +"raw_csvs";

	private final static Set<String> filters = TabDriver.filters;

	private static Map<String, Map<String, List<LyricSheet>>> lyricSheetsByArtist = new HashMap<String, Map<String, List<LyricSheet>>>();
	private static Map<String, Map<String, List<ChordSheet>>> chordSheetsByArtist = new HashMap<String, Map<String, List<ChordSheet>>>();

	private static Map<String, Map<String, List<LyricSheet>>> lyricKeys = null;
	
	private static PrintWriter lyricsWriter = null;

	public static void main(String[] args) throws FileNotFoundException, IOException {
		loadLyricSheets(null);
	}
	
	public static Map<String, Map<String, List<LyricSheet>>> loadLyricSheets(Map<String, Map<String, String>> songs) throws FileNotFoundException, IOException {
		CSVParser csvRecordParser;
		Iterator<CSVRecord> csvRecordIterator;
		lyricsWriter = new PrintWriter(new File(TabDriver.dataDir + "/allArtistsAndSongs.txt"));
		for (int s = 0; s < lyricSites.length; s++){
			String lyricSite = lyricSites[s];
			// parse each lyric sheet, perhaps differently depending on the site
			csvRecordParser = loadCSVRecordParserForSite(lyricSite);
			csvRecordIterator = csvRecordParser.iterator();
			csvRecordIterator.next();
			for (int i = 1; csvRecordIterator.hasNext(); i++) {
				loadLyricSheet(csvRecordIterator.next(),s,songs);
				if (i % 20000 == 0) System.out.println("Scanned " + i + " records...");
				if (lyricSheetsByArtist.size() > 500)
					return lyricSheetsByArtist;
			}
			
			
			csvRecordParser.close();
			if (DEBUG) Utils.promptEnterKey("Check " + lyricSite + " output...");
		}
		lyricsWriter.close();
//		lyricsWriter = new PrintWriter(new File(TabDriver.dataDir + "/plainLyrics.txt"));
//		for (Map<String, List<LyricSheet>> songsByArtist : lyricSheetsByArtist.values()) {
//			for (List<LyricSheet> songsByName : songsByArtist.values()) {
//				if (songsByName.size() > 0){
//					String song = songsByName.get(0).getLyrics();
//					String[] words = song.split("\\s+");
//					for (String word : words) {
//						lyricsWriter.print(word.replaceFirst("^[^a-zA-Z]+", "").replaceFirst("[^a-zA-Z]+$", ""));
//						lyricsWriter.print(" ");
//					}
//					lyricsWriter.println();
//				}
//			}
//		}
//		lyricsWriter.close();

		return lyricSheetsByArtist;		
	}
	public static Map<String, Map<String, List<ChordSheet>>> loadChordSheets()
			throws FileNotFoundException, IOException {
		CSVParser csvRecordParser;
		Iterator<CSVRecord> csvRecordIterator;
		for (String site : chordSites) {
			csvRecordParser = loadCSVRecordParserForSite(site);
			csvRecordIterator = csvRecordParser.iterator();
			
			csvRecordIterator.next();
			boolean ug = (site == "ultimate-guitar");
			for (int i = 1; csvRecordIterator.hasNext(); i++) {
				loadChordSheet(csvRecordIterator.next(), ug);
				if (i % 10000 == 0) System.out.println("Scanned " + i + " records...");
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
		String artistKey = artistName.toLowerCase().replaceAll("&", "and").replaceAll("[^A-Za-z0-9 ]", "").replaceFirst("^the ", "");

		if (filter(artistKey) || lyricKeys != null && !lyricKeys.containsKey(artistKey)) {
			return;
		}
		
		ChordSheet newChordSheet = null;
		try {
			newChordSheet = new ChordSheet(csvRecord, artistName, ug);
		} catch (Exception e) {
			System.err.print(csvRecord.get(ChordSheet.URL_COL) + ":\t");
			e.printStackTrace();
			ChordSheet.malformattedTabs++;
		}
		if (newChordSheet == null || newChordSheet.hasLyrics()) {
			return;
		}
		
		String songKey = newChordSheet.getTitle().toLowerCase().replaceAll("&", "and").replaceAll("[^a-z0-9 ]", "");
		
		if (lyricKeys != null && !lyricKeys.get(artistKey).containsKey(songKey)) {
			return;
		}
		
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

	public static CSVParser loadCSVRecordParserForSite(String site) throws FileNotFoundException, IOException {
		System.out.println("Loading CSVs for " + site + "... ");
		
		FileReader csvFileReader = new FileReader(raw_cvsv_dir + "/" + site + ".csv");
		CSVParser parser = new CSVParser(csvFileReader, CSVFormat.RFC4180);
		return parser;
	}

	public static void loadLyricSheet(CSVRecord csvRecord,int provider, Map<String, Map<String, String>> songs) {
		String artistName = Utils.removeParen(csvRecord.get(LyricSheet.ARTIST_COL)).trim();
		int idxFeat = artistName.indexOf(" Feat."); // Remove "featuring so and so" denoted by "Feat."
		if(idxFeat != -1)
		{
			artistName = artistName.substring(0, idxFeat);
		}
		
		String artistKey = artistName.toLowerCase().replaceAll("&", "and").replaceAll("[^a-z0-9 ]", "").replaceFirst("^the ", "");
		if (filter(artistKey) || songs != null && !songs.containsKey(artistKey)) {
			return;
		}
		lyricsWriter.println(artistKey);

		LyricSheet newLyricSheet = new LyricSheet(csvRecord, artistName,provider);
		if (!newLyricSheet.hasLyrics())
			return;
		
		String songKey = newLyricSheet.getTitle().toLowerCase().replaceAll("&", "and").replaceAll("[^a-z0-9 ]", "");

		if (songs != null && !songs.get(artistKey).containsKey(songKey)){
			return;
		}
		lyricsWriter.print("\t" + songKey);
//		if (songs == null)
//			return;
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

	private static boolean filter(String artistKey) {
		if (filters.size() == 0)
			return false;
		else
			return (!filters.contains(artistKey));
	}

	public static void setLyricKeys(Map<String, Map<String, List<LyricSheet>>> lyricSheets) {
		lyricKeys = lyricSheets;
	}
}
