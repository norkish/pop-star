package globalstructure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import data.BackedDistribution;
import data.MusicXMLModel;
import data.MusicXMLModelLearner;
import data.ParsedMusicXMLObject;

public class DistributionalGlobalStructureEngineer extends GlobalStructureEngineer {

	public static class DistributionalGlobalStructureEngineerMusicXMLModel extends MusicXMLModel {

		BackedDistribution<GlobalStructure> distribution = null;
		private Map<GlobalStructure, List<Integer>> structureToSongIdx = new HashMap<GlobalStructure, List<Integer>>();
		private int trainCount = 0;
		
		@Override
		public void trainOnExample(ParsedMusicXMLObject musicXML) {
			distribution = null;
			
			GlobalStructure structureX = new GlobalStructure(musicXML.globalStructure.values().toArray(new SegmentType[0])); // 

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
