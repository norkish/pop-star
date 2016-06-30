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
	private String tabURL;
	
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

}
