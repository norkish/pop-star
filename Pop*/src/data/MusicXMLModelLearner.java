package data;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;

import globalstructure.GlobalStructureExtractor;
import harmony.SegmentSpecificHarmonyEngineer;
import harmony.SegmentSpecificHarmonyEngineer.SegmentSpecificHarmonyEngineerMusicXMLModel;
import lyrics.LyricTemplateEngineer;
import lyrics.LyricTemplateEngineer.LyricTemplateEngineerMusicXMLModel;
import melody.SegmentSpecificMelodyEngineer;
import melody.SegmentSpecificMelodyEngineer.SegmentSpecificMelodyEngineerMusicXMLModel;

public class MusicXMLModelLearner {
	private static final File[] files = new File(
			"/Users/norkish/Archive/2017_BYU/ComputationalCreativity/data/Wikifonia").listFiles();

	private static Map<Class,MusicXMLModel> trainedModels = null;
	
	public static MusicXMLModel getTrainedModel(Class modelClassName) {
		if (trainedModels == null) {
			Map<Class,MusicXMLModel> models = new HashMap<Class,MusicXMLModel>();

			// populate from configuration
			models.put(LyricTemplateEngineer.class, new LyricTemplateEngineerMusicXMLModel());
			models.put(SegmentSpecificHarmonyEngineer.class, new SegmentSpecificHarmonyEngineerMusicXMLModel());
			models.put(SegmentSpecificMelodyEngineer.class, new SegmentSpecificMelodyEngineerMusicXMLModel());
			
			System.out.println("Training models on XML dataset");
			trainModelsOnWholeDataset(models.values());
			System.out.println("Training complete");
			
			trainedModels = models;
			
		} else if (!trainedModels.containsKey(modelClassName)) {
			throw new RuntimeException();
		}
		return trainedModels.get(modelClassName);
	}
	
	private static void trainModelsOnWholeDataset(Collection<MusicXMLModel> models) {
		for (File file : files) {
			 if (!file.getName().equals("Billy Joel - Just The Way You Are.mxl"))
			 continue;
			// if (file.getName().charAt(0) < 'T') {
			// continue;
			// }
			System.out.println(file.getName());
			MusicXMLParser musicXMLParser = null;
			try {
				final Document xml = MusicXMLSummaryGenerator.mxlToXML(file);
				MusicXMLSummaryGenerator.printDocument(xml, System.out);

				musicXMLParser = new MusicXMLParser(xml);
			} catch (Exception e) {
				e.printStackTrace();
			}
			WikifoniaCorrection.applyManualCorrections(musicXMLParser, file.getName());
			ParsedMusicXMLObject musicXML = musicXMLParser.parse(true);
			System.out.println(musicXML);
			
			GlobalStructureExtractor.annotateGlobalStructure(musicXML);

			for (MusicXMLModel musicXMLModel: models) {
				musicXMLModel.trainOnExample(musicXML);
			}
		}
	}
	
	public static void main(String[] args) {
		LyricTemplateEngineerMusicXMLModel model = (LyricTemplateEngineerMusicXMLModel) MusicXMLModelLearner.getTrainedModel(LyricTemplateEngineer.class);
		System.out.println(model);
	}

}
