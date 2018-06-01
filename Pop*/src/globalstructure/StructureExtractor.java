package globalstructure;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.SortedMap;
import java.util.TreeMap;

import condition.ConstraintCondition;
import condition.BinaryMatch;
import condition.Rhyme;
import constraint.Constraint;
import data.MusicXMLParser;
import data.MusicXMLParser.Harmony;
import data.MusicXMLParser.Note;
import data.MusicXMLParser.NoteLyric;
import data.MusicXMLParser.NoteTie;
import data.ParsedMusicXMLObject;
import utils.Pair;
import utils.Triple;

public class StructureExtractor {

	public static final String STRUCTURE_ANNOTATIONS_DIR = "wikifonia_structure_annotations";
	public static final String GENERALIZED_STRUCTURE_ANNOTATIONS_DIR = "wikifonia_generalized_structure_annotations";
	
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
		SortedMap<Integer, SortedMap<Double, List<Constraint<NoteLyric>>>> constraintsByNote = musicXML.segmentLyricStructure;
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
					conditionClass = BinaryMatch.class;
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
				List<Triple<Integer,Double,Constraint<NoteLyric>>> constraintsFromEntry = (List<Triple<Integer, Double, Constraint<NoteLyric>>>) enumerateConstraintsFromEntry(conditionClass, "lyric", constrainedNotes)[0];
				
