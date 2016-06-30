package tabcomplete.tab;

import java.io.Serializable;
import java.util.List;
import java.util.SortedMap;

import harmony.Chord;
import pitch.Pitch;

@SuppressWarnings("serial")
public class CompletedTab implements Serializable {

	public int pitch;
	public List<String> words;
	public List<SortedMap<Integer, Chord>> chords;
	public int[] scheme;
	public char[] structure;
	
	public CompletedTab(int pitch, List<String> words, List<SortedMap<Integer, Chord>> chords, int[] scheme, char[] structure) {
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

	public int rhymeSchemeAt(int i) {
		return scheme[i];
	}

}
