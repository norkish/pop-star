package tabcomplete.normalize;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import harmony.Chord;
import pitch.Pitch;
import tabcomplete.rawsheet.ChordSheet;
import tabcomplete.rawsheet.RawDataLoader;
import tabcomplete.utils.Utils;

public class ChordNormalizer {

	final static private int QUALITY_CT = 3;
	final static private int MINOR_OFFSET = 1;
	final static private int DIMINISHED_OFFSET = 2;
	
	public static int normalize(List<List<SortedMap<Integer, Chord>>> chordBlocks, int key) {
		if (chordBlocks.size() == 0) {
			return key;
		}
		
		// We first sum up all occurrences of each major and minor chord for all 12 steps of the scale
		int[] occurrences = new int[12 * QUALITY_CT];
		
		for (List<SortedMap<Integer, Chord>> chordBlock : chordBlocks) {
			if (chordBlock == null) continue;
			for (SortedMap<Integer, Chord> chordLine : chordBlock) {
				if (chordLine == null) continue;
				for (Chord chord : chordLine.values()) {
					occurrences[chord.getRoot() * QUALITY_CT + (chord.isMinor() ? MINOR_OFFSET:chord.isDiminished()?DIMINISHED_OFFSET:0)]++;
				}
			}
		}
		
		int bssf = -1;
		int bksf = -1;
		int currSolution;
		// Then find the key k which maximizes the notes comprising the chords of the song that are in the key signature
		for (int i = 0; i < 12; i++) {
			currSolution = 0;
			System.out.print("For " + Pitch.getPitchName(i) + ":");
//			System.out.println("\tFound " + occurrences[i*QUALITY_CT] + " of " + Pitch.getPitchName(i) + " (root major)");
//			System.out.println("\tFound " + occurrences[((i + 2) % 12)*QUALITY_CT + MINOR_OFFSET] + " of " + Pitch.getPitchName((i+2)%12) + "m (2 minor)");
//			System.out.println("\tFound " + occurrences[((i + 4) % 12)*QUALITY_CT + MINOR_OFFSET] + " of " + Pitch.getPitchName((i+4)%12) + "m (3 minor)");
//			System.out.println("\tFound " + occurrences[((i + 5) % 12)*QUALITY_CT] + " of " + Pitch.getPitchName((i+5)%12) + " (4 major)");
//			System.out.println("\tFound " + occurrences[((i + 7) % 12)*QUALITY_CT] + " of " + Pitch.getPitchName((i+7)%12) + " (5 major)");
//			System.out.println("\tFound " + occurrences[((i + 9) % 12)*QUALITY_CT + MINOR_OFFSET] + " of " + Pitch.getPitchName((i+9)%12) + "m (6 minor)");
//			System.out.println("\tFound " + occurrences[((i + 11) % 12)*QUALITY_CT + DIMINISHED_OFFSET] + " of " + Pitch.getPitchName((i+11)%12) + "dim (7 dim)");
			currSolution += occurrences[i*QUALITY_CT]; // hypothetical major root
			currSolution += occurrences[((i + 2) % 12)*QUALITY_CT + MINOR_OFFSET]; // minor 2nd (add 2 half steps, and plus offset for minor)
			currSolution += occurrences[((i + 4) % 12)*QUALITY_CT + MINOR_OFFSET]; // minor 3nd (add 4 half steps, and plus offset for minor)
			currSolution += occurrences[((i + 5) % 12)*QUALITY_CT]; // major 4th (add 5 half steps)
			currSolution += occurrences[((i + 7) % 12)*QUALITY_CT]; // major 5th (add 7 half steps)
			currSolution += occurrences[((i + 9) % 12)*QUALITY_CT + MINOR_OFFSET]; // minor 6th (add 4 half steps, and plus offset for minor)
			currSolution += occurrences[((i + 11) % 12)*QUALITY_CT + DIMINISHED_OFFSET]; // dim 7th (add 4 half steps, and plus offset for diminished)
			
			System.out.println("SCORE = " + currSolution);
			
			if (currSolution > bssf) { 
				bssf = currSolution;
				bksf = i;
			}
		}
		
		Utils.promptEnterKey("Check the chords (chose " + Pitch.getPitchName(bksf) + ")");

//		SortedMap<Integer, Chord> firstLine = chordBlocks.get(0).get(0);
//		List<SortedMap<Integer, Chord>> lastBlock = chordBlocks.get(chordBlocks.size()-1);
//		SortedMap<Integer, Chord> lastLine = lastBlock.get(lastBlock.size()-1);
//		Chord firstChord = firstLine.get(firstLine.firstKey());
//		Chord lastChord = lastLine.get(lastLine.lastKey());
		
		return bksf;
	}
	
	public static int normalizeByMachineLearning(List<List<SortedMap<Integer, Chord>>> chordBlocks, int key) {
		// NOT FINISHED!
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
	
	public static void main(String[] args) throws FileNotFoundException, IOException {
		Map<String, Map<String, List<ChordSheet>>> chordSheets = RawDataLoader.loadChordSheets();
	}
}
