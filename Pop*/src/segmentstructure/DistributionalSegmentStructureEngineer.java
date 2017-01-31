package segmentstructure;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Scanner;
import java.util.SortedMap;
import java.util.TreeMap;

import composition.Measure;
import condition.ConstraintCondition;
import condition.DelayedConstraintCondition;
import condition.ExactBinaryMatch;
import condition.ExactUnaryMatch;
import condition.Rhyme;
import constraint.Constraint;
import data.MusicXMLModel;
import data.MusicXMLModelLearner;
import data.MusicXMLParser.KeyMode;
import data.MusicXMLParser.Note;
import data.ParsedMusicXMLObject;
import globalstructure.SegmentType;
import lyrics.Lyric;
import utils.Pair;
import utils.Triple;
import utils.Utils;

public class DistributionalSegmentStructureEngineer extends SegmentStructureEngineer {

	public static class DistributionalSegmentStructureEngineerMusicXMLModel extends MusicXMLModel {

		private Map<SegmentType,Map<Integer,Integer>> measureCountDistribution = new EnumMap<SegmentType, Map<Integer,Integer>>(SegmentType.class);
		private Map<SegmentType, Integer> measureCountDistributionTotals = new EnumMap<SegmentType, Integer>(SegmentType.class); 
		private Map<Integer, List<List<Triple<Integer, Double, Constraint<Lyric>>>>> rhymeConstraintDistribution = new HashMap<Integer, List<List<Triple<Integer, Double, Constraint<Lyric>>>>>();
		private Random rand = new Random();
		
		public DistributionalSegmentStructureEngineerMusicXMLModel() {
			for(SegmentType segType: SegmentType.values()) {
				measureCountDistribution.put(segType, new HashMap<Integer, Integer>());
				measureCountDistributionTotals.put(segType, 0);
			}
		}
		
		@Override
		public void trainOnExample(ParsedMusicXMLObject musicXML) {
			
			// train measure count distribution
			int prevMsr = 0;
			SegmentType prevType = null;
			final SortedMap<Integer, SegmentType> globalStructure = musicXML.globalStructure;
			for(Integer msr : globalStructure.keySet()) {
				if (prevType != null) {
					Utils.incrementValueForKey(measureCountDistribution.get(prevType), (msr-prevMsr));
					measureCountDistributionTotals.put(prevType, measureCountDistributionTotals.get(prevType)+1);
				}
				
				prevType = globalStructure.get(msr);
				prevMsr = msr;
			}

			if (prevType != null) {
				Utils.incrementValueForKey(measureCountDistribution.get(prevType), (musicXML.getMeasureCount()-prevMsr));
				measureCountDistributionTotals.put(prevType, measureCountDistributionTotals.get(prevType)+1);
			}
			
			// train constraint distribution
			SegmentType currSegType,prevSegType = null;
			List<Triple<Integer, Double, Constraint<Lyric>>> currentSegmentRhymeConstraints = null;
			Map<Integer, SortedMap<Integer, Note>> notesMap = musicXML.getNotesByPlayedMeasureAsMap();
			Map<Integer, SortedMap<Double, List<Constraint<Lyric>>>> manuallyAnnotatedConstraints = loadAndInstantiateConstraints(musicXML, notesMap);
			
			int currentSegmentStartMeasure = 0;
			for (int measure = 0; measure < musicXML.getMeasureCount(); measure++) {
				currSegType = globalStructure.get(measure);
				int measureIdxInSegment = measure - currentSegmentStartMeasure;
				if (currSegType == null) {
					currSegType = prevSegType;
				} else {
					// new segment
					if (prevSegType != null) { // if not first segment, add previous one.
						int currSegmentLength = measureIdxInSegment;
						List<List<Triple<Integer, Double, Constraint<Lyric>>>> rhymeConstraintDistributionForSegLen = rhymeConstraintDistribution.get(currSegmentLength);
						if (rhymeConstraintDistributionForSegLen == null) {
							rhymeConstraintDistributionForSegLen = new ArrayList<List<Triple<Integer, Double, Constraint<Lyric>>>>();
							rhymeConstraintDistribution.put(currSegmentLength, rhymeConstraintDistributionForSegLen);
						}
						rhymeConstraintDistributionForSegLen.add(currentSegmentRhymeConstraints);
					}
					
					prevSegType = currSegType;
					currentSegmentStartMeasure = measure;
					currentSegmentRhymeConstraints = new ArrayList<Triple<Integer, Double, Constraint<Lyric>>>();
				}
				
				SortedMap<Integer, Note> notesForMeasure = notesMap.get(measure);
				if (notesForMeasure != null) { 
					for(Integer offsetInDivs: notesForMeasure.keySet()) {
						Map<Double, List<Constraint<Lyric>>> constraintsForMeasure = manuallyAnnotatedConstraints.get(measure);
						if (constraintsForMeasure != null) {
							double offsetInBeats = musicXML.divsToBeats(offsetInDivs,measure);
							List<Constraint<Lyric>> constraintsForOffset = constraintsForMeasure.get(offsetInBeats);
							if (constraintsForOffset != null) {
								for (Constraint<Lyric> constraint : constraintsForOffset) {
									Constraint<Lyric> modifiedConstraint = (Constraint<Lyric>) Utils.deepCopy(constraint);
									ConstraintCondition<Lyric> modifiedConstraintCondition = modifiedConstraint.getCondition();
									if (modifiedConstraintCondition instanceof DelayedConstraintCondition<?>) {
										// need to cast
										modifiedConstraintCondition.setReferenceMeasure(modifiedConstraintCondition.getReferenceMeasure());
									}
									currentSegmentRhymeConstraints.add(new Triple<Integer, Double, Constraint<Lyric>>(measureIdxInSegment, offsetInBeats, modifiedConstraint));
								}
							}
						}
					}
				}
			}
		}

