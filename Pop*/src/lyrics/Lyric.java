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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((lyric == null) ? 0 : lyric.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof Lyric))
			return false;
		Lyric other = (Lyric) obj;
		if (lyric == null) {
			if (other.lyric != null)
				return false;
		} else if (!lyric.equals(other.lyric))
			return false;
		return true;
	}
}
