package orchestrate;

import composition.Composition;
import main.ProgramArgs;

public abstract class Orchestrator {

	public abstract Orchestration orchestrate(Composition newSong);

	public static Orchestrator getOrchestrator() {
		Orchestrator orchestrator = null;

		switch (ProgramArgs.orchestratorSetting) {
		case COMPING:
			orchestrator = new CompingOrchestrator(); 
			break;
		default:
			throw new RuntimeException("Invalid orchestrator configuration: " + ProgramArgs.orchestratorSetting);
		}

		return orchestrator;
	}

}
