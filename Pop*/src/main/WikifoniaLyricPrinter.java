package main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.List;

import org.w3c.dom.Document;

import data.MusicXMLParser;
import data.MusicXMLParser.Note;
import data.MusicXMLParser.NoteLyric;
import data.MusicXMLParser.Syllabic;
import data.MusicXMLSummaryGenerator;
import data.ParsedMusicXMLObject;
import tabcomplete.main.TabDriver;
import utils.Triple;

public class WikifoniaLyricPrinter {
	private static final File[] files = new File(TabDriver.dataDir + "/Wikifonia_xmls").listFiles();

	public static void main(String[] args) throws FileNotFoundException {
		PrintWriter writer = new PrintWriter("wikifonia_lyrics.txt");
		
		for (File file : files) {
			if (file.getName().startsWith(".DS")) continue;
			MusicXMLParser musicXMLParser = null;
			try {
				final Document xml = MusicXMLSummaryGenerator.parseXML(new FileInputStream(file));
				musicXMLParser = new MusicXMLParser(file.getName(), xml);
//				MusicXMLSummaryGenerator.printDocument(xml, System.out);
			} catch (Exception e) {
//				e.printStackTrace();
			}
			ParsedMusicXMLObject musicXML = null;
			try {
				musicXML = musicXMLParser.parse(true);
			} catch (RuntimeException e) {
				
			}
			if (musicXML == null) {
//				System.err.println("musicXML was null for " + file.getName());
				continue;
			}
			if (musicXML.totalSyllables < 10 || musicXML.totalSyllablesWithStressFromEnglishDictionary < musicXML.totalSyllables*.9) {
				continue;
			}
			
			List<Triple<Integer, Integer, Note>> notesByPlayedMeasure = musicXML.getNotesByPlayedMeasure();
			
			StringBuilder str = new StringBuilder();
			for (int i = 0; i < notesByPlayedMeasure.size(); i++) {
				Note note = notesByPlayedMeasure.get(i).getThird();
				// TRAIN LYRIC
				final NoteLyric lyric = note.getLyric(true);
				if (lyric != null) {
					str.append(lyric.text);
					if (lyric.syllabic == Syllabic.END || lyric.syllabic == Syllabic.SINGLE)
						str.append(' ');
				}
			}
			
//			String[] trainingSentences = str.toString().split("(?<=[;.!?:]+) ");
			writer.println(file.getName().trim());
			writer.println(str.toString().replaceAll("\n", " ").trim());
		}
		
		writer.close();
	}
}
