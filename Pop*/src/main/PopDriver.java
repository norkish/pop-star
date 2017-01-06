package main;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import composition.Composition;

/*
 * Generates a new song inspired from a system-selected inspiring idea
 */
public class PopDriver {
	public static void main(String[] args) throws IOException
	{
		ProgramArgs.loadProgramArgs(args);
		
		Studio studio = new Studio();
		
		Composition newSong = studio.generate();
		
//		System.out.println(newSong);
		Files.write(Paths.get("./newSong.xml"), newSong.toString().getBytes());
		
//		Orchestrator orchestrator = Orchestrator.getOrchestrator();
//		Orchestration orchestration = orchestrator.orchestrate(newSong);
//		orchestration.saveToFile(new File(System.getProperty("user.home") + "/popstar_hit.kar"));
	}
}
