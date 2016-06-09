package globalstructure;

public enum SegmentType {
	INTRO, VERSE, PRECHORUS, CHORUS, BRIDGE, OUTRO;

	public static SegmentType valueOf(Character key) {
		switch(key){
		case 'I':
			return INTRO;
		case 'V':
			return VERSE;
		case 'C':
			return CHORUS;
		case 'B':
			return BRIDGE;
		case 'O':
			return OUTRO;
			default:
			return null;
		}
		
	} 
}
