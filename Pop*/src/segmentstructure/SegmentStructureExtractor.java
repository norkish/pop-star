package segmentstructure;

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
import condition.DelayedConstraintCondition;
import condition.ExactBinaryMatch;
import condition.ExactUnaryMatch;
import condition.Rhyme;
import constraint.Constraint;
import data.ParsedMusicXMLObject;
import data.MusicXMLParser.Note;
import data.MusicXMLParser.NoteLyric;
import globalstructure.SegmentType;
import lyrics.Lyric;
import utils.Pair;
import utils.Triple;
import utils.Utils;

public class SegmentStructureExtractor {

	public static void annotateSegmentStructure(ParsedMusicXMLObject musicXML) {
		SortedMap<Integer, SortedMap<Integer, Note>> notesMap = musicXML.getNotesByPlayedMeasureAsMap();
		musicXML.segmentStructure = loadAndInstantiateConstraints(musicXML, notesMap);
		musicXML.phraseBeginnings = annotatePhraseBeginnings(musicXML, notesMap);
	}
	
	
	final private static int MIN_FULL_MSRS_LYR_PHRASE = 2;

	private static SortedMap<Integer,Double> annotatePhraseBeginnings(ParsedMusicXMLObject musicXML,
			SortedMap<Integer, SortedMap<Integer, Note>> notesMap) {
		
		SortedMap<Integer,Double> phraseBeginnings = new TreeMap<Integer, Double>();
		
		SortedMap<Integer, SegmentType> globalStructure = musicXML.globalStructure;
		SortedMap<Integer, SortedMap<Double, List<Constraint<NoteLyric>>>> constraintsByNote = musicXML.segmentStructure;
		List<Triple<Integer, Integer, Note>> notes = musicXML.notesByPlayedMeasure;
		int lyricSequenceFirstMeasure = 0;
		
		boolean searchingForNewPhrase = true;
		for (Integer measure: notesMap.keySet()) {
			final SortedMap<Integer, Note> notesMapForMeasure = notesMap.get(measure);
			boolean firstInMeasure = true;
			for (Integer offsetInDivs: notesMapForMeasure.keySet()) {
				double offsetInBeats = musicXML.divsToBeats(offsetInDivs, measure);
				
				// check essentially to make sure we didn't just add a phrase because of rhyme
				if (globalStructure.containsKey(measure) && (measure - lyricSequenceFirstMeasure >= MIN_FULL_MSRS_LYR_PHRASE)) {
					searchingForNewPhrase = true;
				}
				
				if (searchingForNewPhrase) {
					Note note = notesMapForMeasure.get(offsetInDivs);
					if (note != null && note.pitch != Note.REST) {
						lyricSequenceFirstMeasure = firstInMeasure ? measure : measure+1;
						phraseBeginnings.put(measure, offsetInBeats);
						searchingForNewPhrase = false;
					}
				}
	
				// if the note has rhyme constraints or (is the beginning of a new segment && didn't just end because of rhyme constraint), that's a phrase boundary
				if (isPhraseBoundaryByRhyme(measure, offsetInBeats, constraintsByNote)) {
					searchingForNewPhrase = true;
				}
				firstInMeasure = false;
			}
		}

		return phraseBeginnings;
	}
	
	private static boolean isPhraseBoundaryByRhyme(int measure, double offsetInBeats, SortedMap<Integer, SortedMap<Double, List<Constraint<NoteLyric>>>> constraintsByNote) {
		SortedMap<Double, List<Constraint<NoteLyric>>> constraintsByMeasure = constraintsByNote.get(measure);
		if (constraintsByMeasure != null) {
			List<Constraint<NoteLyric>> constraintsByOffset = constraintsByMeasure.get(offsetInBeats);
			if (constraintsByOffset != null) {
				for (Constraint<NoteLyric> constraint : constraintsByOffset) {
					if (constraint.getCondition() instanceof Rhyme) {
						return true;
					}
				}
			}
		}
		
		return false;
	}

