package tabcomplete.normalize;

import java.util.List;
import java.util.SortedMap;

import harmony.Chord;
import pitch.Pitch;

public class ChordNormalizer {

	public static int normalize(List<List<SortedMap<Integer, Chord>>> chordBlocks, int key) {
		if (chordBlocks.size() == 0 || key == Pitch.NO_KEY)
			return key;
		SortedMap<Integer, Chord> firstLine = chordBlocks.get(0).get(0);
		List<SortedMap<Integer, Chord>> lastBlock = chordBlocks.get(chordBlocks.size()-1);
		SortedMap<Integer, Chord> lastLine = lastBlock.get(lastBlock.size()-1);
		Chord firstChord = firstLine.get(firstLine.firstKey());
		Chord lastChord = lastLine.get(lastLine.lastKey());
		
		// First 24 inputs are frequencies of 24 chords, then 24 for first chord and 24 for last chord
		double[] inputs = new double[24];
		int total = 0;
		for (List<SortedMap<Integer,Chord>> block : chordBlocks) {
			for (SortedMap<Integer, Chord> line : block) {
				if (line == null) continue; //TODO: Check out why this is happening...
				for (Chord chord : line.values()) {
					if (chord.isMinor())
						inputs[chord.getRoot()*2+1]++;
					else
						inputs[chord.getRoot()*2]++;
					total++;
				}
			}
		}

//		inputs[24 + firstChord.getRoot() * 2 + (firstChord.isMinor()?1:0)]++;
//		inputs[48 + lastChord.getRoot() * 2 + (lastChord.isMinor()?1:0)]++;
		
		for (double d : inputs) {
			System.out.print((d/total) + ",");
		}
		
		System.out.print(firstChord.getRoot() + (firstChord.isMinor()?"m":"") + ",");
		System.out.print(lastChord.getRoot() + (lastChord.isMinor()?"m":"") + ",");
		
		System.out.println(key);
		
		return key;
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
}
