package globalstructure;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.SortedMap;
import java.util.TreeMap;

import condition.ConstraintCondition;
import condition.ExactBinaryMatch;
import condition.Rhyme;
import constraint.Constraint;
import data.MusicXMLParser.Note;
import data.MusicXMLParser.NoteLyric;
import data.MusicXMLParser.NoteTie;
import data.ParsedMusicXMLObject;
import utils.Pair;
import utils.Triple;

public class StructureExtractor {

	public static final String STRUCTURE_ANNOTATIONS_DIR = "wikifonia_structure_annotations";
	
	public static void annotateStructure(ParsedMusicXMLObject musicXML) {
		
		if (musicXML.unoverlappingHarmonyByPlayedMeasure.isEmpty() || musicXML.getNotesByPlayedMeasure().isEmpty()) {
			musicXML.setGlobalStructure(null);
			return;
		}

		loadStructureFromFile(musicXML);
		
		annotatePhraseBeginnings(musicXML);
	}

	final public static int MIN_FULL_MSRS_LYR_PHRASE = 2;

	private static void annotatePhraseBeginnings(ParsedMusicXMLObject musicXML) {
		
		SortedMap<Integer,Double> phraseBeginnings = new TreeMap<Integer, Double>();
		SortedMap<Integer, SortedMap<Integer, Note>> notesMap = musicXML.getNotesByPlayedMeasureAsMap();
		
		SortedMap<Integer, SortedMap<Double, SegmentType>> globalStructure = musicXML.getGlobalStructureBySegmentTokenStart();
		SortedMap<Integer, SortedMap<Double, List<Constraint<NoteLyric>>>> constraintsByNote = musicXML.segmentStructure;
		int lyricSequenceFirstMeasure = 0;
		
		boolean newSegmentStartNotAfterRhyme = true, prevLyrNoteRhymeConstrained = false, currLyrNoteRhymeConstrained = false;
		Rhyme<NoteLyric> lastRhymeConstraint = null;
		SegmentType currSegment = null;
		for (Integer measure: notesMap.keySet()) {
			final SortedMap<Integer, Note> notesMapForMeasure = notesMap.get(measure);
			boolean firstInMeasure = true;
			for (Integer offsetInDivs: notesMapForMeasure.keySet()) {
				double offsetInBeats = musicXML.divsToBeats(offsetInDivs, measure);
				
				// check essentially to make sure we didn't just add a phrase because of rhyme
				SortedMap<Double, SegmentType> currSegmentsByOffset = globalStructure.get(measure);
				if (currSegmentsByOffset != null && currSegmentsByOffset.containsKey(offsetInBeats)) {
					currSegment = currSegmentsByOffset.get(offsetInBeats);
					if(measure - lyricSequenceFirstMeasure >= MIN_FULL_MSRS_LYR_PHRASE) {
						newSegmentStartNotAfterRhyme = true;
					}
				}
				
				Note note = notesMapForMeasure.get(offsetInDivs);
				boolean isLyricOnset = (note != null && note.getLyric(currSegment != null && currSegment.mustHaveDifferentLyricsOnRepeats()) != null && note.pitch != Note.REST && note.tie != NoteTie.STOP);
				// if this is a lyric and it's is rhyme 
				Rhyme<NoteLyric> rhymeConstraint = getRhymeConstraint(measure, offsetInBeats, constraintsByNote);
				if (isLyricOnset && rhymeConstraint != null) {
					currLyrNoteRhymeConstrained = true;
				} else { // otherwise it's not a rhyming lyric
					currLyrNoteRhymeConstrained = false;
				}
				
				// if looking for new phrase (multiple rhyming syllables should extend the previous phrase)
				if (newSegmentStartNotAfterRhyme || prevLyrNoteRhymeConstrained && !currLyrNoteRhymeConstrained) {
					// if it's a potential starting point for a new phrase
					if (isLyricOnset) {
						lyricSequenceFirstMeasure = firstInMeasure ? measure : measure+1;
						phraseBeginnings.put(measure, offsetInBeats);
						newSegmentStartNotAfterRhyme = false;
						
						if(prevLyrNoteRhymeConstrained && !currLyrNoteRhymeConstrained) {
							lastRhymeConstraint.markPhraseEndingRhymeConstraint(true);
						}
					}
				}
	
				// only keep track of prev rhyme constraint if it was actually a lyric
				if (isLyricOnset) {
					prevLyrNoteRhymeConstrained = currLyrNoteRhymeConstrained;
					lastRhymeConstraint = rhymeConstraint;
				}
				firstInMeasure = false;
			}
		}
		musicXML.phraseBeginnings = phraseBeginnings;
	}
	
