package globalstructure;

public enum SegmentType {
	INTRO, VERSE, PRECHORUS, CHORUS, BRIDGE, OUTRO, INTERLUDE;

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
		case 'N':
			return INTERLUDE;
			default:
			return null;
		}
		
	}

	public boolean hasLyrics() {
		if(this == INTRO || this == OUTRO || this == INTERLUDE) {
			return false;
		}
		return true;
	} 
}