		private Map<Integer, SortedMap<Double, List<Constraint<Lyric>>>> loadAndInstantiateConstraints(
				ParsedMusicXMLObject musicXML, Map<Integer, SortedMap<Integer, Note>> notesMap) {
			
			Map<Integer, SortedMap<Double, List<Constraint<Lyric>>>> constraints = new HashMap<Integer, SortedMap<Double, List<Constraint<Lyric>>>>();
			
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
				List<Triple<Integer,Double,Constraint<Lyric>>> constraintsFromEntry = enumerateConstraintsFromEntry(conditionClass, constrainedNotes);
				
				// index the processed constraint(s) by the positions (msr,offset) that it constrains
				for (Triple<Integer, Double, Constraint<Lyric>> triple : constraintsFromEntry) {
					Integer measure = triple.getFirst();
					SortedMap<Double, List<Constraint<Lyric>>> constraintsForMeasure = constraints.get(measure);
					if (constraintsForMeasure == null) {
						constraintsForMeasure = new TreeMap<Double, List<Constraint<Lyric>>>();
						constraints.put(measure, constraintsForMeasure);
					}
					
					Double offsetInBeats = triple.getSecond();
					List<Constraint<Lyric>> constraintsForOffset = constraintsForMeasure.get(offsetInBeats);
					if (constraintsForOffset == null) {
						constraintsForOffset = new ArrayList<Constraint<Lyric>>();
						constraintsForMeasure.put(offsetInBeats, constraintsForOffset);
					}
					constraintsForOffset.add(triple.getThird());
				}
			}
			
			return constraints;
		}

