package alignment;

import harmony.Chord;

public class TokenComparator {
	public static boolean matchCharactersGenerally(char charA, char charB) {
		if (Character.isWhitespace(charA))
			return Character.isWhitespace(charB);
		
		switch(Character.getType(charA)){
		case Character.LOWERCASE_LETTER:
			return charA == Character.toLowerCase(charB);
		case Character.UPPERCASE_LETTER:
			return charA == Character.toUpperCase(charB);
		case Character.OTHER_PUNCTUATION:
			return Character.getType(charB) == Character.OTHER_PUNCTUATION;
		}
		
		return charA == charB;
	}

	public static boolean matchChordsGenerally(Chord chord1, Chord chord2) {
		int root1 = chord1.getRoot();
		int root2 = chord2.getRoot();
		boolean isMinor1 = chord1.isMinor();
		boolean isMinor2 = chord2.isMinor();
		
		//TODO: implement and give graduated scores for relative major matching
		if (root1 == root2) {
			return (isMinor1 == isMinor2); // root and minor are the same
		} /*else if (isMinor1 == isMinor2) {
			return false; // root different, minor same
		} else if (isMinor1) { // first is minor, second is not
			return (root2 == chord1.relativeMajor()) ; // chords are related by natural minor
		} else { // first is major, second is minor
			return (root1 == chord2.relativeMajor()); // chords are related by natural minor
		}*/
		
		return false; // roots are different
	}
	
	public static void main(String[] args) {
		Chord d = Chord.parse("D", true);
		Chord em = Chord.parse("Em", true);
		Chord e = Chord.parse("E", true);
		Chord bb = Chord.parse("Bb", true);
		Chord bbm = Chord.parse("Bbm", true);
		Chord bbmaj = Chord.parse("Bbmaj", true);
		System.out.println(matchChordsGenerally(d, d));
		System.out.println(matchChordsGenerally(em, em));
		System.out.println(matchChordsGenerally(bb, bb));
		System.out.println(matchChordsGenerally(bb, bbmaj));
		System.out.println(matchChordsGenerally(e, em));
		System.out.println(matchChordsGenerally(bb, bbm));
		System.out.println(matchChordsGenerally(bbm, bbmaj));
		System.out.println(bb.isMinor());
		System.out.println(bbmaj.isMinor());
	}
}
