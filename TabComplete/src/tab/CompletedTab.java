package tab;

import java.util.List;
import java.util.SortedMap;

import harmony.Chord;

public class CompletedTab {

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

}
