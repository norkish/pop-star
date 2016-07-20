package tabcomplete.rawsheet;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVRecord;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import tabcomplete.filter.DirtyFilter;
import tabcomplete.main.TabDriver;
import tabcomplete.utils.Utils;

public class LyricSheet implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final int LYRICS_COL = TabDriver.mini_data_set? 0 : 3;
	public static final int TITLE_COL = 1;
	public static final int URL_COL = TabDriver.mini_data_set? 2 : 0;
	public static final int ARTIST_COL = TabDriver.mini_data_set? 3 : 4;
	public static final int PROVIDER_COL = TabDriver.mini_data_set? 4 : 2;

	public static final int LYRICSNET = 0;
	public static final int METROLYRICS = 1;
	public static final int SONGLYRICS = 2;
	private static final String[] LYRICSNET_N_SONGLYRICS_TAGS = new String[] { "script", "img", "p" };
	private static final boolean DEBUG = false;

	private List<List<String>> lyricBlocks = new ArrayList<List<String>>();
	private String title;
	private String url;
	private String artist;
	
	private static int lyricSheetsIgnoredBecauseOfLanguage = 0;

	public LyricSheet(CSVRecord csvRecord, String artistName, int provider) {
		artist = artistName;
		title = csvRecord.get(TITLE_COL);
		title = Utils.removeParen(title).trim();
		url = csvRecord.get(URL_COL);

		String lyrics = csvRecord.get(LYRICS_COL).trim();
		
		
		
		if (provider == LYRICSNET) {
			lyrics = lyrics.replaceAll("\\r\\n", "<br>").replaceAll("\\n", "<br>");

			Document doc = Jsoup.parse(lyrics);
			for (String tag : LYRICSNET_N_SONGLYRICS_TAGS) {
				doc.select(tag).unwrap();
			}

			lyrics = "";

			for (Element el : doc.select("pre")) {
				lyrics += el.html();
			}

			String[] blocks = lyrics.split("<br>\\s*<br>");

			for (int i = 0; i < blocks.length; i++) {
				String block = blocks[i];
				List<String> lyricBlock = new ArrayList<String>();
				for (String lyr : block.split("<br>")) {
					String trim = lyr.replaceAll("(?i)([\\S])(\\1)+", "$1$2").trim();
					if (trim.length() > 0) {
						if (DirtyFilter.isProfane(trim)){
							lyricSheetsIgnoredBecauseOfLanguage++;
							if (DEBUG) System.out.println("Ignoring " + this.url + " for explicit language");
							lyricBlocks.clear();
							return;
						}
						lyricBlock.add(trim);
					}
				}
				if (lyricBlock.size() > 0)
					lyricBlocks.add(lyricBlock);
			}

		} else if (provider == SONGLYRICS && lyrics.length() > 0) {
			lyrics = lyrics.replaceAll("\\r?\\n", "");

			Document doc = Jsoup.parse(lyrics);
			for (String tag : LYRICSNET_N_SONGLYRICS_TAGS) {
				doc.select(tag).unwrap();
			}

			lyrics = "";

			for (Element el : doc.select("body")) {
				lyrics += el.html();
			}

			String[] blocks = lyrics.split("<br>\\s*<br>");

			for (int i = 0; i < blocks.length; i++) {
				String block = blocks[i];
				List<String> lyricBlock = new ArrayList<String>();
				for (String lyr : block.split("<br>")) {
					String trim = lyr.replaceAll("(?i)([\\S])(\\1)+", "$1$2").trim();
					if (trim.length() > 0) {
//						if (DirtyFilter.isProfane(trim)){
//							lyricSheetsIgnoredBecauseOfLanguage++;
//							if (DEBUG) System.out.println("Ignoring " + this.url + " for explicit language");
//							lyricBlocks.clear();
//							return;
//						}
						lyricBlock.add(trim);
					}
				}
				if (lyricBlock.size() > 0)
					lyricBlocks.add(lyricBlock);
			}
			// System.out.println("BLOCKS 2:" + lyricBlocks);
		} else { // METROLYRICS
			
			lyrics = lyrics.replaceAll("\\r", "").replaceAll("\\n", "");// .replaceAll("<br>", "\r\n");
			// System.out.println("LYRICS 2:" + lyrics);
			
			Document doc = Jsoup.parse(lyrics);
			// System.out.println("DOC:" + doc);

			for (Element el : doc.select("p")) {
				// System.out.println("CONSIDERING BLOCK: " + el.html());
				if (el.text().length() > 0) {
					List<String> lyricBlock = new ArrayList<String>();
					for (String lyr : el.html().split("<br>")) {
						String trim = lyr.replaceAll("(?i)([\\S])(\\1)+", "$1$2").trim();
						if (trim.length() > 0) {
							if (trim.startsWith("We are not in a position to display these lyrics due to licensing restrictions. Sorry for the inconvenience."))
								return;
							if (DirtyFilter.isProfane(trim)){
								lyricSheetsIgnoredBecauseOfLanguage++;
								if (DEBUG) System.out.println("Ignoring " + this.url + " for explicit language");
								lyricBlocks.clear();
								return;
							}
							lyricBlock.add(trim);
						}
					}
					if (lyricBlock.size() > 0)
						lyricBlocks.add(lyricBlock);
				}
			}
			// System.out.println("BLOCKS 2:" + lyricBlocks);
		}
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("RawLyricSheet [title=").append(title).append(", artist=").append(artist).append(", url=")
				.append(url).append(", lyrics=");
		boolean first = true;
		for (List<String> block : lyricBlocks) {
			if (first) {
				first = false;
			} else {
				builder.append("\n");
			}
			for (String lyric : block) {
				builder.append("\n\t" + lyric);
			}
		}
		builder.append("\n]");
		return builder.toString();
	}

	public String getTitle() {
		return title;
	}

	public static String parseSummary() {
		String summary = "lyricSheetsIgnoredBecauseOfLanguage = " + lyricSheetsIgnoredBecauseOfLanguage + "\n";
		return summary;
	}

	public String getLyrics() {
		StringBuilder builder = new StringBuilder();

		boolean first = true;
		for (List<String> block : lyricBlocks) {
			for (String lyric : block) {
				if (first)
					first = false;
				else
					builder.append("\n");
				builder.append(lyric);
			}
		}
		return builder.toString();
	}

	public String getURL() {
		return url;
	}

	public boolean hasNoLyrics() {
		return lyricBlocks.size() == 0;
	}
}
