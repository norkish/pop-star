package midieval;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

import syllabify.Syllabifier;
import tabcomplete.alignment.Aligner;
import tabcomplete.alignment.SequencePair;
import tabcomplete.alignment.SequencePair.AlignmentBuilder;
import tabcomplete.alignment.StringPair;
import tabcomplete.alignment.StringPairAlignment;
import tabcomplete.main.TabDriver;
import tabcomplete.rawsheet.LyricSheet;
import tabcomplete.rawsheet.RawDataLoader;
import tabcomplete.rhyme.Phonetecizer;
import tabcomplete.rhyme.StressedPhone;
import tabcomplete.utils.Serializer;
import utils.Pair;
import utils.Triple;

public class GenerateFilesForLabeling {
	
	public class NoteEvent {
		double measure_offset = -1.0;
		int offset_from_beginning = -1;
		double normalized_duration = -1.0;
		int transposed_note = -1;
		int note = -1;
		int measure = -1;
		int duration = -1;
		int velocity = -1;
		String type = null;
		public double getMeasure_offset() {
			return measure_offset;
		}
		public void setMeasure_offset(double measure_offset) {
			this.measure_offset = measure_offset;
		}
		public int getOffset_from_beginning() {
			return offset_from_beginning;
		}
		public void setOffset_from_beginning(int offset_from_beginning) {
			this.offset_from_beginning = offset_from_beginning;
		}
		public double getNormalized_duration() {
			return normalized_duration;
		}
		public void setNormalized_duration(double normalized_duration) {
			this.normalized_duration = normalized_duration;
		}
		public int getTransposed_note() {
			return transposed_note;
		}
		public void setTransposed_note(int transposed_note) {
			this.transposed_note = transposed_note;
		}
		public int getNote() {
			return note;
		}
		public void setNote(int note) {
			this.note = note;
		}
		public int getMeasure() {
			return measure;
		}
		public void setMeasure(int measure) {
			this.measure = measure;
		}
		public int getDuration() {
			return duration;
		}
		public void setDuration(int duration) {
			this.duration = duration;
		}
		public int getVelocity() {
			return velocity;
		}
		public void setVelocity(int velocity) {
			this.velocity = velocity;
		}
		public String getType() {
			return type;
		}
		public void setType(String type) {
			this.type = type;
		}
	}

	private static String JSON_DIR = "/Users/norkish/Archive/2016_BYU/dlrgroup/software/JSONs";
	private static String HUMAN_LYRIC_ANNOTATION_DIR = "/Users/norkish/Archive/2016_BYU/dlrgroup/software/annotation";
	private static String serializedLyricsPath = TabDriver.serializedDataDir + "/lyricsForMIDI.ser";
	
	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		// Load in the metadata csv file
		Map<String, Map<String, Object>> annots = PopMidParser.load_popmid(new File("/Users/norkish/Archive/2016_BYU/dlrgroup/software/metadata_for_midis.csv"));
		
		Map<String,Map<String,String>> songs = new HashMap<String,Map<String,String>>();
		
		String artist, title;
		Map<String,String> songsByArtist;
		for (String fname : annots.keySet()) {
			Map<String, Object> annot = annots.get(fname);
			artist = ((String)annot.get("artist")).trim().toLowerCase().replaceAll("&", "and").replaceAll("[^a-z0-9 ]", "").replaceFirst("^the ", "");
			title = ((String)annot.get("title")).trim().toLowerCase().replaceAll("&", "and").replaceAll("[^a-z0-9 ]", "");
			
			songsByArtist = songs.get(artist);
			if (songsByArtist == null) {
				songsByArtist = new HashMap<String,String>();
				songs.put(artist, songsByArtist);
			}
			songsByArtist.put(title,fname);
		}
		boolean deserializeLyrics = true;
		// Load lyrics given the artist and song name
		Map<String, Map<String, List<LyricSheet>>> lyricSheets = (deserializeLyrics? (Map<String, Map<String, List<LyricSheet>>>) Serializer.load(serializedLyricsPath): RawDataLoader.loadLyricSheets(songs));
		boolean serialize = false;
		if (serialize)
			Serializer.serialize(lyricSheets, serializedLyricsPath);
		