	private static Rhyme<NoteLyric> getRhymeConstraint(int measure, double offsetInBeats, SortedMap<Integer, SortedMap<Double, List<Constraint<NoteLyric>>>> constraintsByNote) {
		SortedMap<Double, List<Constraint<NoteLyric>>> constraintsByMeasure = constraintsByNote.get(measure);
		if (constraintsByMeasure != null) {
			List<Constraint<NoteLyric>> constraintsByOffset = constraintsByMeasure.get(offsetInBeats);
			if (constraintsByOffset != null) {
				for (Constraint<NoteLyric> constraint : constraintsByOffset) {
					ConstraintCondition<NoteLyric> condition = constraint.getCondition();
					if (condition instanceof Rhyme) {
						return (Rhyme<NoteLyric>) condition;
					}
				}
			}
		}
		
		return null;
	}
	
	private static void loadStructureFromFile(ParsedMusicXMLObject musicXML) {
		SortedMap<Integer, Triple<SegmentType,Integer,Double>> structure = new TreeMap<Integer, Triple<SegmentType,Integer,Double>>();
		SortedMap<Integer, SortedMap<Double, List<Constraint<NoteLyric>>>> constraints = new TreeMap<Integer, SortedMap<Double, List<Constraint<NoteLyric>>>>();
		
		List<List<Integer>> absoluteToPlayedMeasureNumbersMap = musicXML.absoluteToPlayedMeasureNumbersMap;
		
		// First load contents of file
		Scanner scan;
		String filename = musicXML.filename.replaceFirst("xml(\\.[\\d])?", "txt");
		try {
			scan = new Scanner(new File(STRUCTURE_ANNOTATIONS_DIR + "/" + filename));
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
		
		SortedMap<Integer, SortedMap<Integer, Note>> notesMap = musicXML.getNotesByPlayedMeasureAsMap();
		String nextLine;
		int lineNum = 0;
		SegmentType currType = null;
		Class conditionClass = null;
		Pair<Integer, Note> offsetNote;
		while(scan.hasNextLine()) {
			nextLine = scan.nextLine();
			lineNum++;
			if (nextLine.startsWith("//") || nextLine.trim().isEmpty()) {
				continue;
			}
			String[] tokens = nextLine.split("\t");
			try {
				currType = SegmentType.valueOf(tokens[0]);
				try {
					int segmentStartMeasure = Integer.parseInt(tokens[1]) - 1;
					int segmentStartMeasureRepeat = Integer.parseInt(tokens[2]) - 1;
					segmentStartMeasure = absoluteToPlayedMeasureNumbersMap.get(segmentStartMeasure).get(segmentStartMeasureRepeat);
					Double segmentStartOffset;
					try {
						segmentStartOffset = Double.parseDouble(tokens[3]);
					} catch (NumberFormatException e) {
						offsetNote = null;
						try {
							Integer occurrences = null;
							if (tokens.length > 5) { // optional sixth token to denote occurrence if particular lyric token appears multiple times within specified measure
								occurrences = Integer.parseInt(tokens[5]) - 1;
							}
							offsetNote = findNoteInMeasureWithLyric(notesMap.get(segmentStartMeasure), tokens[3], currType.mustHaveDifferentLyricsOnRepeats(), occurrences);
						} catch (Exception ex) {
							System.err.println("AT LINE " + lineNum + ": In measure " + segmentStartMeasure);
							throw new RuntimeException(ex);
						}
						segmentStartOffset = musicXML.divsToBeats(offsetNote.getFirst(), segmentStartMeasure);
					}
					
					int deltaFromFormStart = Integer.parseInt(tokens[4]);
					assert structure.isEmpty() || segmentStartMeasure > structure.lastKey(): "Global Structure annotation should be in order by measure number where segment starts occur";
					structure.put(segmentStartMeasure - deltaFromFormStart, new Triple<SegmentType, Integer, Double>(currType, deltaFromFormStart, segmentStartOffset));
				} catch (Exception ex) {
					System.err.println("AT LINE " + lineNum + ":");
					throw new RuntimeException(ex);
				}
			} catch (IllegalArgumentException e) {
				// parse constraint
				if (nextLine.startsWith("rh")) {
					conditionClass = Rhyme.class;
				} else if (nextLine.startsWith("ex")) {
					conditionClass = ExactBinaryMatch.class;
				} else {
					throw new RuntimeException("AT LINE " + lineNum + ": Improperly formatted constraint file. Expected new constraint or segment definition. Found: " + nextLine);
				}
				
				List<Triple<Integer,Double,Note>> constrainedNotes = new ArrayList<Triple<Integer, Double, Note>>();
				while(scan.hasNextLine()) {
					nextLine = scan.nextLine();
					lineNum++;
					if (nextLine.trim().length() == 0)
						break;
					tokens = nextLine.split("\t");
					int measure = Integer.parseInt(tokens[1]) - 1;
					try {
						int measureRepeat = Integer.parseInt(tokens[2]) - 1;
						measure = absoluteToPlayedMeasureNumbersMap.get(measure).get(measureRepeat);
					} catch (Exception ex) {
						System.err.println("AT LINE " + lineNum + ": In measure " + measure + " in " + filename);
						throw new RuntimeException(ex);
					}
					Integer occurrence = null;
					if (tokens.length > 3) { // optional fourth token to denote occurrence if particular lyric token appears multiple times within specified measure
						occurrence = Integer.parseInt(tokens[3]) - 1;
					}
					// look up token that is described in line
					try {
						offsetNote = findNoteInMeasureWithLyric(notesMap.get(measure), tokens[0], currType.mustHaveDifferentLyricsOnRepeats(), occurrence);
					} catch (Exception ex) {
						System.err.println("AT LINE " + lineNum + ": In measure " + measure + " in " + filename);
						throw new RuntimeException(ex);
					}
					double offsetInBeats = musicXML.divsToBeats(offsetNote.getFirst(), measure);
					// add it to constrained Notes
					assert (constrainedNotes.isEmpty() || constrainedNotes.get(constrainedNotes.size()-1).getFirst() < measure  ||
							constrainedNotes.get(constrainedNotes.size()-1).getFirst() == measure && constrainedNotes.get(constrainedNotes.size()-1).getSecond() <= offsetInBeats);
					constrainedNotes.add(new Triple<Integer, Double, Note>(measure, offsetInBeats, offsetNote.getSecond()));
				}

				// process constraint(s) given conditionClass and constrainedNotes
				List<Triple<Integer,Double,Constraint<NoteLyric>>> constraintsFromEntry = enumerateConstraintsFromEntry(conditionClass, constrainedNotes);
				
				// index the processed constraint(s) by the positions (msr,offset) that it constrains
				for (Triple<Integer, Double, Constraint<NoteLyric>> triple : constraintsFromEntry) {
					Integer measure = triple.getFirst();
					SortedMap<Double, List<Constraint<NoteLyric>>> constraintsForMeasure = constraints.get(measure);
					if (constraintsForMeasure == null) {
						constraintsForMeasure = new TreeMap<Double, List<Constraint<NoteLyric>>>();
						constraints.put(measure, constraintsForMeasure);
					}
					
					Double offsetInBeats = triple.getSecond();
					List<Constraint<NoteLyric>> constraintsForOffset = constraintsForMeasure.get(offsetInBeats);
					if (constraintsForOffset == null) {
						constraintsForOffset = new ArrayList<Constraint<NoteLyric>>();
						constraintsForMeasure.put(offsetInBeats, constraintsForOffset);
					}
					constraintsForOffset.add(triple.getThird());
				}
			}
		}
		
		musicXML.setGlobalStructure(structure);
		musicXML.segmentStructure = constraints;
	}
	
	private static List<Triple<Integer, Double, Constraint<NoteLyric>>> enumerateConstraintsFromEntry(Class conditionClass,
			List<Triple<Integer, Double, Note>> constrainedNotes) {
		
		List<Triple<Integer, Double, Constraint<NoteLyric>>> enumeratedConstraints = new ArrayList<Triple<Integer, Double, Constraint<NoteLyric>>>();
		
		if (conditionClass == Rhyme.class) {
			// if it's a rhyme, we place a constraint on all cases
			Triple<Integer,Double,Note> prevTriple = null;
			for (Triple<Integer,Double,Note> triple : constrainedNotes) {
				if (prevTriple == null) { // note that the first lyric is constrained to "rhyme with itself"; this signals that it is part of a rhyme, but that it should determine what the rhyme sound should be
					Constraint<NoteLyric> constraint = new Constraint<NoteLyric>(new Rhyme<NoteLyric>(triple.getFirst(), triple.getSecond()), true);
					enumeratedConstraints.add(new Triple<Integer, Double, Constraint<NoteLyric>>(triple.getFirst(), triple.getSecond(), constraint));
					prevTriple = triple;
				} else {
					Constraint<NoteLyric> constraint = new Constraint<NoteLyric>(new Rhyme<NoteLyric>(prevTriple.getFirst(), prevTriple.getSecond()), true);
					enumeratedConstraints.add(new Triple<Integer, Double, Constraint<NoteLyric>>(triple.getFirst(), triple.getSecond(), constraint));
				}
			}
		} else if (conditionClass == ExactBinaryMatch.class) {
			// if it's an exact match, we place a constraint on all of the positions
			assert constrainedNotes.size() == 2; // first is start, second is end (inclusive)
			for (Triple<Integer,Double,Note> triple : constrainedNotes) {
				Constraint<NoteLyric> constraint = new Constraint<NoteLyric>(new ExactBinaryMatch<NoteLyric>(ExactBinaryMatch.PREV_VERSE, ExactBinaryMatch.PREV_VERSE), true);
				enumeratedConstraints.add(new Triple<Integer, Double, Constraint<NoteLyric>>(triple.getFirst(), triple.getSecond(), constraint));
			}
		}
		
		return enumeratedConstraints;
	}

	public static Pair<Integer, Note> findNoteInMeasureWithLyric(SortedMap<Integer, Note> notesMap, String lyricToMatch, boolean requireLyricVerseMatchesRepeatCount, Integer idx) throws Exception {
		Pair<Integer, Note> match = null;
		
		int occurrences = 0;
		for (Integer divsOffset : notesMap.keySet()) {
			Note note = notesMap.get(divsOffset);
			NoteLyric lyric = note.getLyric(requireLyricVerseMatchesRepeatCount);
			if (lyric != null && lyric.text.equals(lyricToMatch)) {
				if (idx == null) {
					if (match != null) throw new Exception("Two matching lyrics for \"" + lyricToMatch +"\" at offsets " + match.getFirst() + " and " + divsOffset);
					match = new Pair<Integer, Note>(divsOffset, note);
				} else {
					if (occurrences == idx) {
						match = new Pair<Integer, Note>(divsOffset, note);
					}
					occurrences++;
				}
			}
		}

		if (match == null) {
			throw new Exception("No matching lyrics for \"" + lyricToMatch +"\" found in " + notesMap.values());
		}
		
		return match;
	}

	public static boolean annotationsExistForFile(File MusicXMLFile) {
		String filename = MusicXMLFile.getName().replaceFirst("\\.xml", ".txt");
		if (filename.equals(".DS_Store")) return false;
		File file =  new File(STRUCTURE_ANNOTATIONS_DIR + "/" + filename);
		return file.exists();
	}

}
