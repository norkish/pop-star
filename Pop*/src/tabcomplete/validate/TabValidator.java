package tabcomplete.validate;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import harmony.Chord;
import tabcomplete.alignment.Aligner;
import tabcomplete.alignment.ProgressiveMSA;
import tabcomplete.alignment.SequencePair;
import tabcomplete.rawsheet.ChordSheet;
import tabcomplete.rawsheet.LyricSheet;
import tabcomplete.rawsheet.RawDataLoader;
import tabcomplete.rhyme.RhymeStructureAnalyzer;
import tabcomplete.structure.SegmentStructureAnalyzer;
import tabcomplete.tab.CompletedTab;
import tabcomplete.utils.Utils;
import utils.Pair;

public class TabValidator {

	/**
	 * @param lyricSheetsByArtist
	 * @param chordSheetsByArtist
	 * @return 
	 */
	public static List<CompletedTab> validateTabs(Map<String, Map<String, List<LyricSheet>>> lyricSheetsByArtist,
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
		
		List<CompletedTab> completedTabs = new ArrayList<CompletedTab>();
		CompletedTab tabComplete;
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
					
					int[] scheme = RhymeStructureAnalyzer.extractRhymeScheme(words);
					
					char[] structure = SegmentStructureAnalyzer.extractSegmentStructure(words, chords);
					
					for (int i = 0; i < structure.length; i++) {
						System.out.println("" + i + "\t" + structure[i] + "\t" + scheme[i] + "\t" + words.get(i));
					}
					
					tabComplete = new CompletedTab(chordSheet.getKey(), words,chords,scheme,structure);
					completedTabs.add(tabComplete);
				}
			}
		}
		return completedTabs;
	}
	
	public static void main(String[] args) throws FileNotFoundException, IOException {
		Map<String, Map<String, List<LyricSheet>>> lyricSheetsByArtist = RawDataLoader.loadLyricSheets();
		Map<String, Map<String, List<ChordSheet>>> chordSheetsByArtist = RawDataLoader.loadChordSheets();

		validateTabs(lyricSheetsByArtist, chordSheetsByArtist);
	}
}