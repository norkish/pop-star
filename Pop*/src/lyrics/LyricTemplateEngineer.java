package lyrics;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;

import composition.Measure;
import composition.Score;
import condition.Rhyme;
import constraint.Constraint;
import data.MusicXMLModel;
import data.MusicXMLModelLearner;
import data.MusicXMLParser.Note;
import data.MusicXMLParser.NoteLyric;
import data.MusicXMLParser.NoteTie;
import data.MusicXMLParser.Syllabic;
import data.ParsedMusicXMLObject;
import globalstructure.SegmentType;
import inspiration.Inspiration;
import utils.Triple;

public class LyricTemplateEngineer extends LyricalEngineer {

	public static class LyricTemplateEngineerMusicXMLModel extends MusicXMLModel {

		Map<Integer, List<List<NoteLyric>>> templatesBySyllableLength = new HashMap<Integer, List<List<NoteLyric>>>();
		
		@Override
		/**
		 * Only trains on examples with 95% of the lyrics recognizable English lyrics
		 */
		public void trainOnExample(ParsedMusicXMLObject musicXML) {
			
			if (musicXML.totalSyllables == 0 || musicXML.totalSyllablesWithStressFromEnglishDictionary == 0) {
				return;
			}
			
			List<Triple<Integer, Integer, Note>> notes = musicXML.notesByPlayedMeasure;
			SortedMap<Integer, Double> phraseBeginnings = musicXML.phraseBeginnings;
			List<NoteLyric> currPhrase = null;
			for (Triple<Integer, Integer, Note> triple : notes) {
				int measure = triple.getFirst();
				int divsOffset = triple.getSecond();
				double beatsOffset = musicXML.divsToBeats(divsOffset, measure);
				if (phraseBeginnings.get(measure) == divsOffset) {
					if (currPhrase != null && !currPhrase.isEmpty()) {
						addTemplate(currPhrase);
					}
					currPhrase = new ArrayList<NoteLyric>();
				}
				Note note = triple.getThird();
				if (note.lyric != null && note.lyric.text != null) {
					currPhrase.add(note.lyric);
				}
			}
		}

		private void addTemplate(List<NoteLyric> currPhrase) {
			List<List<NoteLyric>> templatesForSize = templatesBySyllableLength.get(currPhrase.size());
			if (templatesForSize == null) {
				templatesForSize = new ArrayList<List<NoteLyric>>();
				templatesBySyllableLength.put(currPhrase.size(), templatesForSize);
			}
			templatesForSize.add(currPhrase);
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

		private static Random rand = new Random();
		public List<NoteLyric> sampleTemplate(int phraseLength) {
			List<List<NoteLyric>> templatesForLength = templatesBySyllableLength.get(phraseLength);
			int offsetIntoDistribution = rand.nextInt(templatesForLength.size());
			return templatesForLength.get(offsetIntoDistribution);
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
		//TODO: get phrase lengths from score somehow and sample model to get lyrics
		int phraseLength = 0;
		List<NoteLyric> template = model.sampleTemplate(phraseLength);
		List<NoteLyric> lyrics = getExternalLyrics();//model.templatesBySyllableLength.get(258).get(0);
		int lyrIdx = 0;
		
		int chorusStartMeasure = -1;
		int chorusMeasureCounter = -1;
		
		SegmentType prevType = null;
		
		for (int currMeasureNumber = 0; currMeasureNumber < measures.size(); currMeasureNumber++) {
			Measure measure = measures.get(currMeasureNumber);
			
			boolean generateLyrics = true;
			switch (measure.segmentType) {
			case INTERLUDE:
			case INTRO:
			case OUTRO:
				generateLyrics = false;
				break; // don't generate new lyrics
			case CHORUS:
				if (prevType != SegmentType.CHORUS) {
					// beginning of chorus
					if (chorusStartMeasure == -1) {
						// first chorus
						chorusStartMeasure = currMeasureNumber;
						break; // go on to generate lyrics
					} else {
						chorusMeasureCounter = 0;
					}
				}
				
				if (chorusMeasureCounter != -1) {
					// we're on a repeat of the chorus, need to go back and copy previous chorus lyrics
					// TODO: this assumes that exact same notes exist between choruses, what if they change? this isn't robust
					TreeMap<Double, Note> otherNotes = measures.get(chorusStartMeasure+chorusMeasureCounter).getNotes();
					TreeMap<Double, Note> notes = measure.getNotes();
					for (Double offset: otherNotes.keySet()) {
						Note thisNote = notes.get(offset);
						Note otherNote = otherNotes.get(offset);
						thisNote.lyric = otherNote.lyric == null ? null : new NoteLyric(otherNote.lyric);
					}
					
					chorusMeasureCounter++;
					generateLyrics = false;
				}
				break;
			default:
				break;
			}
			
			if (generateLyrics) {
				TreeMap<Double, Note> notes = measure.getNotes();
				for (Note note : notes.values()) {
					if (note.pitch != Note.REST && note.tie != NoteTie.STOP) {
						note.lyric = lyrics.get(lyrIdx++);
						if (lyrIdx == lyrics.size()) return;
					}
				}
			}
			prevType = measure.segmentType;
		}
	}

	private List<NoteLyric> getExternalLyrics() {
		String text = null;
		try {
			text = new String(Files.readAllBytes(Paths.get("externalLyrics.txt")), StandardCharsets.UTF_8);
		} catch (IOException e) {
			e.printStackTrace();
		}
		String[] words = text.split("\\s+");
		List<NoteLyric> lyrics = new ArrayList<NoteLyric>();
		for (String word : words) {
			lyrics.add(new NoteLyric(Syllabic.SINGLE, word, false, false));
		}
		return lyrics;
	}
}
