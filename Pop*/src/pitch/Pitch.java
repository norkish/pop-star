package pitch;

import java.util.HashMap;
import java.util.Map;

public class Pitch {
	public final static int NO_KEY = 13;
	
	private final static  Map<String,Integer> valueByName;
	static {
		valueByName = new HashMap<String,Integer>();
		valueByName.put("A", 0);
		valueByName.put("A#", 1);
		valueByName.put("Bb", 1);
		valueByName.put("B", 2);
		valueByName.put("Cb", 2);
		valueByName.put("B#", 3);
		valueByName.put("C", 3);
		valueByName.put("C#", 4);
		valueByName.put("Db", 4);
		valueByName.put("D", 5);
		valueByName.put("D#", 6);
		valueByName.put("Eb", 6);
		valueByName.put("E", 7);
		valueByName.put("Fb", 7);
		valueByName.put("F", 8);
		valueByName.put("E#", 8);
		valueByName.put("F#", 9);
		valueByName.put("Gb", 9);
		valueByName.put("G", 10);
		valueByName.put("G#", 11);
		valueByName.put("Ab", 11);
	}

//	private final static String[] nameByValueSharp = new String[]{"A","A#","B","C","C#","D","D#","E","F","F#","G","G#"};
//	private final static String[] nameByValueFlat = new String[]{"A","Bb","B","C","Db","D","Eb","E","F","Gb","G","Ab"};
	private final static String[] nameByValue = new String[]{"A","Bb","B","C","C#","D","Eb","E","F","F#","G","Ab"};
	
//	private final static Set<Integer> accidentals = new HashSet<Integer>(Arrays.asList(1,4,6,9,11)); 
//	private final static Set<Integer> keysUsingFlats = new HashSet<Integer>(Arrays.asList(1,6,8,11)); 
	
	public static int getPitchValue(String name) {
		if (!valueByName.containsKey(name))
			return NO_KEY;
		return valueByName.get(name);
	}

	public static String getPitchName(int value) {//, int key) {
		if (value == NO_KEY) {
			return "X";
		} else if (value < 0) {
			return "-1";
		}
//		if (!accidentals.contains(value)) {
//			return nameByValue[value];
//		}
//		else if(keysUsingFlats.contains(key)) {
//			return nameByValueFlat[value];
//		} else if (key == 3) {
//			return nameByValue[value];
//		} else {
//			return nameByValueSharp[value];
//		}
		return nameByValue[value];
	}

}
