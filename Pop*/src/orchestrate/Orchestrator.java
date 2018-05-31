package orchestrate;

import java.util.List;

import composition.Composition;
import composition.Measure;
import composition.Score;
import main.ProgramArgs;

public abstract class Orchestrator {

	public void orchestrate(Composition newSong) {
		
		// 1.All 1.5.- 2.chord 2.5.bass All (where all is chord and bass)
		Score score = newSong.getScore();
		score.hasOrchestration(true);
		List<Measure> measures = score.getMeasures();
		int changePatternInterval = 4;
		
		int prevPatternChoice = -1;
		for (int i = 0; i < measures.size(); i++) {
			Measure measure = measures.get(i);
			prevPatternChoice = orchestrate(measure, i == measures.size()-1, i % changePatternInterval == 0 ? -1 : prevPatternChoice);
		}
	}

	abstract int orchestrate(Measure measure, boolean lastMeasure, int prevMeasurePatternChoice);

	public static Orchestrator getOrchestrator() {
		Orchestrator orchestrator = null;

		switch (ProgramArgs.orchestratorSetting) {
		case COMPING:
			orchestrator = new CompingMusicXMLOrchestrator(); 
			break;
		default:
			throw new RuntimeException("Invalid orchestrator configuration: " + ProgramArgs.orchestratorSetting);
		}

		return orchestrator;
	}

}
