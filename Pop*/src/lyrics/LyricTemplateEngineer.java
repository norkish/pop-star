package lyrics;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
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
import data.MusicXMLModel;
import data.MusicXMLModelLearner;
import data.MusicXMLParser.Note;
import data.MusicXMLParser.NoteLyric;
import data.MusicXMLParser.NoteTie;
import data.MusicXMLParser.Syllabic;
import data.ParsedMusicXMLObject;
import globalstructure.SegmentType;
import inspiration.Inspiration;
import segmentstructure.SegmentStructureExtractor;
import utils.Pair;
import utils.Triple;
import utils.Utils;

public class LyricTemplateEngineer extends LyricalEngineer {

	public static class LyricTemplateEngineerMusicXMLModel extends MusicXMLModel {

		SortedMap<Integer, List<List<NoteLyric>>> templatesBySyllableLength = new TreeMap<Integer, List<List<NoteLyric>>>();
		
		@Override
		/**
		 * Only trains on examples with 95% of the lyrics recognizable English lyrics
		 */
		public void trainOnExample(ParsedMusicXMLObject musicXML) {
			
			if (musicXML.totalSyllables == 0 || musicXML.totalSyllablesWithStressFromEnglishDictionary == 0) {
				return;
			}
			
			List<Triple<Integer, Integer, Note>> notes = musicXML.getNotesByPlayedMeasure();
			SortedMap<Integer, Double> phraseBeginnings = musicXML.phraseBeginnings;
			SortedMap<Integer, SegmentType> globalStructure = musicXML.globalStructure;
			List<NoteLyric> currPhrase = null;
			SegmentType currSegment = null;
			for (Triple<Integer, Integer, Note> triple : notes) {
				int measure = triple.getFirst();
				if (globalStructure.containsKey(measure)) {
					currSegment = globalStructure.get(measure);
				}
				int divsOffset = triple.getSecond();
				double beatsOffset = musicXML.divsToBeats(divsOffset, measure);
				Double phraseBeginningInMeasure = phraseBeginnings.get(measure);
				if (phraseBeginningInMeasure != null && phraseBeginningInMeasure == beatsOffset) {
					if (currPhrase != null && !currPhrase.isEmpty()) {
						addTemplate(currPhrase);
					}
					currPhrase = new ArrayList<NoteLyric>();
				}
				Note note = triple.getThird();
				NoteLyric lyric = note.getLyric(currSegment != SegmentType.CHORUS);
				if (lyric != null && lyric.text != null) {
					currPhrase.add(lyric);
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
			
			for (Integer syllableLength : templatesBySyllableLength.keySet()) {
				for (List<NoteLyric> lyricSeq : templatesBySyllableLength.get(syllableLength)) {
					str.append(syllableLength).append('\t');
					for (NoteLyric noteLyric : lyricSeq) {
						str.append(noteLyric.text);
						if (noteLyric.syllabic == Syllabic.SINGLE || noteLyric.syllabic == Syllabic.END) str.append(' ');
					}
					str.append('\n');
				}
			}
			
			return str.toString();
		}

		private static Random rand = new Random();
		/**
		 * returned template may contain nulls to pad to phraseLength if no template can be found that perfectly matches phraseLength 
		 * @param phraseLength
		 * @return
		 */
		public List<NoteLyric> sampleTemplate(int phraseLength) {
			List<NoteLyric> template = new ArrayList<NoteLyric>();
			int lengthLeftToFill = phraseLength;
			while(lengthLeftToFill > 0) {
				List<List<NoteLyric>> templatesForLength = Utils.valueForKeyBeforeOrEqualTo(lengthLeftToFill, templatesBySyllableLength);
				if (templatesForLength == null) {
					System.err.println("No lyric templates with syllable count <= " + lengthLeftToFill);
					while(template.size() < phraseLength) {
						template.add(null);
					}
				} else {
					int offsetIntoDistribution = rand.nextInt(templatesForLength.size());
					template.addAll(templatesForLength.get(offsetIntoDistribution));
				}
				lengthLeftToFill = phraseLength - template.size();
			}
			return template;
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
		List<Triple<SegmentType, List<Integer>, List<Integer>>> syllablesPerPhrase = getNoteCountBySegment(score);
		
		List<Triple<SegmentType, List<List<NoteLyric>>, List<Integer>>> templatesBySegment = new ArrayList<Triple<SegmentType, List<List<NoteLyric>>, List<Integer>>>();
		boolean chorusTemplateGenerated = false;
		List<Integer> templateLengths;
		List<List<NoteLyric>> templates = null;
		for (Triple<SegmentType, List<Integer>, List<Integer>> triple : syllablesPerPhrase) {
			boolean sampleLyrics = false;
			switch (triple.getFirst()) {
			case CHORUS:
				if (chorusTemplateGenerated)
					templates = null;
				else {
					sampleLyrics = true;
				}
				break;
			case BRIDGE:
			case INTERLUDE:
			case INTRO:
			case OUTRO:
			case PRECHORUS:
			case VERSE:
				sampleLyrics = true;
				break;
			default:
				break;
			}
			
			if (sampleLyrics) {
				templateLengths = triple.getSecond();
				templates = new ArrayList<List<NoteLyric>>();
				for (Integer templateLength : templateLengths) {
					templates.add(model.sampleTemplate(templateLength));
				}
			}
			
			templatesBySegment.add(new Triple<SegmentType, List<List<NoteLyric>>, List<Integer>>(triple.getFirst(), templates, triple.getThird()));
		}
		int lyrIdx = 0;
		int segmentIdx = -1;
		int phraseIdx = -1;
		List<List<NoteLyric>> currTemplates = null;
		
		int chorusStartMeasure = -1;
		int chorusMeasureCounter = -1;
		
		SegmentType prevType = null, currType;
		printTemplateForLyrist(inspiration, templatesBySegment);
		List<Triple<SegmentType, List<List<NoteLyric>>, List<Integer>>> lyricPhrasesToAdd = templatesBySegment;
		
		boolean inTheMiddleOfATie = false;
		for (int currMeasureNumber = 0; currMeasureNumber < measures.size(); currMeasureNumber++) {
			Measure measure = measures.get(currMeasureNumber);
			
			boolean generateLyrics = true;
			currType = measure.segmentType;
			if (currType != prevType) {
				phraseIdx = 0;
				lyrIdx = 0;
				segmentIdx++;
				currTemplates = lyricPhrasesToAdd.get(segmentIdx).getSecond();
			}
			switch (currType) {
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
						NoteLyric otherLyric = otherNote.getLyric(false);
						thisNote.setLyric(otherLyric == null ? null : new NoteLyric(otherLyric), true);
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
					if (note.tie == NoteTie.STOP) {
						inTheMiddleOfATie = false;
					}
					if (note.isPlayedNoteOnset() && !inTheMiddleOfATie) {
						if (note.tie == NoteTie.START) {
							inTheMiddleOfATie = true;
						}
						List<NoteLyric> currTemplate = currTemplates.get(phraseIdx);
						if (lyrIdx < currTemplate.size()) {
							note.setLyric(currTemplate.get(lyrIdx++), true);
							if (lyrIdx == currTemplate.size()) {
								phraseIdx++;
								lyrIdx = 0;
							}
						}
					}
				}
			}
			prevType = currType;
		}
		assert segmentIdx == lyricPhrasesToAdd.size()-1;
	}

	private void printTemplateForLyrist(Inspiration inspiration, List<Triple<SegmentType, List<List<NoteLyric>>, List<Integer>>> templatesBySegment) {
//		File lyricFile = new File("lyricTemplate.txt");
//		File rhymeFile = new File("rhymeTemplate.txt");
//		PrintWriter lyricFileWriter = new PrintWriter(lyricFile);
//		PrintWriter rhymeFileWriter = new PrintWriter(rhymeFile);
		
		System.out.println("TITLE: BSSF");
		System.out.println("INSPIRATION: " + inspiration.getMaxEmotion());
		System.out.println();

		for (Triple<SegmentType, List<List<NoteLyric>>, List<Integer>> triple : templatesBySegment) {
			SegmentType type = triple.getFirst();
			List<List<NoteLyric>> templatePhrasesForSegment = triple.getSecond();
			List<Integer> rhymeGroupLabels = triple.getThird();
			assert templatePhrasesForSegment.size() == rhymeGroupLabels.size() : "mismatch between rhyme groups and phrases";
			System.out.println(type);
			for (int i = 0; i < templatePhrasesForSegment.size(); i++) {
				List<NoteLyric> templatePhrase = templatePhrasesForSegment.get(i);
				Integer rhymeGroupNumber = rhymeGroupLabels.get(i);
				char rhymeGroupLabel = (char) (rhymeGroupNumber == null?' ':'A' + rhymeGroupNumber);
				System.out.print(rhymeGroupLabel + "\t");
				for (NoteLyric noteLyric : templatePhrase) {
					if (noteLyric == null) {
						System.out.print("null ");
					} else {
						System.out.print(noteLyric.text + (noteLyric.syllabic == Syllabic.SINGLE || noteLyric.syllabic == Syllabic.END?' ':'•'));
					}
				}
				System.out.println();
			}
			System.out.println();
		}
		
	}

	private List<Triple<SegmentType, List<Integer>, List<Integer>>> getNoteCountBySegment(Score score) {
		List<Triple<SegmentType, List<Integer>, List<Integer>>> noteCountBySegment = new ArrayList<Triple<SegmentType, List<Integer>,List<Integer>>>();
		
		//TODO: get phrase lengths from score somehow and sample model to get lyrics
		List<Measure> measures = score.getMeasures();
		SegmentType prevType = null, currType;
		int currPhraseNoteCount = 0;
		int currPhraseMeasureCount = 0;
		List<Integer> currSegmentPhrases = new ArrayList<Integer>();
		List<Integer> currSegmentRhymeGroups = new ArrayList<Integer>();
		Measure prevMeasure = null, nextMeasure = null, measure = measures.isEmpty()? null :measures.get(0);
		boolean forceEndPhraseOnNextNote = false; 
		Map<Integer,Map<Double,Integer>> rhymeGroupByMeasureOffset = new HashMap<Integer,Map<Double,Integer>>();
		int nextRhymeGroup = 0;
		int prevPhraseRhymeGroup = -1;
		boolean inTheMiddleOfATie = false;
		for (int currMeasureNumber = 0; currMeasureNumber < measures.size(); currMeasureNumber++) {
			if (currMeasureNumber + 1 < measures.size()) {
				nextMeasure = measures.get(currMeasureNumber+1);
			} else {
				nextMeasure = null;
			}
			currType = measure.segmentType;
			
			// if new segment
			boolean addPreviousSegment = currType != prevType && prevType != null;
			if (addPreviousSegment) {
				if (currPhraseMeasureCount >= SegmentStructureExtractor.MIN_FULL_MSRS_LYR_PHRASE && currPhraseNoteCount > 0) {
					currSegmentPhrases.add(currPhraseNoteCount);
					currSegmentRhymeGroups.add(null);
					currPhraseNoteCount = 0;
					currPhraseMeasureCount = 0;
				}
				
				noteCountBySegment.add(new Triple<SegmentType, List<Integer>,List<Integer>>(prevType, currSegmentPhrases, currSegmentRhymeGroups));
				currSegmentPhrases = new ArrayList<Integer>();
				currSegmentRhymeGroups = new ArrayList<Integer>();
				rhymeGroupByMeasureOffset = new HashMap<Integer,Map<Double,Integer>>();
			}
			
			TreeMap<Double, Note> notes = measure.getNotes();
			// get the offset in the segment structure at which a phrase-ending rhyme constraint occurs
			Pair<Rhyme<NoteLyric>, Double> phraseEndingRhymeAndOffset = measure.getPhraseEndingRhymeAndOffset();
			double  phraseEndOffset = -1.;
			Integer  phraseRhymeGroup = -1;
			if (phraseEndingRhymeAndOffset != null) {
				phraseEndOffset = phraseEndingRhymeAndOffset.getSecond();
				Rhyme<NoteLyric> condition = phraseEndingRhymeAndOffset.getFirst();
				int rhymeDefiningMeasure = condition.getReferenceMeasure();
				double rhymeDefiningOffset = condition.getReferenceOffset();
				Map<Double, Integer> rhymeGroupByOffset = rhymeGroupByMeasureOffset.get(rhymeDefiningMeasure);
				if (rhymeGroupByOffset == null) {
					rhymeGroupByOffset = new HashMap<Double,Integer>();
					rhymeGroupByMeasureOffset.put(rhymeDefiningMeasure, rhymeGroupByOffset);
				}
				phraseRhymeGroup = rhymeGroupByOffset.get(rhymeDefiningOffset);
				if (phraseRhymeGroup == null) {
					phraseRhymeGroup = nextRhymeGroup;
					rhymeGroupByOffset.put(rhymeDefiningOffset,phraseRhymeGroup);
					nextRhymeGroup++;
				}
			}
			
			// find the played note in the measure (or in the prev or following measure) that occurs closes to that offset
			double closestNoteOffsetToPhraseEndOffset = measure.getClosestNoteOffset(phraseEndOffset,prevMeasure, nextMeasure);
			// if closest note is in previous measure, the phrase is done, so add it.
			if (closestNoteOffsetToPhraseEndOffset == Double.NEGATIVE_INFINITY) { // constraint really already passed, end phrase
				currSegmentPhrases.add(currPhraseNoteCount);
				currSegmentRhymeGroups.add(phraseRhymeGroup);
				currPhraseNoteCount = 0;
			}
			// add notes, and if the closest note is one of the notes in the measure, include it and then end the phrase.
			for (Double beatOffset : notes.keySet()) {
				Note note = notes.get(beatOffset);
				if (note.tie == NoteTie.STOP) {
					inTheMiddleOfATie = false;
				}
				if (note.isPlayedNoteOnset() && !inTheMiddleOfATie) {
					if (note.tie == NoteTie.START) {
						inTheMiddleOfATie = true;
					}
					currPhraseNoteCount++;
					if (forceEndPhraseOnNextNote || beatOffset == closestNoteOffsetToPhraseEndOffset) {
						// add previous phrase
						currSegmentPhrases.add(currPhraseNoteCount);
						if (forceEndPhraseOnNextNote) {
							currSegmentRhymeGroups.add(prevPhraseRhymeGroup);
							forceEndPhraseOnNextNote = false;
						} else {
							currSegmentRhymeGroups.add(phraseRhymeGroup);
						}
						currPhraseNoteCount = 0;
					}
				}
			}
			// if closest note is in the next measure, force the first note in the next measure to end the phrase.
			if (closestNoteOffsetToPhraseEndOffset == Double.POSITIVE_INFINITY) { // constraint is next note in next measure, end phrase
				prevPhraseRhymeGroup = phraseRhymeGroup;
				forceEndPhraseOnNextNote = true;
			}
			currPhraseMeasureCount++;
			prevType = currType;
			prevMeasure = measure;
			measure = nextMeasure;
		}
		if (prevType != null) { // if we saw anything
			if (currPhraseMeasureCount >= SegmentStructureExtractor.MIN_FULL_MSRS_LYR_PHRASE && currPhraseNoteCount > 0) {
				currSegmentPhrases.add(currPhraseNoteCount);
				currSegmentRhymeGroups.add(null);
			}
			
			noteCountBySegment.add(new Triple<SegmentType, List<Integer>,List<Integer>>(prevType, currSegmentPhrases, currSegmentRhymeGroups));
		}
		
		return noteCountBySegment;
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