	private static SortedMap<Integer, SortedMap<Double, List<Constraint<NoteLyric>>>> loadAndInstantiateConstraints(
			ParsedMusicXMLObject musicXML, Map<Integer, SortedMap<Integer, Note>> notesMap) {
		
		SortedMap<Integer, SortedMap<Double, List<Constraint<NoteLyric>>>> constraints = new TreeMap<Integer, SortedMap<Double, List<Constraint<NoteLyric>>>>();
		
		// First load contents of file
		Scanner scan;
		String filename = musicXML.filename.replaceFirst("mxl(\\.[\\d])?", "txt");
		try {
			scan = new Scanner(new File(Constraint.CONSTRAINT_ANNOTATIONS_DIR + "/" + filename));
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
		
		String nextLine;
		Class conditionClass = null;
		while(scan.hasNextLine()) {
			nextLine = scan.nextLine();
			if (nextLine.startsWith("rh")) {
				conditionClass = Rhyme.class;
			} else if (nextLine.startsWith("ex")) {
				conditionClass = ExactUnaryMatch.class;
			} else {
				throw new RuntimeException("Improperly formatted constraint file. Expected new constraint definition. Found: " + nextLine);
			}
			
			List<Triple<Integer,Double,Note>> constrainedNotes = new ArrayList<Triple<Integer, Double, Note>>();
			Pair<Integer, Note> offsetNote;
			nextLine = scan.nextLine();
			do {
				String[] tokens = nextLine.split("\t");
				int measure = Integer.parseInt(tokens[1]) - 1;
				// look up token that is described in line
				try {
					offsetNote = findNoteInMeasureWithLyric(notesMap.get(measure), tokens[0]);
				} catch (Exception e) {
					throw new RuntimeException(e.getMessage() + " in measure " + measure + " in " + filename);
				}
				// add it to constrained Notes
				double offsetInBeats = musicXML.divsToBeats(offsetNote.getFirst(), measure);
				assert (constrainedNotes.isEmpty() || constrainedNotes.get(constrainedNotes.size()-1).getFirst() < measure  ||
						constrainedNotes.get(constrainedNotes.size()-1).getFirst() == measure && constrainedNotes.get(constrainedNotes.size()-1).getSecond() <= offsetInBeats);
				constrainedNotes.add(new Triple<Integer, Double, Note>(measure, offsetInBeats, offsetNote.getSecond()));
				nextLine = scan.nextLine();
			} while(nextLine.trim().length() > 0 && scan.hasNextLine());
			
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
		
		return constraints;
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
				} else {
					Constraint<NoteLyric> constraint = new Constraint<NoteLyric>(new Rhyme<NoteLyric>(prevTriple.getFirst(), prevTriple.getSecond()), true);
					enumeratedConstraints.add(new Triple<Integer, Double, Constraint<NoteLyric>>(triple.getFirst(), triple.getSecond(), constraint));
				}
				prevTriple = triple;
			}
		} else if (conditionClass == ExactUnaryMatch.class) {
			// if it's an exact match, we place a constraint on all of the positions
			for (Triple<Integer,Double,Note> triple : constrainedNotes) {
				Constraint<NoteLyric> constraint = new Constraint<NoteLyric>(new ExactBinaryMatch<NoteLyric>(ExactBinaryMatch.PREV_VERSE, ExactBinaryMatch.PREV_VERSE), true);
				enumeratedConstraints.add(new Triple<Integer, Double, Constraint<NoteLyric>>(triple.getFirst(), triple.getSecond(), constraint));
			}
		}
		
		return enumeratedConstraints;
	}

	private static Pair<Integer, Note> findNoteInMeasureWithLyric(SortedMap<Integer, Note> notesMap, String lyricToMatch) throws Exception {
		Pair<Integer, Note> match = null;
		
		for (Integer divsOffset : notesMap.keySet()) {
			Note note = notesMap.get(divsOffset);
			if (note.lyric != null && note.lyric.text.equals(lyricToMatch)) {
				if (match != null) throw new Exception("Two matching lyrics for \"" + lyricToMatch +"\" at offsets " + match.getFirst() + " and " + divsOffset);
				match = new Pair<Integer, Note>(divsOffset, note);
			}
		}

		if (match == null) {
			throw new Exception("No matching lyrics for \"" + lyricToMatch +"\" found");
		}
		
		return match;
	}


}
