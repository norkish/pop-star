package globalstructure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import data.BackedDistribution;
import data.MusicXMLModel;
import data.MusicXMLModelLearner;
import data.ParsedMusicXMLObject;
import utils.Triple;

public class DistributionalGlobalStructureEngineer extends GlobalStructureEngineer {

	public static class DistributionalGlobalStructureEngineerMusicXMLModel extends MusicXMLModel {

		BackedDistribution<GlobalStructure> distribution = null;
		private Map<GlobalStructure, List<Integer>> structureToSongIdx = new HashMap<GlobalStructure, List<Integer>>();
		private int trainCount = 0;
		
		@Override
		public void trainOnExample(ParsedMusicXMLObject musicXML) {
			distribution = null;
			
			SortedMap<Integer, Triple<SegmentType, Integer, Double>> globalStructureByFormStart = musicXML.getGlobalStructureByFormStart();
			SegmentType[] globalStructure = new SegmentType[globalStructureByFormStart.size()];
			
			int i = 0;
			for (Integer segmentType : globalStructureByFormStart.keySet()) {
				globalStructure[i++] = globalStructureByFormStart.get(segmentType).getFirst();
			}
			GlobalStructure structureX = new GlobalStructure(globalStructure); 

			List<Integer> songsWithStructureX = structureToSongIdx.get(structureX);
			if (songsWithStructureX == null) {
				songsWithStructureX = new ArrayList<Integer>();
				structureToSongIdx.put(structureX, songsWithStructureX);
			}
			songsWithStructureX.add(trainCount);
			
			trainCount++;
		}

		public GlobalStructure sampleAccordingToDistribution() {
			if (distribution == null) {
				generateDistribution();
			}
			return distribution.sampleAccordingToDistribution();
		}

		private void generateDistribution() {
			distribution = new BackedDistribution<GlobalStructure>(structureToSongIdx );
		}

	}

	private DistributionalGlobalStructureEngineerMusicXMLModel model;

	public DistributionalGlobalStructureEngineer() {
		this.model = (DistributionalGlobalStructureEngineerMusicXMLModel) MusicXMLModelLearner.getTrainedModel(this.getClass());
	}
	
	@Override
	public GlobalStructure generateStructure() {
		return model.sampleAccordingToDistribution();
	}

}
