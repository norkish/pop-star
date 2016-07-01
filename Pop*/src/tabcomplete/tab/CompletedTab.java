package tabcomplete.tab;

import java.io.Serializable;
import java.util.List;
import java.util.SortedMap;

import harmony.Chord;

public class CompletedTab implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public int pitch;
	public List<String> words;
	public List<SortedMap<Integer, Chord>> chords;
	public int[] scheme;
	public char[] structure;
	public String tabURL;
	
	public CompletedTab(int pitch, List<String> words, List<SortedMap<Integer, Chord>> chords, int[] scheme, char[] structure, String tabURL) {
		this.words = words;
		this.chords = chords;
		this.scheme = scheme;
		this.structure = structure;
		this.tabURL = tabURL;
	}

	public int length() {
		return scheme.length;
	}

	public char segmentLabelAt(int i) {
		return structure[i];
	}

	public int rhymeSchemeAt(int i) {
		return scheme[i];
	}

	public String toString() {
		StringBuilder str = new StringBuilder();
		
		str.append("URL:");
		str.append(tabURL);
		str.append("\n");
		str.append("Key signature:");
		str.append(pitch);
		str.append("\n");
		
		for (int i = 0; i < scheme.length; i++) {
			str.append("\n");
			str.append("\t\t\t" + chords.get(i));
			str.append("\n");
			str.append("" + i + "\t" + structure[i] + "\t" + scheme[i] + "\t" + words.get(i));
		}
		
		return str.toString();
	}
}
