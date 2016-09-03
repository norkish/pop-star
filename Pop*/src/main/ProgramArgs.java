package main;

import java.util.Arrays;

public class ProgramArgs {

	public enum UISetting {
		UNSET, COMMANDLINE, GUI
	}
	
	public enum SongConfigSourceSetting {
		FROM_FILE, RANDOM, FROM_COMMANDLINE, UNSET, SIMPLE, TEST, DISTRIBUTIONAL
	}
	
	public static SongConfigSourceSetting configurationSetting = SongConfigSourceSetting.UNSET;
	public static UISetting userInterfaceSetting = UISetting.UNSET;
	
	public static void loadProgramArgs(String[] args) {
		if (args.length == 0)
		{
			loadDefaultProgramArgs();
		}
		else
		{
			throw new UnsupportedOperationException("Illegal arguments passed to program:" + Arrays.toString(args));
		}
	}

	private static void loadDefaultProgramArgs() {
//		configurationSetting = SongConfigSourceSetting.TEST;
		configurationSetting = SongConfigSourceSetting.DISTRIBUTIONAL;
		userInterfaceSetting = UISetting.COMMANDLINE;
	}

}
