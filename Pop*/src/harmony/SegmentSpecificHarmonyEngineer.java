package harmony;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import composition.Score;
import data.MusicXMLModel;
import data.MusicXMLModelLearner;
import data.MusicXMLParser.Harmony;
import data.MusicXMLParser.NoteLyric;
import data.MusicXMLParser.Quality;
import data.MusicXMLParser.Syllabic;
import data.ParsedMusicXMLObject;
import inspiration.Inspiration;
import markov.AbstractMarkovModel;
import markov.SparseSingleOrderMarkovModel;

public class SegmentSpecificHarmonyEngineer extends HarmonyEngineer {

	public static class SegmentSpecificHarmonyEngineerMusicXMLModel extends MusicXMLModel {

		AbstractMarkovModel<Harmony> markovModel;
		Map<Harmony,Integer> statesByIndex = new HashMap<Harmony,Integer>();
		Map<Integer, Double> priorCounts = new HashMap<Integer, Double>();
		Map<Integer,Map<Integer,Double>> transitionCounts = new HashMap<Integer,Map<Integer, Double>>();
		
		@Override
		/**
		 * Only trains on examples with harmony 
		 */
		public void trainOnExample(ParsedMusicXMLObject musicXML) {
			// if doesn't have harmony, return
			if (musicXML.harmonyCount == 0) {
				return;
			}

			// the current markovModel is now obselete and will be regenerated when next sampled
			markovModel = null;
			
			Integer prevHarmIdx = -1;
			
			for (Entry<Integer, Map<Integer, Harmony>> measureEntry : musicXML.unoverlappingHarmonyByMeasure.entrySet()){
//				int measure = measureEntry.getKey();
				for (Entry<Integer, Harmony> offsetHarmony : measureEntry.getValue().entrySet()) {
//					int offset = offsetHarmony.getKey();
					Harmony harmony = offsetHarmony.getValue();
					if (harmony == null)
						continue;
					
					Integer harmIdx = statesByIndex.get(harmony);
					if (harmIdx == null) {
						harmIdx = statesByIndex.size();
						statesByIndex.put(harmony, harmIdx);
					}
					
					if (prevHarmIdx == -1) {
						Double count = priorCounts.get(harmIdx);
						priorCounts.put(harmIdx, count == null ? 1.0 : count + 1.0);
					} else {
						Map<Integer,Double> toMap = transitionCounts.get(prevHarmIdx);
						if (toMap == null) { // never even seen prevState
							toMap = new HashMap<Integer,Double>();
							toMap.put(harmIdx, 1.0);
							transitionCounts.put(prevHarmIdx, toMap);
						} else { // seen prev state
							Double count = toMap.get(harmIdx);
							toMap.put(harmIdx, count == null ? 1.0 : count + 1.0);
						}
					}
					
					prevHarmIdx = harmIdx;
				}
			}
			
		}
		
		public String toString() {
			StringBuilder str = new StringBuilder();
			
			// string representation of model
			
			return str.toString();
		}

		public List<Harmony> sampleHarmonySequenceOfLength(int length) {
			if (markovModel == null) {
				markovModel = buildModel();
			}
			return markovModel.generate(length);
		}

		private AbstractMarkovModel<Harmony> buildModel() {
			Map<Integer, Double> priors = new HashMap<Integer, Double>();
			Map<Integer,Map<Integer,Double>> transitions = new HashMap<Integer,Map<Integer, Double>>();
			
			
			
			return new SparseSingleOrderMarkovModel<>(statesByIndex, priors, transitions);
		}
	}
	
	private SegmentSpecificHarmonyEngineerMusicXMLModel model;
	
	public SegmentSpecificHarmonyEngineer() {
		this.model = (SegmentSpecificHarmonyEngineerMusicXMLModel) MusicXMLModelLearner.getTrainedModel(this.getClass());
	}
	@Override
	public void addHarmony(Inspiration inspiration, Score score) {
		List<Harmony> harmSeq = model.sampleHarmonySequenceOfLength(score.length());
		for (int i = 0; i < score.length(); i++) {
			score.addHarmony(i, 0.0, harmSeq.get(i));
		}		
	}

	// Legacy Code
//	private Map<SegmentType, Map<Integer, BackedDistribution<ConstraintBlock<Chord>>>> chordConstraintsDistribution = DataLoader.getChordConstraintsDistribution();
//	private Map<SegmentType, SparseSingleOrderMarkovModel<Chord>> mModel = DataLoader.getChordMarkovModel();
//	
//	@Override
//	protected void applyVariation(ProgressionSegment segmentProgression, Inspiration inspiration, SegmentStructure segmentSubstructures, SegmentType segmentType, boolean isLast){
//		// TODO Auto-generated method stub
//		
//	}
//
//	@Override
//	protected ProgressionSegment generateSegmentHarmony(Inspiration inspiration, SegmentStructure segmentSubstructures,
//			SegmentType segmentKey) {
//		
//		List<List<Chord>> chordLines = new ArrayList<List<Chord>>();
//		ConstraintBlock<Chord> constraintBlock = chordConstraintsDistribution.get(segmentKey).get(segmentSubstructures.linesPerSegment).sampleRandomly();
//		
//		int chordsPerLine;
//		for (int i = 0; i < segmentSubstructures.linesPerSegment; i++) {
//			List<Constraint<Chord>> constraints = segmentSubstructures.chordConstraints.getConstraintsForLine(i);
//			Constraint.reifyConstraints(constraints,chordLines);
//			chordsPerLine = constraintBlock.getLengthConstraint(i);
//			SparseNHMM<Chord> constrainedLyricModel = new SparseNHMM<Chord>(mModel.get(segmentKey), chordsPerLine, constraints);
//			chordLines.add(constrainedLyricModel.generate(chordsPerLine));
//		}
//		
//		return new ProgressionSegment(chordLines);
//	}
//

}
