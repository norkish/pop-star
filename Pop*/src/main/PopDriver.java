package main;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import composition.Composition;
import orchestrate.Orchestrator;

/*
 * Generates a new song inspired from a system-selected inspiring idea
 */
public class PopDriver {
	public static void main(String[] args) throws IOException
	{
		annotateSysOutErrCalls();
		
		ProgramArgs.loadProgramArgs(args);
		
		Studio studio = new Studio();
		
		Composition newSong = studio.generate();
		
//		System.out.println(newSong);
		Files.write(Paths.get("./compositions/newSong.xml"), newSong.toString().getBytes());
		
		Orchestrator orchestrator = Orchestrator.getOrchestrator();
		orchestrator.orchestrate(newSong);
		Files.write(Paths.get("./compositions/newSongOrchestrated.xml"), newSong.toString().getBytes());
	}

	private static void annotateSysOutErrCalls() {
		System.setOut(createAnnotatedPrintStream(System.out));
		System.setErr(createAnnotatedPrintStream(System.err));
	}

	private static PrintStream createAnnotatedPrintStream(PrintStream stream) {
		return new java.io.PrintStream(stream) {

            private StackTraceElement getCallSite() {
                for (StackTraceElement e : Thread.currentThread()
                        .getStackTrace())
                    if (!e.getMethodName().equals("getStackTrace")
                            && !e.getClassName().equals(getClass().getName()))
                        return e;
                return null;
            }

            @Override
            public void println(String s) {
                println((Object) s);
            }

            @Override
            public void println(Object o) {
                StackTraceElement e = getCallSite();
                String callSite = e == null ? "??" :
                    String.format("%s.%s(%s:%d)",
                                  e.getClassName(),
                                  e.getMethodName(),
                                  e.getFileName(),
                                  e.getLineNumber());
                super.println(o + "\t\tat " + callSite);
            }
        };
	}
}
