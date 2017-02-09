package data;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.w3c.dom.Document;

import globalstructure.DistributionalGlobalStructureEngineer;
import globalstructure.DistributionalGlobalStructureEngineer.DistributionalGlobalStructureEngineerMusicXMLModel;
import globalstructure.StructureExtractor;
import harmony.SegmentSpecificHarmonyEngineer;
import harmony.SegmentSpecificHarmonyEngineer.SegmentSpecificHarmonyEngineerMusicXMLModel;
import lyrics.LyricTemplateEngineer;
import lyrics.LyricTemplateEngineer.LyricTemplateEngineerMusicXMLModel;
import melody.SegmentSpecificMelodyEngineer;
import melody.SegmentSpecificMelodyEngineer.SegmentSpecificMelodyEngineerMusicXMLModel;
import segmentstructure.DistributionalSegmentStructureEngineer;
import segmentstructure.DistributionalSegmentStructureEngineer.DistributionalSegmentStructureEngineerMusicXMLModel;
import tabcomplete.main.TabDriver;

public class MusicXMLModelLearner {
	private static final File[] files = new File(
			TabDriver.dataDir + "/Wikifonia").listFiles();

	private static Map<Class,MusicXMLModel> trainedModels = null;
	
	public static MusicXMLModel getTrainedModel(Class modelClassName) {
		if (trainedModels == null) {
			LinkedHashMap<Class, MusicXMLModel> models = new LinkedHashMap<Class,MusicXMLModel>();

			// populate from configuration
			models.put(DistributionalGlobalStructureEngineer.class, new DistributionalGlobalStructureEngineerMusicXMLModel());
			models.put(DistributionalSegmentStructureEngineer.class, new DistributionalSegmentStructureEngineerMusicXMLModel());
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
//			 if (!file.getName().equals("Billy Joel - Just The Way You Are.mxl"))
//			 continue;
			// if (file.getName().charAt(0) < 'T') {
			// continue;
			// }
			 if (!StructureExtractor.annotationsExistForFile(file)) {
				 continue;
			 }
			System.out.println(file.getName());
			MusicXMLParser musicXMLParser = null;
			try {
				final Document xml = MusicXMLSummaryGenerator.mxlToXML(file);
//				MusicXMLSummaryGenerator.printDocument(xml, System.out);

				musicXMLParser = new MusicXMLParser(file.getName(), xml);
			} catch (Exception e) {
				e.printStackTrace();
			}
			WikifoniaCorrection.applyManualCorrections(musicXMLParser, file.getName());
			ParsedMusicXMLObject musicXML = musicXMLParser.parse(true);
			if (musicXML == null) {
				System.err.println("musicXML was null for " + file.getName());
				continue;
			}
			System.out.println(musicXML);
			try {
				StructureExtractor.annotateStructure(musicXML);
	
				for (MusicXMLModel musicXMLModel: models) {
					musicXMLModel.trainOnExample(musicXML);
				}
			} catch (Exception e) {
				System.err.println("For " + file.getName() + ":\n");
				throw new RuntimeException(e);
			}
		}
	}
	
	public static void main(String[] args) {
		LyricTemplateEngineerMusicXMLModel model = (LyricTemplateEngineerMusicXMLModel) MusicXMLModelLearner.getTrainedModel(LyricTemplateEngineer.class);
		System.out.println(model);
	}

}
