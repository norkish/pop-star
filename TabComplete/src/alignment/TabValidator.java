package alignment;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import harmony.Chord;
import raw.ChordSheet;
import raw.LyricSheet;
import raw.RawDataLoader;
import rhyme.RhymeStructureAnalyzer;
import structure.SegmentStructureAnalyzer;
import utils.Pair;
import utils.Utils;

public class TabValidator {

	/**
	 * @param lyricSheetsByArtist
	 * @param chordSheetsByArtist
	 */
	public static void validateTabs(Map<String, Map<String, List<LyricSheet>>> lyricSheetsByArtist,
			Map<String, Map<String, List<ChordSheet>>> chordSheetsByArtist) {
		
		Map<String, List<LyricSheet>> lyricSheetsForArtist;
		Map<String, List<ChordSheet>> chordSheetsForArtist;
		List<LyricSheet> lyricSheetsForArtistAndSong;
		int lyricCount;
		String[] lyrics;
		LyricSheet lyric;
		ProgressiveMSA msa;
		String consensus;
		List<ChordSheet> chordSheetsForArtistAndSong;
		Pair<List<SortedMap<Integer, Chord>>, List<String>> correctedTab;
		List<SortedMap<Integer, Chord>> chords;
		List<String> words;

		Aligner.setMinPercOverlap(.7);
		SequencePair.setCosts(1,-1, 0,0);
		
		for (String artist : chordSheetsByArtist.keySet()) {
			lyricSheetsForArtist = lyricSheetsByArtist.get(artist);
			if (lyricSheetsForArtist == null) continue;
			chordSheetsForArtist = chordSheetsByArtist.get(artist);
			for (String songName : chordSheetsForArtist.keySet()) {
//				if (!songName.equals("piano man")) continue;
				lyricSheetsForArtistAndSong = lyricSheetsForArtist.get(songName);
				if (lyricSheetsForArtistAndSong == null) continue;
				
				lyricCount = lyricSheetsForArtistAndSong.size();
				if (lyricCount < 2) continue;
				lyrics = new String[lyricCount];
				for (int i = 0; i < lyricCount; i++) {
					lyric = lyricSheetsForArtistAndSong.get(i);
					//System.out.println(lyric.getURL());
					lyrics[i] = lyric.getLyrics();
				}
				msa = new ProgressiveMSA(lyrics);
				consensus = msa.getConsensus();
				
				chordSheetsForArtistAndSong = chordSheetsForArtist.get(songName);
				for(ChordSheet chordSheet: chordSheetsForArtistAndSong){
					correctedTab = chordSheet.validate(consensus);
					if(correctedTab == null) {
						continue;
					}
					
					chords = correctedTab.getFirst();
					words = correctedTab.getSecond();
					
					System.out.println("Validated Tab:");
					for (int i = 0; i < chords.size(); i++) {
						System.out.println(chords.get(i));
						System.out.println(words.get(i));
					}
					
					char[] scheme = RhymeStructureAnalyzer.extractRhymeScheme(words);
					
					char[] structure = SegmentStructureAnalyzer.extractSegmentStructure(words, chords, scheme);
					
					for (int i = 0; i < structure.length; i++) {
						System.out.println("" + i + "\t" + structure[i] + "\t" + scheme[i] + "\t" + words.get(i));
					}
					Utils.promptEnterKey("");
				}
			}
		}
	}
	
	public static void main(String[] args) throws FileNotFoundException, IOException {
		Map<String, Map<String, List<LyricSheet>>> lyricSheetsByArtist = RawDataLoader.loadLyricSheets();
		Map<String, Map<String, List<ChordSheet>>> chordSheetsByArtist = RawDataLoader.loadChordSheets();

		validateTabs(lyricSheetsByArtist, chordSheetsByArtist);
	}
}
