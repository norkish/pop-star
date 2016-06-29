package lyrics;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Lyric implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
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
		
		if (lastVowelSeqStart == -1)
			return lyric;
		else 
			return lyric.substring(lastVowelSeqStart);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((lyric == null) ? 0 : lyric.toLowerCase().hashCode());
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
		} else if (!lyric.equalsIgnoreCase(other.lyric))
			return false;
		return true;
	}

	public static List<Lyric> parseLyrics(String wordsForLine) {
		List<Lyric> list = new ArrayList<Lyric>();
		if (wordsForLine.length() == 0) {
			return list;
		}
		for (String word : wordsForLine.split("[^a-zA-Z0-9']+")) {
			if (word.length() > 0) {
				list.add(new Lyric(word));
			}
		}
		return list;
	}
}
