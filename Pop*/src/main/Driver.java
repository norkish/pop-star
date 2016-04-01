package main;

import composition.Composition;

/*
 * Generates a new song inspired from a system-selected inspiring idea
 */
public class Driver {
	public static void main(String[] args)
	{
		ProgramArgs.loadProgramArgs(args);
		
		Studio studio = new Studio();
		
		Composition newSong = studio.generate();
		
		System.out.println(newSong);
	}
}
