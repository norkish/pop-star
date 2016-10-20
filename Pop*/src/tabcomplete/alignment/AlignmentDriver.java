package tabcomplete.alignment;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class AlignmentDriver {

	public static void main(String[] args) throws FileNotFoundException {
//		System.out.println("Loading " + args[0]);
		Scanner lyrscan = new Scanner(new File(args[0]));
		
		List<Double> lyrs = new ArrayList<Double>();
		while(lyrscan.hasNextLine()) {
			lyrs.add(Double.parseDouble(lyrscan.nextLine()));
		}
		lyrscan.close();
		
//		System.out.println("Loading " + args[1]);
		Scanner notescan = new Scanner(new File(args[1]));
		
		List<Double> notes = new ArrayList<Double>();
		while(notescan.hasNextLine()) {
			notes.add(Double.parseDouble(notescan.nextLine()));
		}
		notescan.close();
		
//		System.out.println("Aligning:\n" + lyrs + "\nand\n" + notes + "...");
		
		Aligner.setMinPercOverlap(.7);
		SequencePair.setCosts(1,-1,-1,-1);
		TemporalSequencePairAlignment aln = (TemporalSequencePairAlignment) Aligner.alignNW(new TemporalSequencePair(lyrs, notes));
		System.out.println(aln.getFinalScore());
		
	}

}