		private List<Triple<Integer, Double, Constraint<Lyric>>> enumerateConstraintsFromEntry(Class conditionClass,
				List<Triple<Integer, Double, Note>> constrainedNotes) {
			
			List<Triple<Integer, Double, Constraint<Lyric>>> enumeratedConstraints = new ArrayList<Triple<Integer, Double, Constraint<Lyric>>>();
			
			if (conditionClass == Rhyme.class) {
				// if it's a rhyme, we place a constraint on the latter cases (assuming a feed-forward generation)
				Triple<Integer,Double,Note> prevTriple = null;
				for (Triple<Integer,Double,Note> triple : constrainedNotes) {
					if (prevTriple != null) {
						Constraint<Lyric> constraint = new Constraint<Lyric>(new Rhyme<Lyric>(prevTriple.getFirst(), prevTriple.getSecond()), true);
						enumeratedConstraints.add(new Triple<Integer, Double, Constraint<Lyric>>(triple.getFirst(), triple.getSecond(), constraint));
					}
					prevTriple = triple;
				}
			} else if (conditionClass == ExactUnaryMatch.class) {
				// if it's an exact match, we place a constraint on all of the positions
				for (Triple<Integer,Double,Note> triple : constrainedNotes) {
					Constraint<Lyric> constraint = new Constraint<Lyric>(new ExactBinaryMatch<Lyric>(ExactBinaryMatch.PREV_VERSE, ExactBinaryMatch.PREV_VERSE), true);
					enumeratedConstraints.add(new Triple<Integer, Double, Constraint<Lyric>>(triple.getFirst(), triple.getSecond(), constraint));
				}
			}
			
			return enumeratedConstraints;
		}

		private Pair<Integer, Note> findNoteInMeasureWithLyric(SortedMap<Integer, Note> notesMap, String lyricToMatch) throws Exception {
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

		public int sampleMeasureCountForSegmentType(SegmentType segmentType) {
			int offsetIntoDistribution = rand.nextInt(measureCountDistributionTotals.get(segmentType));
			
			Map<Integer, Integer> distForType = measureCountDistribution.get(segmentType);
			
			int accum = 0;
			for (Entry<Integer, Integer> entry : distForType.entrySet()) {
				accum += entry.getValue();
				if (accum >= offsetIntoDistribution) {
					return entry.getKey();
				}
			}
			
			throw new RuntimeException("Should never reach here");
		}

		public List<Triple<Integer, Double, Constraint>> sampleConstraints(int measureCount) {
			List<Triple<Integer, Double, Constraint>> constraints = new ArrayList<Triple<Integer, Double, Constraint>>();
			
//			constraints.add(new Triple<Integer, Double, Constraint>(2, Constraint.ALL_POSITIONS, new Constraint<Harmony>(2345, new ExactBinaryMatch<Harmony>(0, Constraint.ALL_POSITIONS), true));

			return constraints;
		}
	}

	DistributionalSegmentStructureEngineerMusicXMLModel model;
	
	public DistributionalSegmentStructureEngineer() {
		model = (DistributionalSegmentStructureEngineerMusicXMLModel) MusicXMLModelLearner.getTrainedModel(this.getClass());
	}
	
	@Override
	public SegmentStructure defineSegmentStructure(SegmentType segmentType) {
		int measureCount = model.sampleMeasureCountForSegmentType(segmentType);
		
		SegmentStructure segmentStructure = new SegmentStructure(measureCount, segmentType);
		segmentStructure.addDivisionsPerQuarterNote(0,4);
		segmentStructure.addKey(0,0,KeyMode.MAJOR);
		segmentStructure.addTime(0,4,4);
		
		List<Triple<Integer, Double, Constraint>> constraints = model.sampleConstraints(measureCount);
		for (Triple<Integer, Double, Constraint> triple : constraints) {
			segmentStructure.addConstraint(triple.getFirst(), triple.getSecond(), triple.getThird());
		}
		
		return segmentStructure;
	}

	@Override
	public List<Measure> instantiateSegmentStructure(SegmentType segmentType, SegmentStructure segmentStructure,
			boolean lastOfKind, boolean lastSegment) {
		return instantiateExactSegmentStructure(segmentType, segmentStructure, lastOfKind, lastSegment);
	}
}
