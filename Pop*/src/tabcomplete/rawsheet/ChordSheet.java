package tabcomplete.rawsheet;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.parser.Parser;

import harmony.Chord;
import harmony.Chord.ChordQuality;
import pitch.Pitch;
import tabcomplete.alignment.Aligner;
import tabcomplete.alignment.StringPair;
import tabcomplete.alignment.StringPairAlignment;
import tabcomplete.alignment.XGenericPairwiseAlignment;
import tabcomplete.alignment.SequencePair.AlignmentBuilder;
import tabcomplete.filter.DirtyFilter;
import tabcomplete.main.TabDriver;
import tabcomplete.normalize.ChordNormalizer;
import tabcomplete.utils.Utils;
import utils.Pair;

public class ChordSheet implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final boolean DEBUG = false;

	static int barRepeatsTot = 0;
	static int xRepeatsTot = 0;
	static int recoveredChordsTot = 0;
	static int inferredChordsTot = 0;
	static int embeddedTabsTot = 0;
	static int singleBlockTabsTot = 0;
	static int blocksWithRoleIndicatedTot = 0;
	static int tabsWithChorusMarkedTot = 0;
	private static int chordSheetsIgnoredBecauseOfLanguage = 0;
	public static int malformattedTabs = 0;

//	public static final int RATING_COL = TabDriver.mini_data_set? 1 ;
	public static final int ARTIST_COL = TabDriver.mini_data_set? 2 : 1;
	public static final int URL_COL = TabDriver.mini_data_set? 3 : 2;
	public static final int TITLE_COL = TabDriver.mini_data_set? 4 : 3;
	public static final int DIFFICULTY_COL = TabDriver.mini_data_set? 7 : 4;
	public static final int RAWTAB_COL = TabDriver.mini_data_set? 8 : 0;
	public static final int KEY_COL = TabDriver.mini_data_set? 9 : 5;
	public static final int PROVIDER_COL = TabDriver.mini_data_set? 10 : 6;
	public static final int CONTRIBUTOR_COL = TabDriver.mini_data_set? 11 : 7;
	public static final int TYPE_COL = TabDriver.mini_data_set? 13 : 8;
	private static final String[] EC_TAGS = new String[] { "script", "img", "p", "i", "b", "span", "div:not(.chorus)" };
	private static final String[] UG_TAGS = new String[] { "script", "img", "p", "div", "i", "b", "u" };
	private String title;
	private String artist;
	String url;
	private String contributor;
	private String type;
	private String difficulty;
	private int key;
	private List<List<String>> lyricBlocks = new ArrayList<List<String>>();
	private List<List<SortedMap<Integer, Chord>>> chordBlocks = new ArrayList<List<SortedMap<Integer, Chord>>>();
	private List<Integer> lostCharacters = new ArrayList<Integer>();
	private List<String> blockRoles = new ArrayList<String>();
	private int barRepeats = 0;
	private int xRepeats = 0;
	private List<String> recoveredChords = new ArrayList<String>();
	private List<String> inferredChords = new ArrayList<String>();
	private boolean embeddedTab = false;
	private boolean singleBlockTab = false;
	private int blocksWithRoleIndicated = 0;
	private boolean chorusMarked = false;

	// Pattern looks for all chars ≠ "<" up to <u>...</u> and then all chars ≠ "<"
	private static final Pattern markedChordPattern = Pattern.compile("([^<]*)?(<u>([^>]*?)</u>)?([^<]*)");



	public ChordSheet(CSVRecord csvRecord, String artistName, boolean ug) {
		this.artist = artistName;
		this.url = csvRecord.get(URL_COL);
		if (!url.equals("https://tabs.ultimate-guitar.com/e/eagles/its_your_world_now_ukulele_crd.htm"))
			return;
		
		this.title = csvRecord.get(TITLE_COL);
		this.type = csvRecord.get(TYPE_COL).trim();

		String rawTab = csvRecord.get(RAWTAB_COL).trim();
		rawTab = rawTab.replaceAll("\\r?\\n", "<br>");

		Document doc = Jsoup.parse(rawTab);
		doc.outputSettings().prettyPrint(false);

		removeComments(doc);
		doc.select(".hide_tab").remove();
		rawTab = "";

		if (ug) { // ULTIMATE GUITAR
			this.title = csvRecord.get(TITLE_COL);
			this.type = " " + csvRecord.get(TYPE_COL);
			int idxOfTypeInTitle = this.title.indexOf(this.type);
			if (idxOfTypeInTitle == -1) {
				this.title = this.title.substring(0,
						this.title.indexOf(this.type.substring(0, this.type.length() - 1)));
			} else {
				this.title = this.title.substring(0, idxOfTypeInTitle);
			}

			for (String tag : UG_TAGS) {
				doc.select(tag).unwrap();
			}

			for (Element el : doc.select("body")) {
				rawTab += el.html();
			}

			rawTab = rawTab.replaceAll("<span>", "<u>").replaceAll("</span>", "</u>");
		} else { // ECHORDS

			for (String tag : EC_TAGS) {
				doc.select(tag).unwrap();
			}

			boolean chorusMarked = false;
			for (Element el : doc.select("div.chorus")) {
				chorusMarked = true;
				el.prepend("echordschorus<br>");
				el.html(el.html().replaceAll("<br>\\s*<br>", "<br>"));
				el.unwrap();
			}
			if (chorusMarked) {
				this.chorusMarked = true;
			}

			for (Element el : doc.select("body")) {
				rawTab += el.html();
			}
		}

		if(!parseTab(rawTab))
			return;

		this.title = Utils.removeParen(this.title).trim();

		this.difficulty = csvRecord.get(DIFFICULTY_COL).trim();
		this.key = Pitch.getPitchValue(csvRecord.get(KEY_COL));
		this.contributor = csvRecord.get(CONTRIBUTOR_COL).trim();

		System.out.println(this.toStringWithHeader());
		
//		this.key = normalizeChords(this.key);
	}

	private int normalizeChords(int key) {
		return ChordNormalizer.normalize(chordBlocks,key);
	}

	private static void removeComments(Node node) {
		for (int i = 0; i < node.childNodes().size();) {
			Node child = node.childNode(i);
			if (child.nodeName().equals("#comment"))
				child.remove();
			else {
				removeComments(child);
				i++;
			}
		}
	}

	/**
	 * Parse tab and lyrics from rawTab string into appropriate static data structures 
	 * @param rawTab
	 * @return false if tab is determined to be invalid or to contain bad language
	 */
	private boolean parseTab(String rawTab) {
		if (rawTab.length() == 0)
			return false;
		// Find two newlines before first chord (second to keep block together)
		int start = findStartIdx(rawTab);
		if (start == -1)
			return false;

		// Find second new line after last chord (second to keep block together)
		int end = findLastIdx(rawTab);
		if (end == -1)
			return false;

		// Grab the stuff in between
		rawTab = rawTab.substring(start, end);

		// Split into blocks and prepare the data structure
		rawTab = rawTab.replaceAll("[\\(\\[]<", "<"); // some embedded UG tabs put square brackets around chords
		rawTab = rawTab.replaceAll(">[\\)\\]]", ">");
		rawTab = Parser.unescapeEntities(rawTab, true).replaceAll("&", "and").replaceAll("(?i)([\\S])(\\1)+", "$1$2").trim();
		if (DirtyFilter.isProfane(rawTab)){
			chordSheetsIgnoredBecauseOfLanguage++;
			if (DEBUG) System.out.println("Ignoring " + this.url + " for explicit language");
			return false;
		}
		String[] blocks = rawTab.split("<br>\\s*<br>");

		System.out.println(rawTab.replaceAll("<br>", "\n"));
		// first try parsing the tab as though not embedded
		if (!parseTabBlocks(blocks, false)) { // if it proves to be embedded
			parseTabBlocks(blocks, true); // parse it as embedded
		}

		barRepeatsTot += barRepeats;
		xRepeatsTot += xRepeats;
		recoveredChordsTot += recoveredChords.size();
		inferredChordsTot += inferredChords.size();
		if (embeddedTab)
			embeddedTabsTot++;
		if (singleBlockTab)
			singleBlockTabsTot++;
		blocksWithRoleIndicatedTot += blocksWithRoleIndicated;
		if (chorusMarked)
			tabsWithChorusMarkedTot++;
		
		return true;
	}

	/**
	 * For each of the blocks in blocks, separate the chords and the lyrics into static data structures
	 * @param blocks array of strings each of which represents a block of lyrics and chords in the tab
	 * @param embedded if true, the chords are assumed to be embedded in the same line as the lyrics
	 * @return false if embedded == false, but algorithm detects it should be set to true
	 */
	private boolean parseTabBlocks(String[] blocks, boolean embedded) {
		List<Pair<Integer, Integer>> embeddedLines = new ArrayList<Pair<Integer, Integer>>();

		for (int i = 0; i < blocks.length; i++) {
			String block = blocks[i];
			String[] lines = block.split("<br>");

			parseTabBlock(lines, embeddedLines, embedded);
		}

		// If less than half of the tab's lines were embedded
		// Loop through and delete the "embedded" lines
		// which are almost certainly non-lyric information
		int totalLyricLines = 0;
		for (List<String> block : lyricBlocks) {
			totalLyricLines += block.size();
		}

		// If embedded lines constitute less than half of the total lines
		if (embeddedLines.size() <= totalLyricLines * .5) {
			// record the lost characters
			for (int i = embeddedLines.size() - 1; i >= 0; i--) {
				Pair<Integer, Integer> blockId_lineId = embeddedLines.get(i);
				int blockId = blockId_lineId.getFirst();
				int lineId = blockId_lineId.getSecond();
				int additionalLostChars = lyricBlocks.get(blockId).get(lineId).length();
				lostCharacters.set(blockId, lostCharacters.get(blockId) + additionalLostChars);

				// TODO: What the heck?! Make it so that lines without lyrics keep their embedded content
				if (chordBlocks.get(blockId).size() > (lineId + 1)
						&& chordBlocks.get(blockId).get(lineId + 1) == null) {
					lyricBlocks.get(blockId).remove(lineId);
					chordBlocks.get(blockId).remove(lineId + 1);
					totalLyricLines--;
				} else {
					lyricBlocks.get(blockId).set(lineId, null);
				}
			}
		} else {
			if (!embedded) {
				if (DEBUG)
					System.out.println("EMBEDDING DETECTED: REPARSING AS EMBEDDED");
				resetBlocks();
				return false; // FAILED:Should have been parsed as embedded, but wasn't
			} else {
				embeddedTab = true;
			}
		}

		int totalChordLines = 0;
		for (List<SortedMap<Integer, Chord>> block : chordBlocks) {
			totalChordLines += block.size();
		}

		if (chordBlocks.size() == 1) {
			singleBlockTab = true;
		}

		if (totalChordLines != totalLyricLines) {
			throw new RuntimeException("Chord and lyric array lengths do not match on: " + title + " by " + artist);
		}

		return true;
	}

	private void resetBlocks() {
		lyricBlocks = new ArrayList<List<String>>();
		chordBlocks = new ArrayList<List<SortedMap<Integer, Chord>>>();
		lostCharacters = new ArrayList<Integer>();
		blockRoles = new ArrayList<String>();
		barRepeats = 0;
		xRepeats = 0;
		recoveredChords = new ArrayList<String>();
		inferredChords = new ArrayList<String>();
		blocksWithRoleIndicated = 0;
	}

	/**
	 * Parse lines to separate lyrics and chords and put them in the appropriate static data structures
	 * @param lines array of strings representing the current unparsed tab block
	 * @param embeddedLines data structure in which to keep lines of text that appear on the same lines as chords
	 * @param embedded if true, we assume lyrics are embedded in same lines as chords
	 */
	private void parseTabBlock(String[] lines, List<Pair<Integer, Integer>> embeddedLines, boolean embedded) {
		List<String> lyricBlock = new ArrayList<String>();
		List<SortedMap<Integer, Chord>> chordBlock = new ArrayList<SortedMap<Integer, Chord>>();

		String roleTest = null;
		String role = null;
		for (int i = 0; i < lines.length; i++) {
			String line = lines[i];
			StringBuilder lyric = new StringBuilder();

			if (DEBUG)
				System.out.println("FOR LINE:" + line);
			SortedMap<Integer, Chord> chordLine = parseLineForChords(line, lyric, embedded);
			String lyricStr = lyric.toString();
			if (DEBUG) {
				System.out.println("PARSED CHORDS:" + chordLine);
				System.out.println("NEW LINE:" + lyricStr);
			}

			roleTest = lyricStr.toLowerCase().replaceAll("([^A-Za-z])", "").trim();
			roleTest = ROLE_MAP.get(roleTest);
			if (roleTest != null) {
				blocksWithRoleIndicated++;
				// Terminate the block we've been building early
				// If a block ends with chords but no lyrics or lyrics but no chords
				terminateBlock(lyricBlock, chordBlock, role);

				lyricBlock = new ArrayList<String>();
				chordBlock = new ArrayList<SortedMap<Integer, Chord>>();

				// Set the metadata of the new block and
				// clear the lyric
				role = roleTest;
//				lyric = new StringBuilder(); I think we can delete this line
			} else {
				if (chordLine.size() > 1 && lyricStr.replaceAll("([^A-Za-z])", "").trim().length() > 0) {
					if (DEBUG) {
						System.out.println("EXTRANEOUS CHORDS?:" + lyricStr);
						System.out.println("URL: " + url);
					}
				}

				// If a lyric is not the first line of a block and has some alphabetic characters
				// we are able to make this former assumption because
				// chords always follow lyrics, this may unduly delete
				// some lines of lyrics, but never lyric/chord pairs
				if (lyricStr.replaceAll("[^\\w]|[0-9]", "").trim().length() > 0) {
					lyricBlock.add(lyricStr.replaceAll("\\s+$", "")); // rtrim
					// If a chord and a lyric were on the same line
					// mark it as embedded, we'll delete them later unless most
					// of the song is embedded
					if (chordLine.size() > 0)
						// The embedded line indexes
						// after being added to the data structure
						embeddedLines.add(new Pair<Integer, Integer>(lyricBlocks.size(), lyricBlock.size() - 1));
				}
			}

			// If a chord was detected, add it
			if (chordLine.size() > 0)
				chordBlock.add(chordLine);

			// if we have added a line of lyrics without a chord, we
			// must have a missing chord. This is because we assume
			// a line of chords ALWAYS comes before the paired line of lyrics
			if (chordBlock.size() < lyricBlock.size()) {
				chordBlock.add(null);
			}
			// If there was a line of chords in the middle of a block
			// with no following lyrics
			else if (chordBlock.size() - 1 > lyricBlock.size()) {
				lyricBlock.add(null);
			}
		}

		terminateBlock(lyricBlock, chordBlock, role);
	}

	/**
	 * Function which parses the line for chords (demarcated by u tag and also those missing)
	 * 
	 * @param line
	 *            raw input of line
	 * @param lyric
	 *            StringBuilder to which to append the growing line
	 * @param embedded
	 * @return A map mapping line position to chord symbol
	 */
	public SortedMap<Integer, Chord> parseLineForChords(String line, StringBuilder lyric, boolean embedded) {
		line = resolveBarredRepeats(line);
		SortedMap<Integer, Chord> chordLine = new TreeMap<Integer, Chord>();
		int currentChordLineIdx = 0;
		Matcher matcher = markedChordPattern.matcher(line);
		while (matcher.find()) {
			String before = matcher.group(1);
			String chordGroup = matcher.group(3);
			Chord chord = Chord.parse(chordGroup, true);
			if (DEBUG && chordGroup != null && chord == null) {
				System.out.println("UNPARSABLE MARKED CHORD:" + chordGroup);
				System.out.println(line);
			}
			String after = matcher.group(4);

			if (chord != null) { // the line has chords
				if (embedded) {
					lyric.append(rmParen(before, true));
					currentChordLineIdx += before.length();
				} else {
					currentChordLineIdx = parseForUnmarked(before, lyric, chordLine, currentChordLineIdx);
				}

				chordLine.put(currentChordLineIdx, chord);
				currentChordLineIdx += chord.toString().length();

				if (embedded) {
					lyric.append(rmParen(after, true));
					currentChordLineIdx += after.length();
				} else {
					currentChordLineIdx = parseForUnmarked(after, lyric, chordLine, currentChordLineIdx);
				}
			} else {
				lyric.append(rmParen(before, true));
				currentChordLineIdx += before.length();
				lyric.append(rmParen(after, true));
				currentChordLineIdx += after.length();
			}
		}

		resolveXRepeats(lyric, chordLine, currentChordLineIdx);
		// if (DEBUG) System.out.println(line);
		// if (DEBUG) System.out.println(chordLine.toString());
		// if (DEBUG) System.out.println(lyric);
		// Utils.promptEnterKey("Waiting...");
		return chordLine;
	}

	/**
	 * Simply repeats marked chords (nothing else)
	 * 
	 * @param lyric
	 *            from which to find the X repeat marker
	 * @param chordLine
	 *            the current chords found in the line (to be repeated)
	 * @param currentChordLineIdx
	 *            idx offset for repeating chords idxs
	 * @return
	 */
	private void resolveXRepeats(StringBuilder lyric, SortedMap<Integer, Chord> chordLine, int currentChordLineIdx) {
		if (chordLine.size() == 0)
			return;

		// String[] tokens = line.split("\\W(?=\\w+\\W*$)");
		String lyricStr = lyric.toString();
		String[] tokens = lyricStr.split("\\W+");
		for (int j = tokens.length - 1; j >= 0; j--) {
			String token = tokens[j];
			int rptCt = -1;
			if (token.matches("[xX]1?\\d")) {
				if (DEBUG)
					System.out.println("RESOLVING REPEATS IN \"" + lyricStr + "\"");
				rptCt = Integer.parseInt(token.substring(1));
				int lastIndexOf = lyricStr.lastIndexOf("x" + rptCt);
				if (lastIndexOf == -1)
					lastIndexOf = lyricStr.lastIndexOf("X" + rptCt);
				lyric.setLength(lastIndexOf);
			} else if (token.matches("1?\\d[xX]")) {
				if (DEBUG)
					System.out.println("RESOLVING REPEATS IN \"" + lyricStr + "\"");
				rptCt = Integer.parseInt(token.substring(0, token.length() - 1));
				int lastIndexOf = lyricStr.lastIndexOf("" + rptCt + "x");
				if (lastIndexOf == -1)
					lastIndexOf = lyricStr.lastIndexOf("" + rptCt + "X");
				lyric.setLength(lastIndexOf);
			}

			if (rptCt > 1) {
				xRepeats++;
				List<Integer> origKeys = new ArrayList<Integer>(chordLine.keySet());
				for (int i = 1; i < rptCt; i++) {
					for (Integer origKey : origKeys) {
						chordLine.put(origKey + (i * currentChordLineIdx), chordLine.get(origKey));
					}
				}
				if (DEBUG)
					System.out.println("RESOLUTION: \"" + chordLine + "\"");
				// Utils.promptEnterKey("CHECK OUT X REPEATS...");
				return;
			}
		}
		return;
	}

	private String resolveBarredRepeats(String line) {
		int firstOpenRepeatPos = line.indexOf("||:");
		int lastOpenRepeatPos = line.lastIndexOf(":||");

		if (firstOpenRepeatPos != -1 && lastOpenRepeatPos != -1) {
			barRepeats++;
			// if (DEBUG) System.out.println("RESOLVING REPEATS IN \"" + line + "\"");
			String resolvedRepeats = resolveBarredRepeats(line.substring(firstOpenRepeatPos + 3, lastOpenRepeatPos));
			line = line.substring(0, firstOpenRepeatPos) + resolvedRepeats + " " + resolvedRepeats
					+ line.substring(lastOpenRepeatPos + 3);
			// if (DEBUG) System.out.println("RESOLUTION: \"" + line + "\"");
			// Utils.promptEnterKey("CHECK OUT BARRED REPEATS...");
		}

		return line;
	}

	// static public final String WITH_DELIMITER = "((?<=%1$s)|(?=%1$s))";
	static private final Pattern TOKEN_DELIMITER = Pattern.compile("[:\\|\\.\\s~]+|-(?=([^\\d]))");
	static private final Pattern MULTIPLIER_FINDER = Pattern.compile("([\\s\\({\\[]x)(\\s+)(1?\\d)([$\\s\\)}\\]])");
	static private final Pattern PARENTHESIS_REMOVER = Pattern
			.compile("(?<=([^A-G]|^))([\\({\\[][^\\)}\\]([Xx]1?\\d|1?\\d[Xx])]*[\\]\\)}])");
	static private final Pattern REPEAT_MODIFIER = Pattern.compile("(?i)(repeat)[\\W]*([^(x1?\\d|1?\\dx)]|$)");

	/**
	 * Note that this function is only called when a chord has already been detected
	 * 
	 * @param line
	 * @param lyric
	 * @param chordLine
	 * @param lastOpenRepeatPos
	 * @return
	 */
	private int parseForUnmarked(String line, StringBuilder lyric, SortedMap<Integer, Chord> chordLine,
			int currentChordLineIdx) {
		if (line.length() == 0)
			return currentChordLineIdx;

		Matcher matcher = MULTIPLIER_FINDER.matcher(line);
		if (matcher.find()) {
			if (DEBUG)
				System.out.println("REPLACED: \"" + line + "\"");
			line = matcher.replaceAll(matcher.group(1) + matcher.group(3) + matcher.group(2) + matcher.group(4));
			if (DEBUG)
				System.out.println("WITH    : \"" + line + "\"");
		}

		matcher = REPEAT_MODIFIER.matcher(line);
		if (matcher.find()) {
			if (DEBUG)
				System.out.println("REPLACING \"" + line + "\"");
			int start = matcher.start(1);
			int end = matcher.end(1);
			line = line.substring(0, start) + line.substring(start, end - 2).replaceAll("\\S", " ") + "x2"
					+ line.substring(end);
			if (DEBUG)
				System.out.println("WITH      \"" + line + "\"");
		}
		line = line.replaceAll("(?i)repeat", "      ");

		// We won't allow chords to start with a parenthesis
		line = rmParen(line, false);

		List<String> tokens = splitWithDelimiter(line, TOKEN_DELIMITER);

		String token;
		for (int i = 0; i < tokens.size(); i += 2) {
			token = tokens.get(i);
			if (DEBUG)
				System.out.println("Token \"" + token + "\" in line \"" + line + "\"");
			// Search for legitimate chords
			boolean chordRecovered = false;
			if (token.length() > 0) {
				Chord parsedChord = Chord.parse(token, false);
				if (parsedChord != null) {
					chordLine.put(currentChordLineIdx, parsedChord);
					currentChordLineIdx += parsedChord.toString().length();
					recoveredChords.add(parsedChord.toString());
					if (DEBUG)
						System.out.println("RECOVERED: \"" + parsedChord.toString() + "\"");
					chordRecovered = true;
				} else {
					// Search for inferrable chords
					ChordQuality parsedChordQuality = ChordQuality.parseChordQuality(token, false);
					if (parsedChordQuality != null && chordLine.size() > 0) {
						String prevChordStr = chordLine.get(chordLine.lastKey()).toString();
						parsedChord = Chord.parse(prevChordStr + parsedChordQuality, false);
						if (parsedChord != null) {
							chordRecovered = true;
							inferredChords.add(parsedChord.toString());
							if (DEBUG)
								System.out.println("INFERRED: \"" + parsedChord.toString() + "\" from \"" + prevChordStr
										+ "\" and \"" + parsedChordQuality + "\"");
							chordLine.put(currentChordLineIdx, parsedChord);
							currentChordLineIdx += parsedChord.toString().length();
						}
					}
				}
			}

			if (!chordRecovered) {
				// if (token.length() > 0)
				// if (DEBUG) System.out.println("FAILED TO PARSE: " + token);
				lyric.append(token);
				currentChordLineIdx += token.length();
			}
			if (i + 1 < tokens.size()) {
				token = tokens.get(i + 1);
				lyric.append(token); // add delimiter
				currentChordLineIdx += token.length();
			}
		}
		// Repeat the search by combining consecutive tokens

		return currentChordLineIdx;
	}

	/**
	 * @param line
	 * @return
	 */
	public String rmParen(String line, boolean force) {
		Matcher matcher;
		matcher = PARENTHESIS_REMOVER.matcher(line);
		while (matcher.find()
				&& !ROLE_MAP.containsKey(matcher.group(2).toLowerCase().replaceAll("([^A-Za-z])", "").trim())) {
			if (DEBUG)
				System.out.println("REPLACED PARENS \"" + line + "\"");
			int start = matcher.start(2);
			int end = matcher.end(2);
			if (!force && Chord.parse(matcher.group(2).replaceAll("[\\({\\[\\]}\\)]", "").trim(), false) != null) {
				if (DEBUG)
					System.out.println("COULD BE CHORD, CHANGE TO "
							+ line.substring(start, end).replaceAll("[\\({\\[\\]}\\)]", " "));
				line = line.substring(0, start) + line.substring(start, end).replaceAll("[\\({\\[\\]}\\)]", " ")
						+ line.substring(end);
			} else {
				line = line.substring(0, start) + line.substring(start, end).replaceAll("\\S", " ")
						+ line.substring(end);
			}
			if (DEBUG)
				System.out.println("         RESULT \"" + line + "\"");
		}
		return line;
	}

	/**
	 * @param line
	 * @param tokenDelimiter
	 * @return
	 */
	public List<String> splitWithDelimiter(String line, Pattern tokenDelimiter) {
		Matcher matcher = tokenDelimiter.matcher(line);

		int lastEnd = 0;
		List<String> tokens = new ArrayList<String>();

		while (matcher.find()) {
			int start = matcher.start();
			String before = line.substring(lastEnd, start);
			tokens.add(before);

			lastEnd = matcher.end();
			String after = line.substring(start, lastEnd);
			tokens.add(after);
		}
		tokens.add(line.substring(lastEnd));

		// if (DEBUG) System.out.println("LINE:\"" + line + "\"");
		// System.out.print("TOKENS:");
		// for(String token: tokens){
		// System.out.print("\""+token + "\" ");
		// }
		// if (DEBUG) System.out.println();

		return tokens;
	}

	public void terminateBlock(List<String> lyricBlock, List<SortedMap<Integer, Chord>> chordBlock, String role) {
		if (chordBlock.size() > lyricBlock.size())
			lyricBlock.add(null);
		else if (lyricBlock.size() > chordBlock.size())
			chordBlock.add(null);

		if (DEBUG)
			System.out.println("TERMINATE BLOCK:" + chordBlock.size() + "," + lyricBlock.size());

		if (chordBlock.size() + lyricBlock.size() > 0) {
			addBlock(lyricBlock, chordBlock, role);
		}
	}

	public void addBlock(List<String> lyricBlock, List<SortedMap<Integer, Chord>> chordBlock, String role) {
		lyricBlocks.add(lyricBlock);
		chordBlocks.add(chordBlock);
		blockRoles.add(role);
		lostCharacters.add(0);
	}

	/**
	 * Finds and returns the position in rawTab representing the end of the line after the line containing 
	 * the last chord (as marked by u tag) so as to capture final lyric line
	 * @param rawTab
	 * @return -1 if no chords found, rawTab.length if no br tag after last chord, or index of br tag after br tag after last chord in rawTab (as marked by u tag)
	 */
	private int findLastIdx(String rawTab) {
		int lastChordIdx = rawTab.lastIndexOf("</u>");
		if (lastChordIdx == -1)
			return -1;

		int newLineAfterLastChordIdx = rawTab.indexOf("<br>", lastChordIdx + 4);
		if (newLineAfterLastChordIdx == -1)
			return rawTab.length();

		int newLineAfterNewLineAfterLastChordIdx = rawTab.indexOf("<br>", newLineAfterLastChordIdx + 4);
		if (newLineAfterNewLineAfterLastChordIdx == -1)
			return rawTab.length();
		else
			return newLineAfterNewLineAfterLastChordIdx;
	}

	/**
	 * Finds and returns the position in rawTab representing the beginning of the line
	 * before the line containing the first chord (as marked by u tag) so as to capture block role markers
	 * @param rawTab
	 * @return -1 if there are no chords, 0 if there is no line before the first containing a chord, position of beginning of line (i.e., beginning of br tag) before line containing first instance of u tag
	 */
	public int findStartIdx(String rawTab) {
		int firstChordIdx = rawTab.indexOf("<u>");

		// no chords, error
		if (firstChordIdx == -1)
			return -1;
		int lastBrBeforeChordsIdx = rawTab.lastIndexOf("<br>", firstChordIdx);

		// no line break before chords, that's okay, start from pos 0
		if (lastBrBeforeChordsIdx == -1)
			return 0;
		int secondToLastBrBeforeChordsIdx = rawTab.lastIndexOf("<br>", lastBrBeforeChordsIdx);

		// no second to last line break before chords? that's okay, start from last line break before chords
		if (secondToLastBrBeforeChordsIdx == -1)
			return lastBrBeforeChordsIdx;
		else
			return secondToLastBrBeforeChordsIdx;
	}

	public String toStringWithHeader() {
		StringBuilder builder = new StringBuilder();

		builder.append("RawChordSheet \n\t/**\n\t*title=").append(title).append("\n\t*artist=").append(artist)
		.append("\n\t*url=").append(url).append("\n\t*contributor=").append(contributor).append("\n\t*type=")
		.append(type).append("\n\t*difficulty=").append(difficulty)
		.append("\n\t*key=").append(key).append("\n\t*bar repeats=").append(barRepeats)
		.append("\n\t*x repeats=").append(xRepeats).append("\n\t*recoverred chords=").append(recoveredChords)
		.append("\n\t*inferred chords=").append(inferredChords).append("\n\t*embedded=").append(embeddedTab)
		.append("\n\t*single block=").append(singleBlockTab).append("\n\t*blocks with role=")
		.append(blocksWithRoleIndicated).append("\n\t*chorus marked=").append(chorusMarked)
		.append("\n\t*rawTab=\n\t*/\n");
		
		String role;
		for (int i = 0; i < lyricBlocks.size(); i++) {
			List<SortedMap<Integer, Chord>> chordBlock = chordBlocks.get(i);
			List<String> lyricBlock = lyricBlocks.get(i);
			if (i != 0)
				builder.append("\n");
			role = blockRoles.get(i);
			builder.append("\n\tBLOCK " + (i) + ": " + (role == null ? "NO ROLE" : role.toUpperCase()) + " ("
					+ lostCharacters.get(i) + " lost chars)");
			for (int j = 0; j < lyricBlock.size(); j++) {
				builder.append("\n\t" + render(chordBlock.get(j)));
				String lyric = lyricBlock.get(j);
				builder.append("\n\t" + (lyric == null ? "" : lyric));
			}
		}
		builder.append("\n]");
		
		return builder.toString();
	}
	
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		
		for (int i = 0; i < lyricBlocks.size(); i++) {
			List<SortedMap<Integer, Chord>> chordBlock = chordBlocks.get(i);
			List<String> lyricBlock = lyricBlocks.get(i);
			if (i != 0)
				builder.append("\n");
			for (int j = 0; j < lyricBlock.size(); j++) {
				builder.append("\n\t" + render(chordBlock.get(j)));
				String lyric = lyricBlock.get(j);
				builder.append("\n\t" + (lyric == null ? "" : lyric));
			}
		}
		return builder.toString();
	}

	private String render(SortedMap<Integer, Chord> sortedMap) {
		if (sortedMap == null || sortedMap.size() == 0)
			return "N.C.";

		StringBuilder str = new StringBuilder();
		int i = 0;
		for (Integer mapKey : sortedMap.keySet()) {
			for (; i < mapKey; i++) {
				str.append(" ");
			}
			String chordStr = sortedMap.get(mapKey).toString();
			str.append(chordStr);
			i += chordStr.length();
		}

		return str.toString();
	}

	public String getTitle() {
		return title;
	}

	public static String parseSummary() {
		StringBuilder str = new StringBuilder();

		str.append("barRepeats = " + barRepeatsTot + "\n");
		str.append("xRepeats = " + xRepeatsTot + "\n");
		str.append("recoveredChords = " + recoveredChordsTot + "\n");
		str.append("inferredChords = " + inferredChordsTot + "\n");
		str.append("embeddedTabs = " + embeddedTabsTot + "\n");
		str.append("singleBlockTabs = " + singleBlockTabsTot + "\n");
		str.append("blocksWithRoleIndicated = " + blocksWithRoleIndicatedTot + "\n");
		str.append("tabsWithChorusMarked = " + tabsWithChorusMarkedTot + "\n");
		str.append("malformattedTabs = " + malformattedTabs + "\n");
		str.append("chordSheetsIgnoredBecauseOfLanguage = " + chordSheetsIgnoredBecauseOfLanguage + "\n");

		return str.toString();
	}

	private static final Map<String, String> ROLE_MAP;

	private static final double VALID_LYRIC_LINE_THRESH = .75;
	private static final double VALID_LYRIC_SHEET_THRESH = .8;

	static {
		ROLE_MAP = new HashMap<String, String>();

		ROLE_MAP.put("echordschorus", "chorus");
		ROLE_MAP.put("chorus", "chorus");
		ROLE_MAP.put("bridge", "bridge");
		ROLE_MAP.put("verse", "verse");
		ROLE_MAP.put("prechorus", "prechorus");
		ROLE_MAP.put("intro", "intro");
		ROLE_MAP.put("outro", "outro");
		ROLE_MAP.put("versei", "verse");
		ROLE_MAP.put("verseii", "verse");
		ROLE_MAP.put("verseiii", "verse");
		ROLE_MAP.put("solo", "solo");
		ROLE_MAP.put("final", "final");
		ROLE_MAP.put("refrain", "chorus");
		ROLE_MAP.put("tuning", "tuning");
		ROLE_MAP.put("interlude", "interlude");
		ROLE_MAP.put("introd", "intro");
		ROLE_MAP.put("versea", "verse");
		ROLE_MAP.put("verseb", "verse");
		ROLE_MAP.put("verso", "verse");
		ROLE_MAP.put("vers", "verse");
		ROLE_MAP.put("ref", "ref");
		ROLE_MAP.put("introx", "intro");
		ROLE_MAP.put("refx", "refx");
		ROLE_MAP.put("instl", "interlude");
		ROLE_MAP.put("riff", "riff");
		ROLE_MAP.put("key", "key");
		ROLE_MAP.put("instrumental", "interlude");
		ROLE_MAP.put("ending", "outro");
		ROLE_MAP.put("instrumentalbreak", "interlude");
		ROLE_MAP.put("instrumentalbridge", "interlude");
		ROLE_MAP.put("chords", "chords");
		ROLE_MAP.put("chorusx", "chorus");
		ROLE_MAP.put("lead", "lead");
		ROLE_MAP.put("interlude", "interlude");
		ROLE_MAP.put("notes", "notes");
		ROLE_MAP.put("note", "note");
		ROLE_MAP.put("saxsolo", "solo");
		ROLE_MAP.put("ragtimebit", "interlude");
		ROLE_MAP.put("pianosolo", "solo");
		ROLE_MAP.put("saxoutro", "outro");
		ROLE_MAP.put("coda", "outro");
		ROLE_MAP.put("solosection", "solo");
		ROLE_MAP.put("inst", "interlude");
		ROLE_MAP.put("introriff", "intro");
	}

	public String getLyrics() {
		StringBuilder builder = new StringBuilder();
		boolean first = true;
		for (List<String> lyricBlock : lyricBlocks) {
			for (String lyric : lyricBlock) {
				if (first)
					first = false;
				else 
					builder.append("\n");
				builder.append((lyric == null ? "" : lyric));
			}
		}
		return builder.toString();
	}

	public boolean hasLyrics() {
		return lyricBlocks.size() > 0 && getLyrics().trim().length() > LyricSheet.MINIMUM_SONG_LENGTH;
	}

	public Pair<List<SortedMap<Integer, Chord>>, List<String>> validate(String consensus) {
		String tabLyrics = getLyrics();
		
		if (DEBUG) System.out.println("Aligning:");
		if (DEBUG) System.out.println(consensus.replaceAll("\n", "\\\\n"));
		if (DEBUG) System.out.println(tabLyrics.replaceAll("\n", "\\\\n"));
		StringPairAlignment aln = (StringPairAlignment) Aligner.alignNW(new StringPair(consensus, tabLyrics));
		
		String alnConsensus = aln.getFirst();
		String alnTabLyrics = aln.getSecond();

		if (DEBUG) System.out.println("\nAlignment:");
		if (DEBUG) System.out.println(alnConsensus.replaceAll("\n", "#"));
		if (DEBUG) System.out.println(alnTabLyrics.replaceAll("\n", "#"));
		

		int firstValidChordLine = -1;
		int lastValidLyricLine = -1;
		int alnPos = 0;
		int lyricCharMatchCount = 0;
		
		List<SortedMap<Integer, Chord>> tabChords = new ArrayList<SortedMap<Integer, Chord>>();
		for(List<SortedMap<Integer, Chord>> block : chordBlocks)
			tabChords.addAll(block);
		
		List<SortedMap<Integer, Chord>> finalTabChords = new ArrayList<SortedMap<Integer, Chord>>();
		List<String> finalTabLyrics = new ArrayList<String>();
		
		String[] split = alnTabLyrics.split("\n");
		String alnTabLine, alnLyrLine;
		int lineLen, tabLineLen, charMatchCount;
		char charA;
		boolean isInvalidLyricLine, isInvalidChordLine;
		SortedMap<Integer, Chord> chordLine;
		for (int i = 0; i < split.length; i++) {
			alnTabLine = split[i];
			lineLen = alnTabLine.length();
			
			alnLyrLine = alnConsensus.substring(alnPos, alnPos + lineLen);
			tabLineLen = 0;
			charMatchCount = 0;
			for (int j = 0; j < lineLen; j++) {
				charA = alnTabLine.charAt(j);
				if (charA != AlignmentBuilder.INDEL_CHAR){
					tabLineLen++;
					if (charA == alnLyrLine.charAt(j)) {
						charMatchCount++;
					}
				}
			}
			
			lyricCharMatchCount += charMatchCount;
			
			isInvalidLyricLine = (tabLineLen == 0? true : ((charMatchCount / (double) tabLineLen) < VALID_LYRIC_LINE_THRESH));
			chordLine = tabChords.get(i);
			isInvalidChordLine = (chordLine == null || chordLine.size() == 0);
			
			if (isInvalidChordLine) { // if there aren't any chords
				if (lastValidLyricLine == -1) { // if we haven't found any valid lyric lines yet
					firstValidChordLine = -1; // then we haven't found any valid lyric lines yet
				} else {
					break;
				}
			} else { // if there ARE chords
				if (firstValidChordLine == -1) { // this may be the first valid chord line 
					firstValidChordLine = i;
					finalTabChords = new ArrayList<SortedMap<Integer, Chord>>();
					finalTabLyrics = new ArrayList<String>();
				}
				
				finalTabChords.add(chordLine); // TODO: adjust chords according to INDELS in aligned lines
				
				if (isInvalidLyricLine) {
					finalTabLyrics.add("");
				} else {
					lastValidLyricLine = i;
					// TODO: make first letter upper case
					finalTabLyrics.add(StringUtils.capitalize(alnLyrLine.replaceAll("\n", " ").replaceAll(""+XGenericPairwiseAlignment.INDEL_CHAR, "").trim()));
				}
			}
			
			alnPos += lineLen + 1;
		}
		
		if ( lyricCharMatchCount / (double) consensus.length() < VALID_LYRIC_SHEET_THRESH) {
			if (DEBUG) System.err.println("" + lyricCharMatchCount + " matching lyric chars, less than " + VALID_LYRIC_SHEET_THRESH + " * " +consensus.length() + ", the length of the lyric sheet");
			return null;
		}
		if (DEBUG) System.out.println("Of " + consensus.length() + " chars, " + lyricCharMatchCount + " were matched by the tab");
		
		return new Pair<List<SortedMap<Integer, Chord>>, List<String>>(finalTabChords, finalTabLyrics);
	}

	public int getKey() {
		return key;
	}

	public String getURL() {
		return url;
	}
}