				indexConstraints(constraints, constraintsFromEntry);
			}
		}
		
		musicXML.setGlobalStructure(structure);
		musicXML.segmentLyricStructure = constraints;
	}
	
	private static Object[] enumerateConstraintsFromEntry(Class conditionClass,
			String matchClass, List<Triple<Integer, Double, Note>> constrainedNotes) {
		
		List<Triple<Integer, Double, Constraint<NoteLyric>>> enumeratedLyricConstraints = new ArrayList<Triple<Integer, Double, Constraint<NoteLyric>>>();
		List<Triple<Integer, Double, Constraint<Integer>>> enumeratedPitchConstraints = new ArrayList<Triple<Integer, Double, Constraint<Integer>>>();
		List<Triple<Integer, Double, Constraint<Double>>> enumeratedRhythmConstraints = new ArrayList<Triple<Integer, Double, Constraint<Double>>>();
		List<Triple<Integer, Double, Constraint<Harmony>>> enumeratedHarmonyConstraints = new ArrayList<Triple<Integer, Double, Constraint<Harmony>>>();
		
		if (conditionClass == Rhyme.class) {
			// if it's a rhyme, we place a constraint on all cases
			Triple<Integer,Double,Note> prevTriple = null;
			for (Triple<Integer,Double,Note> triple : constrainedNotes) {
				if (prevTriple == null) { // note that the first lyric is constrained to "rhyme with itself"; this signals that it is part of a rhyme, but that it should determine what the rhyme sound should be
					Constraint<NoteLyric> constraint = new Constraint<NoteLyric>(new Rhyme<NoteLyric>(triple.getFirst(), triple.getSecond()), true);
					enumeratedLyricConstraints.add(new Triple<Integer, Double, Constraint<NoteLyric>>(triple.getFirst(), triple.getSecond(), constraint));
					prevTriple = triple;
				} else {
					Constraint<NoteLyric> constraint = new Constraint<NoteLyric>(new Rhyme<NoteLyric>(prevTriple.getFirst(), prevTriple.getSecond()), true);
					enumeratedLyricConstraints.add(new Triple<Integer, Double, Constraint<NoteLyric>>(triple.getFirst(), triple.getSecond(), constraint));
				}
			}
		} else if (conditionClass == BinaryMatch.class) {
			// if it's an exact match, we place a constraint on all of the positions
			for (Triple<Integer,Double,Note> triple : constrainedNotes) {
				if (matchClass.equals("lyric")) {
					Constraint<NoteLyric> constraint = new Constraint<NoteLyric>(new BinaryMatch<NoteLyric>(BinaryMatch.PREV_VERSE, BinaryMatch.PREV_VERSE), true);
					enumeratedLyricConstraints.add(new Triple<Integer, Double, Constraint<NoteLyric>>(triple.getFirst(), triple.getSecond(), constraint));
				} else if (matchClass.equals("pitch")) {
					Constraint<Integer> constraint = new Constraint<Integer>(new BinaryMatch<Integer>(BinaryMatch.PREV_VERSE, BinaryMatch.PREV_VERSE), true);
					enumeratedPitchConstraints.add(new Triple<Integer, Double, Constraint<Integer>>(triple.getFirst(), triple.getSecond(), constraint));
				} else if (matchClass.equals("rhythm")) {
					Constraint<Double> constraint = new Constraint<Double>(new BinaryMatch<Double>(BinaryMatch.PREV_VERSE, BinaryMatch.PREV_VERSE), true);
					enumeratedRhythmConstraints.add(new Triple<Integer, Double, Constraint<Double>>(triple.getFirst(), triple.getSecond(), constraint));
				} else if (matchClass.equals("chord")) {
					Constraint<Harmony> constraint = new Constraint<Harmony>(new BinaryMatch<Harmony>(BinaryMatch.PREV_VERSE, BinaryMatch.PREV_VERSE), true);
					enumeratedHarmonyConstraints.add(new Triple<Integer, Double, Constraint<Harmony>>(triple.getFirst(), triple.getSecond(), constraint));
				}
			}
		}
		
		return new Object[]{enumeratedLyricConstraints,enumeratedPitchConstraints,enumeratedRhythmConstraints, enumeratedHarmonyConstraints};
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
			List<String> vals = new ArrayList<String>();
			for (Note note : notesMap.values()) {
				final NoteLyric lyric = note.getLyric(requireLyricVerseMatchesRepeatCount);
				if (lyric!=null)
					vals.add(lyric.text);
			}
			System.err.println("No matching lyrics for \"" + lyricToMatch +"\" found in " + vals);
			throw new Exception("No matching lyrics for \"" + lyricToMatch +"\" found in " + vals);
		}
		
		return match;
	}

	public static boolean annotationsExistForFile(File MusicXMLFile) {
		String filename = MusicXMLFile.getName().replaceFirst("\\.xml", ".txt");
		if (filename.equals(".DS_Store")) return false;
		File file =  new File(STRUCTURE_ANNOTATIONS_DIR + "/" + filename);
		return file.exists();
	}
	
	public static boolean generalizedAnnotationsExistForFile(File MusicXMLFile) {
		String filename = MusicXMLFile.getName().replaceFirst("\\.xml", ".txt");
		if (filename.equals(".DS_Store")) return false;
		File file =  new File(GENERALIZED_STRUCTURE_ANNOTATIONS_DIR + "/" + filename);
		return file.exists();
	}

	@SuppressWarnings("unchecked")
	public static void loadGeneralizedStructureAnnotations(ParsedMusicXMLObject musicXML) {
		SortedMap<Integer, Triple<SegmentType,Integer,Double>> structure = new TreeMap<Integer, Triple<SegmentType,Integer,Double>>();
		SortedMap<Integer, SortedMap<Double, List<Constraint<NoteLyric>>>> lyricConstraints = new TreeMap<Integer, SortedMap<Double, List<Constraint<NoteLyric>>>>();
		SortedMap<Integer, SortedMap<Double, List<Constraint<Integer>>>> pitchConstraints = new TreeMap<Integer, SortedMap<Double, List<Constraint<Integer>>>>();
		SortedMap<Integer, SortedMap<Double, List<Constraint<Double>>>> rhythmConstraints = new TreeMap<Integer, SortedMap<Double, List<Constraint<Double>>>>();
		SortedMap<Integer, SortedMap<Double, List<Constraint<Harmony>>>> harmonyConstraints = new TreeMap<Integer, SortedMap<Double, List<Constraint<Harmony>>>>();
		
		// for each group of matching lyric positions (indexed by a letter, e.g., group 'A')
		// you have each of the matching regions, with starting (inclusive) and ending (inclusive) measure and beat position of matching notes 
		Map<Character, List<Pair<Integer, Double>>> rhymeMatches = new HashMap<Character, List<Pair<Integer, Double>>>();
		Map<Character, List<Pair<Pair<Integer, Double>, Pair<Integer, Double>>>> lyricMatches = new HashMap<Character, List<Pair<Pair<Integer,Double>,Pair<Integer,Double>>>>();
		Map<Character, List<Pair<Pair<Integer, Double>, Pair<Integer, Double>>>> pitchMatches = new HashMap<Character, List<Pair<Pair<Integer,Double>,Pair<Integer,Double>>>>();		
		Map<Character, List<Pair<Pair<Integer, Double>, Pair<Integer, Double>>>> rhythmMatches = new HashMap<Character, List<Pair<Pair<Integer,Double>,Pair<Integer,Double>>>>();		
		Map<Character, List<Pair<Pair<Integer, Double>, Pair<Integer, Double>>>> harmonyMatches = new HashMap<Character, List<Pair<Pair<Integer,Double>,Pair<Integer,Double>>>>();
		Map<Character, List<Pair<Pair<Integer, Double>, Pair<Integer, Double>>>> chorusMatches = new HashMap<Character, List<Pair<Pair<Integer,Double>,Pair<Integer,Double>>>>();
		Map<Character, List<Pair<Pair<Integer, Double>, Pair<Integer, Double>>>> verseMatches = new HashMap<Character, List<Pair<Pair<Integer,Double>,Pair<Integer,Double>>>>();
		Map<Character, List<Pair<Pair<Integer, Double>, Pair<Integer, Double>>>> matches;
		List<Pair<Pair<Integer, Double>, Pair<Integer, Double>>> matchesForGroup;
		
		char rhymeGroupLabel = 'A';
		
		List<List<Integer>> absoluteToPlayedMeasureNumbersMap = musicXML.absoluteToPlayedMeasureNumbersMap;
		
		// First load contents of file
		Scanner scan;
		String filename = musicXML.filename.replaceFirst("xml(\\.[\\d])?", "txt");
		try {
			scan = new Scanner(new File(GENERALIZED_STRUCTURE_ANNOTATIONS_DIR + "/" + filename));
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
		
		SortedMap<Integer, SortedMap<Integer, Note>> notesMap = musicXML.getNotesByPlayedMeasureAsMap();
		String nextLine;
		int lineNum = 0;
		SegmentType currType = null;
		SegmentType prevType = null;
		Class conditionClass = null;
		String matchClass = null;
		Character matchGroup;
		Pair<Integer, Note> offsetNote;
		char segmentMatchGroup = '\0';
		char prevSegmentMatchGroup = '\0';
		
		while(scan.hasNextLine()) {
			nextLine = scan.nextLine();
			lineNum++;
			if (nextLine.startsWith("//") || nextLine.trim().isEmpty()) {
				continue;
			}
			String[] tokens = nextLine.split("\t");
			try {
				prevType = currType;
				currType = SegmentType.valueOf(tokens[0]);
				try {
					prevSegmentMatchGroup = segmentMatchGroup;
					segmentMatchGroup = tokens[1].charAt(0);
					int segmentStartMeasure = Integer.parseInt(tokens[2]) - 1;
					int segmentStartMeasureRepeat = Integer.parseInt(tokens[3]) - 1;
					segmentStartMeasure = absoluteToPlayedMeasureNumbersMap.get(segmentStartMeasure).get(segmentStartMeasureRepeat);
					Double segmentStartOffset;
					try {
						segmentStartOffset = Double.parseDouble(tokens[4]);
					} catch (NumberFormatException e) {
						offsetNote = null;
						try {
							Integer occurrences = null;
							if (tokens.length > 6) { // optional sixth token to denote occurrence if particular lyric token appears multiple times within specified measure
								occurrences = Integer.parseInt(tokens[6]) - 1;
							}
							offsetNote = findNoteInMeasureWithLyric(notesMap.get(segmentStartMeasure), tokens[4], currType.mustHaveDifferentLyricsOnRepeats(), occurrences);
						} catch (Exception ex) {
							System.err.println("AT LINE " + lineNum + ": In measure " + segmentStartMeasure + " in " + filename);
							throw new RuntimeException(ex);
						}
						segmentStartOffset = musicXML.divsToBeats(offsetNote.getFirst(), segmentStartMeasure);
					}
					
					int deltaFromFormStart = Integer.parseInt(tokens[5]);
					assert structure.isEmpty() || segmentStartMeasure > structure.lastKey(): "Global Structure annotation should be in order by measure number where segment starts occur";
					structure.put(segmentStartMeasure - deltaFromFormStart, new Triple<SegmentType, Integer, Double>(currType, deltaFromFormStart, 0.0));
					
					if (prevType == SegmentType.CHORUS) {
						matchesForGroup = chorusMatches.get(prevSegmentMatchGroup);
						matchesForGroup.get(matchesForGroup.size()-1).setSecond(new Pair<Integer,Double>(segmentStartMeasure - deltaFromFormStart,0.0));
					} else if (prevType == SegmentType.VERSE) {
						matchesForGroup = verseMatches.get(prevSegmentMatchGroup);
						matchesForGroup.get(matchesForGroup.size()-1).setSecond(new Pair<Integer,Double>(segmentStartMeasure - deltaFromFormStart,0.0));
					}
					if (currType == SegmentType.CHORUS) {
						matchesForGroup = chorusMatches.get(segmentMatchGroup);
						if (matchesForGroup == null) {
							matchesForGroup = new ArrayList<Pair<Pair<Integer, Double>, Pair<Integer, Double>>>();
							chorusMatches.put(segmentMatchGroup, matchesForGroup);
						}
						matchesForGroup.add(new Pair<Pair<Integer,Double>, Pair<Integer,Double>>(new Pair<Integer,Double>(segmentStartMeasure - deltaFromFormStart,0.0), null));
					} else if (currType == SegmentType.VERSE) {
						matchesForGroup = verseMatches.get(segmentMatchGroup);
						if (matchesForGroup == null) {
							matchesForGroup = new ArrayList<Pair<Pair<Integer, Double>, Pair<Integer, Double>>>();
							verseMatches.put(segmentMatchGroup, matchesForGroup);
						}
						matchesForGroup.add(new Pair<Pair<Integer,Double>, Pair<Integer,Double>>(new Pair<Integer,Double>(segmentStartMeasure - deltaFromFormStart,0.0), null));
					}
					
				} catch (Exception ex) {
					System.err.println("AT LINE " + lineNum + ":");
					throw new RuntimeException(ex);
				}
			} catch (IllegalArgumentException e) {
				// parse constraint
				if (nextLine.startsWith("rh")) {
					conditionClass = Rhyme.class;
					matchClass = "rhyme";
					matchGroup = '\0';
				} else if (nextLine.startsWith("ma")) {
					conditionClass = BinaryMatch.class;
					tokens = nextLine.split("\t");
					matchClass = tokens[1];
					matchGroup = tokens[2].charAt(0);
				} else {
					throw new RuntimeException("AT LINE " + lineNum + ": Improperly formatted constraint file. Expected new constraint or segment definition. Found: " + nextLine);
				}
				
				List<Triple<Integer,Double,Note>> constrainedNotes = new ArrayList<Triple<Integer, Double, Note>>();
				while(scan.hasNextLine()) {
					nextLine = scan.nextLine();
					lineNum++;
					if (nextLine.trim().length() == 0 || nextLine.startsWith("END"))
						break;
					tokens = nextLine.split("\t");
					int measure = -1;
					try {
						measure = Integer.parseInt(tokens[1]) - 1;
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
					
					Double offsetInBeats;
					try {
						offsetInBeats = Double.parseDouble(tokens[0]);
						int offsetInDivs = musicXML.beatsToDivs(offsetInBeats, measure);
						offsetNote = new Pair<Integer, Note>(offsetInDivs,notesMap.get(measure).get(offsetInDivs));
					} catch (NumberFormatException f) {
						offsetNote = null;
						try {
							offsetNote = findNoteInMeasureWithLyric(notesMap.get(measure), tokens[0], currType.mustHaveDifferentLyricsOnRepeats(), occurrence);
						} catch (Exception ex) {
							System.err.println("AT LINE " + lineNum + ": In measure " + measure + " in " + filename);
							throw new RuntimeException(ex);
						}
						offsetInBeats = musicXML.divsToBeats(offsetNote.getFirst(), measure);
					}
					
					// add it to constrained Notes
					assert (constrainedNotes.isEmpty() || constrainedNotes.get(constrainedNotes.size()-1).getFirst() < measure  ||
							constrainedNotes.get(constrainedNotes.size()-1).getFirst() == measure && constrainedNotes.get(constrainedNotes.size()-1).getSecond() <= offsetInBeats);
					constrainedNotes.add(new Triple<Integer, Double, Note>(measure, offsetInBeats, offsetNote.getSecond()));
				}

				// process constraint(s) given conditionClass and constrainedNotes
				Object[] constraintsFromEntry = enumerateConstraintsFromEntry(conditionClass, matchClass, constrainedNotes);
				
				indexConstraints(lyricConstraints, (List<Triple<Integer, Double, Constraint<NoteLyric>>>) constraintsFromEntry[0]);
				indexConstraints(pitchConstraints, (List<Triple<Integer, Double, Constraint<Integer>>>) constraintsFromEntry[1]);
				indexConstraints(rhythmConstraints, (List<Triple<Integer, Double, Constraint<Double>>>) constraintsFromEntry[2]);
				indexConstraints(harmonyConstraints, (List<Triple<Integer, Double, Constraint<Harmony>>>) constraintsFromEntry[3]);
				
				// process constraints for match map
				if (matchClass.equals("rhyme")) {
					List<Pair<Integer, Double>> rhymeGroup = new ArrayList<Pair<Integer, Double>>();
					for(Triple<Integer, Double, Note> notePosition : constrainedNotes) {
						rhymeGroup.add(new Pair<Integer, Double>(notePosition.getFirst(), notePosition.getSecond()));
					}
					rhymeMatches.put(rhymeGroupLabel++, rhymeGroup);
				} else {
					if (matchClass.equals("lyric")) {
						matches = lyricMatches;
					} else if (matchClass.equals("pitch")) {
						matches = pitchMatches;
					} else if (matchClass.equals("rhythm")) {
						matches = rhythmMatches;
					} else if (matchClass.equals("chord")) {
						matches = harmonyMatches;
					} else {
						throw new RuntimeException("Unknown constraint type: " + matchClass);
					}
					matchesForGroup = matches.get(matchGroup);
					if (matchesForGroup == null) {
						matchesForGroup = new ArrayList<Pair<Pair<Integer, Double>, Pair<Integer, Double>>>();
						matches.put(matchGroup, matchesForGroup);
					}
					Pair<Integer, Double> startPosition = new Pair<Integer,Double>(constrainedNotes.get(0).getFirst(), constrainedNotes.get(0).getSecond());
					Pair<Integer, Double> endPosition = constrainedNotes.size() == 1 ? new Pair<Integer,Double>(musicXML.getMeasureCount(), 0.0) : new Pair<Integer,Double>(constrainedNotes.get(1).getFirst(), constrainedNotes.get(1).getSecond());
					matchesForGroup.add(new Pair<Pair<Integer, Double>, Pair<Integer, Double>>(startPosition, endPosition));
				}
			}
		}
		
		if (currType == SegmentType.CHORUS) {
			matchesForGroup = chorusMatches.get(segmentMatchGroup);
			matchesForGroup.get(matchesForGroup.size()-1).setSecond(new Pair<Integer,Double>(musicXML.getMeasureCount(),0.0));
		} else if (currType == SegmentType.VERSE) {
			matchesForGroup = verseMatches.get(segmentMatchGroup);
			matchesForGroup.get(matchesForGroup.size()-1).setSecond(new Pair<Integer,Double>(musicXML.getMeasureCount(),0.0));
		}
		
		musicXML.setGlobalStructure(structure);
		musicXML.segmentLyricStructure = lyricConstraints;		
		// This really isn't doing much here, but may someday be useful 
		musicXML.segmentPitchStructure = pitchConstraints;		
		musicXML.segmentRhythmStructure = rhythmConstraints;		
		musicXML.segmentHarmonyStructure = harmonyConstraints;
		
		// this is being used.
		musicXML.setMatches(rhymeMatches, lyricMatches, pitchMatches, rhythmMatches, harmonyMatches, chorusMatches, verseMatches);
	}

	private static <T> void indexConstraints(SortedMap<Integer, SortedMap<Double, List<Constraint<T>>>> constraints,
			List<Triple<Integer, Double, Constraint<T>>> constraintsFromEntry) {
		// index the processed constraint(s) by the positions (msr,offset) that it constrains
		for (Triple<Integer, Double, Constraint<T>> triple : constraintsFromEntry) {
			Integer measure = triple.getFirst();
			SortedMap<Double, List<Constraint<T>>> constraintsForMeasure = constraints.get(measure);
			if (constraintsForMeasure == null) {
				constraintsForMeasure = new TreeMap<Double, List<Constraint<T>>>();
				constraints.put(measure, constraintsForMeasure);
			}
			
			Double offsetInBeats = triple.getSecond();
			List<Constraint<T>> constraintsForOffset = constraintsForMeasure.get(offsetInBeats);
			if (constraintsForOffset == null) {
				constraintsForOffset = new ArrayList<Constraint<T>>();
				constraintsForMeasure.put(offsetInBeats, constraintsForOffset);
			}
			constraintsForOffset.add(triple.getThird());
		}
	}

}
