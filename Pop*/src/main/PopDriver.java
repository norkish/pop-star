package main;

import composition.Composition;
import orchestrate.Orchestrator;

/*
 * Generates a new song inspired from a system-selected inspiring idea
 */
public class PopDriver {
	public static void main(String[] args)
	{
		ProgramArgs.loadProgramArgs(args);
		
		Studio studio = new Studio();
		
		Composition newSong = studio.generate();
		
		System.out.println(newSong);
		
		Orchestrator orchestrator = Orchestrator.getOrchestrator();
		orchestrator.orchestrate(newSong);
		
	}
}
