package tab;

import java.io.Serializable;
import java.util.List;
import java.util.SortedMap;

import harmony.Chord;

@SuppressWarnings("serial")
public class CompletedTab implements Serializable {

	public List<String> words;
	public List<SortedMap<Integer, Chord>> chords;
	public int[] scheme;
	public char[] structure;
	
	public CompletedTab(List<String> words, List<SortedMap<Integer, Chord>> chords, int[] scheme, char[] structure) {
		this.words = words;
		this.chords = chords;
		this.scheme = scheme;
		this.structure = structure;
	}

	public int length() {
		return scheme.length;
	}

	public char segmentLabelAt(int i) {
		return structure[i];
	}

}