		// For each JSON file
		JsonParser parser;
		JsonArray array, notes, lyrEvent;
		NoteEvent[] notesArray;
		NoteEvent noteEvent;
		List<Pair<String,NoteEvent[]>> parsedSong;
		String lyric, midiFileName, filePathName;
		File filePathNameFile;
		Gson gson;
		
		List<Pair<Triple<String,StressedPhone[],Integer>,NoteEvent[]>> fixedSong;
		List<List<Triple<String, StressedPhone[], Integer>>> phonemesAndSyllablesForSong;
		
		List<Triple<String, StressedPhone[], Integer>> currNewTokens;
		Pair<String, NoteEvent[]> currOldToken;
		String currOldLyr;
		NoteEvent[] currOldNotes;
		List<NoteEvent> currNewNotes;
		
		PrintWriter annotationFileWriter;
		for (String loadedArtist : songs.keySet()) {
			boolean fix = false; 
			if (loadedArtist.length() == 0) continue;
			Map<String, String> loadedSongsByArtist = songs.get(loadedArtist);
			if (!lyricSheets.containsKey(loadedArtist)){
				System.out.println("Fix artist \"" + loadedArtist + "\" (" + loadedSongsByArtist + ")");
				fix = true;
			} {//else {
				for (String loadedTitle : loadedSongsByArtist.keySet()) {
					midiFileName = loadedSongsByArtist.get(loadedTitle);
					filePathName = JSON_DIR + "/" + midiFileName.replaceFirst("\\.[^\\.]+$", ".midmel+lyr.JSON");
					filePathNameFile = new File(filePathName);
					if (!filePathNameFile.exists()) {
						//System.out.println("No file found for "+filePathName);
						continue;
					}
//					if (!lyricSheets.get(loadedArtist).containsKey(loadedTitle)) {
//						// Or if not found, let me find one
//						System.out.println("Fix title \"" + loadedTitle + "\" for artist \"" + loadedArtist + "\"");
//						fix = true;
////						continue;
//					}
					if(!fix) continue;
					System.out.println("Loading file " + filePathName);
					// Find associated lyric file
					// http://stackoverflow.com/questions/16377754/parse-json-file-using-gson
					parser = new JsonParser();
					array = parser.parse(new FileReader(filePathName)).getAsJsonArray();
					// Load JSON file
					gson = new Gson();
					// for each lyric event
					parsedSong = new ArrayList<Pair<String,NoteEvent[]>>();
					for (int i = 0; i < array.size(); i++) {
						lyrEvent = array.get(i).getAsJsonArray();
						lyric = lyrEvent.get(0).getAsString();
						// remove the kar newline metachars,parentheses, and any ampersands which we use as a special char in aln
						lyric = lyric.replaceAll("\\([^\\)\\(]*\\)", "").replaceAll("\\{[^\\}\\{]*\\}", "").replaceAll("&", "and").replaceAll("[/\\\\,.\"]", "");
						notes = lyrEvent.get(1).getAsJsonArray();
						notesArray = new NoteEvent[notes.size()];
//						System.out.println(lyric);
						for (int j = 0; j < notes.size(); j++) {
							noteEvent = gson.fromJson(notes.get(j), NoteEvent.class);
							notesArray[j] = noteEvent;
//							System.out.println("\t" + noteEvent.getType().toString());
						}
						parsedSong.add(new Pair<String,NoteEvent[]>(lyric, notesArray));
					}

					for (Pair<String, NoteEvent[]> pair : parsedSong) {
						System.out.print(pair.getFirst() + " ");
					}
					System.out.println();
					Scanner scan = new Scanner(System.in);
					scan.nextLine();
//					phonemesAndSyllablesForSong = recoverPhonemesAndSyllablesForLyrics(parsedSong, lyricSheets.get(loadedArtist).get(loadedTitle));
//					
//					// 1. Fix the syllables in the karaoke file 
//					fixedSong = new ArrayList<Pair<Triple<String,StressedPhone[],Integer>,NoteEvent[]>>();
//					
//					currNewNotes = null;
//					for (int i = 0; i < parsedSong.size(); i++) {
//						currOldToken = parsedSong.get(i);
//						currOldLyr = currOldToken.getFirst();
//						currOldNotes = currOldToken.getSecond();
//
//						currNewTokens = phonemesAndSyllablesForSong.get(i);
//						
//						// if the old token started with [[, keep it albeit this time using a Triple with the same token, and two null values associated with the old notes
//						if (currOldLyr.startsWith("[[")) {
//							fixedSong.add(new Pair<Triple<String,StressedPhone[],Integer>,NoteEvent[]>(new Triple<String,StressedPhone[],Integer>(currOldLyr, null, null), currOldNotes));
//						} else { // else the old token is a legit lyric
//							// if it was associated with nothing in the new version, 
//							if (currNewTokens.isEmpty()) {
//								//add its notes to the current list of note events
//								for (NoteEvent event : currOldNotes) {
//									currNewNotes.add(event);
//								}
//							} else { // other wise (if it was associated with one or syllable in the new version,)
//								// for each syllable with which it was associated, split the first note in the associated note event array and create a new list for each
//								// assign all remaining note events in the note event array to the current new list
//							}
//						}
//						
//					}
//					// TODO: change lyrics that didn't find alignment in the website to [[NO LYRICS]]
//					
//					// 2. Regenerate JSON?
//					
//					// 3. Print out the lyric file syllabified for rhyme/segment labeling
//					annotationFileWriter = new PrintWriter(new File(HUMAN_LYRIC_ANNOTATION_DIR + "/" + midiFileName.replaceFirst("\\.[^\\.]+$", ".toAnnotate.txt")));
//					annotationFileWriter.write("TITLE: " + loadedTitle + "\nARTIST: " + loadedArtist + "\n");
//					for (List<Triple<String, StressedPhone[], Integer>> list : phonemesAndSyllablesForSong) {
//						if (list == null) continue;
//						for (Triple<String, StressedPhone[], Integer> triple : list) {
//							lyric = triple.getFirst();
//							if (lyric.startsWith("" + KAR_PHRASE_DELIM)) {
//								annotationFileWriter.write("\n" + lyric.substring(1));
//							} else {
//								if (!lyric.startsWith(" ")){
//									annotationFileWriter.write("~");
//								}
//								annotationFileWriter.write(lyric);
//							}
//						}
//					}
//					annotationFileWriter.write("\n");
//					annotationFileWriter.close();
//					return;
				}
			}
		}
	}

	private static final String NON_WB_CHARS = "0-9A-Za-z_'";
	
	/**
	 * For the lyrics in the parsed song and using a (list of) lyric sheet(s) for use in validating and 
	 * recovering word boundaries, return a list with the same length as the parsedSong list where each entry in the new list
	 * contains the corrected syllable of the entry in the old list. Where multiple entries in the old list span a single syllable,
	 * the new list contains the syllable once at the index of the first of the old entries
	 *  Note that not all lyrics from the kar file are guaranteed to have syllables in the new list if, 
	 *  for example, the validating lyric sheet is missing lyrics that appear in the KAR file
	 * @param parsedSong
	 * @param list lyric sheets to use in validating lyrics (function picks the most complete by length)
	 * @return
	 */
	private static List<List<Triple<String, StressedPhone[], StressedPhone>>> recoverPhonemesAndSyllablesForLyrics(List<Pair<String, NoteEvent[]>> parsedSong, List<LyricSheet> list) {
		// Concatenate kar lyrics - done
		StringBuilder bldr = new StringBuilder();
		for (Pair<String,NoteEvent[]> pair : parsedSong) {
			String lyric = pair.getFirst();
			if(!lyric.startsWith("[["))
				bldr.append(lyric); 
		}
		String concatKarLyrs = bldr.toString(); 
		
		String lyricsSiteLyrics = getLongestLyricConcatenated(list);
		
		// Align concatenated kar lyrics with lyric site lyrics
		SequencePair.setCosts(1, -1, -1, 0);
		StringPairAlignment aln = (StringPairAlignment) Aligner.alignNW(new StringPair(concatKarLyrs.toLowerCase(), lyricsSiteLyrics.toLowerCase()));
		String concatKarLyrsLCAln = aln.getFirst();
		String lyricsSiteLyricsLCAln = aln.getSecond();
		System.out.println(concatKarLyrsLCAln.replaceAll("\n", "#"));
		System.out.println(lyricsSiteLyricsLCAln.replaceAll("\n", "#"));

		// split on [.-\s]+, get syllables for each word based on pronunciation of word with most syllables (assumption: better to erroneously split syllables than combine them) 
		String[] lyricsSiteWords = lyricsSiteLyrics.split("[^"+NON_WB_CHARS+"]+");
		
		List<String> tmp = new ArrayList<String>();
		for (Pair<String, NoteEvent[]> pair : parsedSong) {
			String lyric = pair.getFirst();
			if (!lyric.startsWith("[[")){
				for (String string : lyric.split("[^"+NON_WB_CHARS+"]+")) {
					string = string.trim();
					if (string.length()!=0)
						tmp.add(string);
				}
			}
		}
		String[] parsedSongTokens = tmp.toArray(new String[0]);
		
		
		List<Pair<String, List<String>>> karTokensForWords = alignKarTokensToWords(parsedSongTokens, lyricsSiteWords, concatKarLyrsLCAln, lyricsSiteLyricsLCAln); 
		
		// syllabify lyric site lyrics keeping all pronunciation
		List<List<Triple<String, StressedPhone[], StressedPhone>>> lyricsSiteWordSylSets = syllabifyWordsWithSyllableGuide(karTokensForWords);
		
		// words per song, syllables per word
		List<List<Triple<String, StressedPhone[], StressedPhone>>> parsedSongAssocSyls = getSylsAssocWithLyrics(parsedSong,
				lyricsSiteWordSylSets, concatKarLyrsLCAln, lyricsSiteLyricsLCAln);
		
		// \for beginning of phrases, /for beginning of lines but not phrases, " " for beginning of words		
		for (int i = 0; i < parsedSong.size(); i++) {
			System.out.print(parsedSong.get(i).getFirst());
			System.out.print(" ==> ");
			List<Triple<String, StressedPhone[], StressedPhone>> value = parsedSongAssocSyls.get(i);
//			if (value != null && value.size() > 0 && !value.get(0).getFirst().startsWith("" + KAR_PHRASE_DELIM) && !value.get(0).getFirst().startsWith(" "))
//				System.err.println("Warning: Word doesn't start with space or phrase delimiter (partial adding of syllables)");
			System.out.println(value==null?"null":Syllabifier.stringify(value));
		}
		
		return parsedSongAssocSyls;
	}
	
	private static List<Pair<String, List<String>>> alignKarTokensToWords(String[] parsedSong,
			String[] lyricsSiteWords, String concatKarLyrsLCAln, String lyricsSiteLyricsLCAln) {
		List<Pair<String, List<String>>> karTokensForWords = new ArrayList<Pair<String,List<String>>>();
		
		String karC, webC;
		int lyricsSiteWordIdx = -1, lyricsSiteWordCharIdx = -1, parsedSongWordIdx = -1, parsedSongWordCharIdx = -1;
		// iterate over alignment, skipping any characters in the regexWB set, and essentially replacing syllables in kar aligned string
		int tokenToAdd = -1;
		List<String> tokensForWord = null;
		for (int i = 0; i < concatKarLyrsLCAln.length(); i++) {
			// anywhere there's a new line (?) in the aligned web lyrics, we need to add a backslash in the kar to represent phrase beginning...
			karC = concatKarLyrsLCAln.substring(i, i+1);
			webC = lyricsSiteLyricsLCAln.substring(i, i+1);
			
			// we add the syllable currently being pointed to in by lyricsSiteWord(Syl(Char))Idx in lyricsSiteWordSylSets to parsedSongAssocSyls
			// when lyricsSiteWordSylIdx or lyricsSiteWordIdx changes value
			// keeping track that we add new lists and empty lyric events at the correct time to parsedSongAssocSyls as we advance past the contents of parsedSong via parsedsongWord(Char)Idx 

			// if character was aligned from karaoke string
			if (karC.matches("["+NON_WB_CHARS+"]")) {
				//advance pointers for parsedSong, adding new lists and empty lyric events at the correct time
				parsedSongWordCharIdx++;
				while (parsedSongWordIdx == -1 || parsedSongWordCharIdx == parsedSong[parsedSongWordIdx].length()) {
					parsedSongWordIdx++;
					parsedSongWordCharIdx = 0;
				}
				
				if(parsedSongWordCharIdx == 0) { //beginning of a token
					tokenToAdd = parsedSongWordIdx;
				}
			} else {
				if(parsedSongWordIdx != -1 && parsedSongWordCharIdx+1 == parsedSong[parsedSongWordIdx].length()) {
					// we're done with whatever the last token was
					tokenToAdd = -1;
				}
			}

			// if character was aligned from web string
			if (webC.matches("["+NON_WB_CHARS+"]")) {
				// advance pointers for lyricsSite, setting addSyllable as necessary
				lyricsSiteWordCharIdx++;
				while (lyricsSiteWordIdx == -1 || lyricsSiteWordCharIdx == lyricsSiteWords[lyricsSiteWordIdx].length()) {
					// advance to next syllable
					lyricsSiteWordIdx++;
					lyricsSiteWordCharIdx = 0;
				}
				if (lyricsSiteWordCharIdx == 0) {
					tokensForWord = new ArrayList<String>();
					karTokensForWords.add(new Pair<String, List<String>>(lyricsSiteWords[lyricsSiteWordIdx], tokensForWord));
				}
				
				if (tokenToAdd != -1) {
					tokensForWord.add(parsedSong[tokenToAdd]);
					tokenToAdd = -1;
				}
			}
		}		
		return karTokensForWords;
	}

	private static final char KAR_PHRASE_DELIM = '/';
	
	/**
	 * @param parsedSong
	 * @param lyricsSiteWordSylSets
	 * @param concatKarLyrsLCAln
	 * @param lyricsSiteLyricsLCAln
	 * @return
	 */
	private static List<List<Triple<String, StressedPhone[], StressedPhone>>> getSylsAssocWithLyrics(
			List<Pair<String, NoteEvent[]>> parsedSong,
			List<List<Triple<String, StressedPhone[], StressedPhone>>> lyricsSiteWordSylSets, String concatKarLyrsLCAln,
			String lyricsSiteLyricsLCAln) {
		List<List<Triple<String, StressedPhone[], StressedPhone>>> parsedSongAssocSyls = new ArrayList<List<Triple<String, StressedPhone[], StressedPhone>>>();
		int parsedSongWordIdx = -1;
		int parsedSongWordCharIdx = -1;
		int lyricsSiteWordIdx = -1;
		int lyricsSiteWordSylIdx = -1;
		int lyricsSiteWordSylCharIdx = -1;
		
		char karC;
		String webC;
		Triple<String, StressedPhone[], StressedPhone> sylToAdd = null;
		boolean pickupNewLineWithNextSyllable = true;
		// iterate over alignment, skipping any characters in the regexWB set, and essentially replacing syllables in kar aligned string
		for (int i = 0; i < concatKarLyrsLCAln.length(); i++) {
			// anywhere there's a new line (?) in the aligned web lyrics, we need to add a backslash in the kar to represent phrase beginning...
			karC = concatKarLyrsLCAln.charAt(i);
			webC = lyricsSiteLyricsLCAln.substring(i, i+1);
			
			// we add the syllable currently being pointed to in by lyricsSiteWord(Syl(Char))Idx in lyricsSiteWordSylSets to parsedSongAssocSyls
			// when lyricsSiteWordSylIdx or lyricsSiteWordIdx changes value
			// keeping track that we add new lists and empty lyric events at the correct time to parsedSongAssocSyls as we advance past the contents of parsedSong via parsedsongWord(Char)Idx 

			// if character was aligned from web string
			if (webC.matches("["+NON_WB_CHARS+"]")) {
				// advance pointers for lyricsSite, setting addSyllable as necessary
				lyricsSiteWordSylCharIdx++;
				while (lyricsSiteWordSylIdx == -1 || lyricsSiteWordSylCharIdx == lyricsSiteWordSylSets.get(lyricsSiteWordIdx).get(lyricsSiteWordSylIdx).getFirst().length()) {
					// advance to next syllable
					lyricsSiteWordSylIdx++;
					lyricsSiteWordSylCharIdx = 0;
					if (lyricsSiteWordIdx == -1 || lyricsSiteWordSylIdx == lyricsSiteWordSylSets.get(lyricsSiteWordIdx).size()) {
						lyricsSiteWordIdx++;
						lyricsSiteWordSylIdx = 0;
					}
					if (lyricsSiteWordSylSets.get(lyricsSiteWordIdx).get(lyricsSiteWordSylIdx).getFirst().length() != 0 &&
							lyricsSiteWordSylSets.get(lyricsSiteWordIdx).get(lyricsSiteWordSylIdx).getFirst().toLowerCase().charAt(lyricsSiteWordSylCharIdx) != webC.charAt(0))
						System.out.println("At position " + i + " in aln, expected " + lyricsSiteWordSylSets.get(lyricsSiteWordIdx).get(lyricsSiteWordSylIdx).getFirst().toLowerCase().charAt(lyricsSiteWordSylCharIdx) + ", found " + webC);
					// now pointing at new syllable, set it to be picked up
					sylToAdd = lyricsSiteWordSylSets.get(lyricsSiteWordIdx).get(lyricsSiteWordSylIdx);
					if (lyricsSiteWordSylIdx == 0) { // If it's the first syllable in a word
						if (pickupNewLineWithNextSyllable) { // If this is the first syllable found since last seeing a new line character
							// add a new phrase character to the lyric
							sylToAdd = new Triple<String, StressedPhone[], StressedPhone>(KAR_PHRASE_DELIM + sylToAdd.getFirst(), sylToAdd.getSecond(),sylToAdd.getThird()); 
							pickupNewLineWithNextSyllable = false;
						} else {
							// Otherwise, if it's not the beginning of a phrase, then put a space in front of it
							sylToAdd = new Triple<String, StressedPhone[], StressedPhone>(' ' + sylToAdd.getFirst(), sylToAdd.getSecond(),sylToAdd.getThird()); // add a new phrase character to the lyric
						}
					}
				}
			} else if (webC.matches("[\n\r]")) {
				pickupNewLineWithNextSyllable = true;
			}

			// if character was aligned from karaoke string
			if (karC != AlignmentBuilder.INDEL_CHAR) {
				//advance pointers for parsedSong, adding new lists and empty lyric events at the correct time
				parsedSongWordCharIdx++;
				while (parsedSongWordIdx == -1 || parsedSongWordCharIdx == parsedSong.get(parsedSongWordIdx).getFirst().length()) {
					parsedSongWordIdx++;
					parsedSongWordCharIdx = 0;
					while (parsedSong.get(parsedSongWordIdx).getFirst().startsWith("[[")){
						parsedSongAssocSyls.add(null);
						parsedSongWordIdx++;
					}
					parsedSongAssocSyls.add(new ArrayList<Triple<String, StressedPhone[], StressedPhone>>());				
				}
				// can only add syllables if a character in the karaoke file aligns to it AND if it hasn't been already added.
				if (sylToAdd != null) { // If something is ready to be added
					// If there’s already a syllable assigned to a lyric event, then wait to see a vowel to add a second syllable (unless there are no more letters in the word (not the kar word, the real one))
					if (parsedSongAssocSyls.get(parsedSongWordIdx).size() == 0 || "aeiouy".indexOf(karC) != -1) { // || lyricsSiteWordSylCharIdx == lyricsSiteWordSylSets.get(lyricsSiteWordIdx).get(lyricsSiteWordSylIdx).getFirst().length()-1) { 
						parsedSongAssocSyls.get(parsedSongWordIdx).add(sylToAdd);
						sylToAdd = null;
					}
				}
			}
		}
		
		// Will point to the last valid kar lyric until it finds a new one (which it won't 'cause we're done) or we manually advance
		parsedSongWordIdx++;
		
		while (parsedSongWordIdx < parsedSong.size() && parsedSong.get(parsedSongWordIdx).getFirst().startsWith("[[")){
			parsedSongAssocSyls.add(null);
			parsedSongWordIdx++;
		}
		return parsedSongAssocSyls;
	}
	/**
	 * @param karTokensForWords 
	 * @param lyricsSiteLyrics
	 * @return
	 */
	private static List<List<Triple<String, StressedPhone[], StressedPhone>>> syllabifyWordsWithSyllableGuide(List<Pair<String, List<String>>> karTokensForWords) {
		// per word, per pronunciation, per syllable
		List<List<Triple<String, StressedPhone[], StressedPhone>>> lyricsSiteWordSylSets = new ArrayList<List<Triple<String, StressedPhone[], StressedPhone>>>();
		// for each word
		String word;
		int optimalSyllableCount;
		for (int i = 0; i < karTokensForWords.size(); i++) {
			word = karTokensForWords.get(i).getFirst();
			optimalSyllableCount = karTokensForWords.get(i).getSecond().size();
			if (word.length() == 0)
				continue;
			// for each pronunciation of the word
			List<Triple<String, StressedPhone[], StressedPhone>> optimalPronunciation = null;
			for(StressedPhone[] p : Phonetecizer.getPhones(word,false)) {
				List<Triple<String, StressedPhone[], StressedPhone>> pronunciation = Syllabifier.syllabify(word, p);
				if(optimalPronunciation == null || Math.abs(pronunciation.size() - optimalSyllableCount) < Math.abs(optimalPronunciation.size() - optimalSyllableCount)) { 
					optimalPronunciation = pronunciation;
				}
			}
			lyricsSiteWordSylSets.add(optimalPronunciation);
		}
		return lyricsSiteWordSylSets;
	}
	/**
	 * @param list
	 * @return
	 */
	private static String getLongestLyricConcatenated(List<LyricSheet> list) {
		// concatenate (longest) lyric site lyrics
		String lyricsSiteLyrics = "";
		for (LyricSheet lyricSheet : list) {
			String nextLyrics = lyricSheet.getLyrics().replaceAll("[/\\\\,.\"]", "");
			if (nextLyrics.length() > lyricsSiteLyrics.length())
				lyricsSiteLyrics = nextLyrics;
		}
		lyricsSiteLyrics = lyricsSiteLyrics.replaceAll("&", "and");
		return lyricsSiteLyrics;
	}
}