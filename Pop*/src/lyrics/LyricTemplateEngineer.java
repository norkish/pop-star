package lyrics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import composition.Measure;
import composition.Score;
import data.MusicXMLModel;
import data.MusicXMLModelLearner;
import data.MusicXMLParser.Note;
import data.MusicXMLParser.NoteLyric;
import data.MusicXMLParser.NoteTie;
import data.MusicXMLParser.Syllabic;
import data.ParsedMusicXMLObject;
import inspiration.Inspiration;
import utils.Triple;

public class LyricTemplateEngineer extends LyricalEngineer {

	public static class LyricTemplateEngineerMusicXMLModel extends MusicXMLModel {

		Map<Integer, List<List<NoteLyric>>> templatesBySyllableLength = new HashMap<Integer, List<List<NoteLyric>>>();
		
		@Override
		/**
		 * Only trains on examples with 95% of the lyrics recognizable English lyrics
		 * Phrases are any string of note-associated lyrics that are demarcated by long stretches of rest or sentence-ending punctuation 
		 */
		public void trainOnExample(ParsedMusicXMLObject musicXML) {
			
			if (musicXML.totalSyllables == 0 || musicXML.totalSyllablesWithStressFromEnglishDictionary == 0) {
				return;
			}
				
			List<NoteLyric> lyricSequence = new ArrayList<NoteLyric>();
			
			int syllableCount = 0;
			
			for (Triple<Integer, Integer, Note> triple : musicXML.notesByMeasure){
				Note note = triple.getThird();
				NoteLyric lyric = note.lyric;
				if (lyric == null || lyric.text == null)
					continue;
				syllableCount++;
				lyricSequence.add(lyric);
			}
			
			List<List<NoteLyric>> templatesOfSpecifiedLen = templatesBySyllableLength.get(syllableCount);
			
			if (templatesOfSpecifiedLen == null) {
				templatesOfSpecifiedLen = new ArrayList<List<NoteLyric>>();
				templatesBySyllableLength.put(syllableCount, templatesOfSpecifiedLen);
			}
			templatesOfSpecifiedLen.add(lyricSequence);
		}
		
		public String toString() {
			StringBuilder str = new StringBuilder();
			
			for (Integer key : templatesBySyllableLength.keySet()) {
				for (List<NoteLyric> lyricSeq : templatesBySyllableLength.get(key)) {
					str.append(key).append('\t');
					for (NoteLyric noteLyric : lyricSeq) {
						str.append(noteLyric.text);
						if (noteLyric.syllabic == Syllabic.SINGLE || noteLyric.syllabic == Syllabic.END) str.append(' ');
					}
				}
			}
			
			return str.toString();
		}
	}

	private LyricTemplateEngineerMusicXMLModel model;
	
	public LyricTemplateEngineer() {
		this.model = (LyricTemplateEngineerMusicXMLModel) MusicXMLModelLearner.getTrainedModel(this.getClass());
	}

	@Override
	public void addLyrics(Inspiration inspiration, Score score) {
		//use score to get constraints and then model to find lyrics that fit constraints
		List<Measure> measures = score.getMeasures();
		
		List<NoteLyric> lyrics = model.templatesBySyllableLength.get(258).get(0);
		int lyrIdx = 0;
		
		for (Measure measure : measures) {
			TreeMap<Double, Note> notes = measure.getNotes();
			for (Note note : notes.values()) {
				if (note.pitch != -1 && note.tie != NoteTie.STOP) {
					note.lyric = lyrics.get(lyrIdx++);
					if (lyrIdx == lyrics.size()) return;
				}
			}
		}
	}


}
