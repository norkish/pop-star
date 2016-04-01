package lyrics;

public class Lyric {

	String lyric = null;
	
	public Lyric(String lyric) {
		this.lyric = lyric;
	}

	public String toString()
	{
		return lyric;
	}

	public String lastSyllable() {
		int lastVowelSeqStart = -1;
		
		boolean inVowelSeq = false;
		for (int i = 0; i < lyric.length(); i++) {
			switch(lyric.toLowerCase().charAt(i))
			{
			case 'a':
			case 'i':
			case 'e':
			case 'o':
			case 'u':
			case 'y':
				if (!inVowelSeq)
				{
					inVowelSeq = true;
					lastVowelSeqStart = i;
				}
				break;
			default:
				inVowelSeq = false;
			}
		}
		
		return lyric.substring(lastVowelSeqStart);
	}
}
