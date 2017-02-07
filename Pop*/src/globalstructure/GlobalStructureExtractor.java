package globalstructure;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.SortedMap;
import java.util.TreeMap;

import constraint.Constraint;
import data.ParsedMusicXMLObject;
import tabcomplete.alignment.Aligner;
import tabcomplete.alignment.Alignment;
import tabcomplete.alignment.MusicXMLPair;
import tabcomplete.alignment.SequencePair;

public class GlobalStructureExtractor {

	public static void annotateGlobalStructure(ParsedMusicXMLObject musicXML) {
		
		if (musicXML.unoverlappingHarmonyByPlayedMeasure.isEmpty() || musicXML.getNotesByPlayedMeasure().isEmpty()) {
			musicXML.globalStructure = null;
			return;
		}

		SortedMap<Integer, SegmentType> globalStructure = annotateGlobalStructureUsingFixed(musicXML); // only works for Just the Way You Are
		musicXML.globalStructure = globalStructure;		
	}

	private static SortedMap<Integer, SegmentType> annotateGlobalStructureUsingFixed(ParsedMusicXMLObject musicXML) {
		SortedMap<Integer, SegmentType> structure = new TreeMap<Integer, SegmentType>();

		// uncomment to see the played-to-absolute measure number
		int i = 0;
		for (Integer msr : musicXML.playedToAbsoluteMeasureNumberMap) {
			System.err.println(i++ + "\t" + msr);
		}
		
		// First load contents of file
		Scanner scan;
		String filename = musicXML.filename.replaceFirst("mxl(\\.[\\d])?", "txt");
		try {
			scan = new Scanner(new File(GlobalStructure.GLOBAL_STRUCTURE_ANNOTATIONS_DIR + "/" + filename));
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
		
		String nextLine;
		while(scan.hasNextLine()) {
			nextLine = scan.nextLine();
			if (nextLine.startsWith("//")) {
				continue;
			}
			String[] tokens = nextLine.split("\t");
			int playedMeasureNumber = Integer.parseInt(tokens[0]);
			SegmentType type = SegmentType.valueOf(tokens[1]);
			assert structure.isEmpty() || playedMeasureNumber > structure.lastKey(): "Global Structure annotation should be in order by measure number where segments occur";
			structure.put(playedMeasureNumber, type);
		}		
		return structure;
	}

	private static SortedMap<Integer, SegmentType> annotateGlobalStructureUsingAlignment(ParsedMusicXMLObject musicXML) {

		SequencePair.setCosts(1, -10, -10, -10);

		// Align the composition against itself, disallowing it to align identity-wise, and using lyrics
		Alignment aln = Aligner.alignSW(new MusicXMLPair(musicXML, musicXML, 0.0, 1.0, 1.0, 1.0));
		double score = aln.getFinalScore();
		System.err.println(aln.toString());

//		if (DEBUG) tabcomplete.utils.Utils.print2DMatrixInt(binary_matrix);
		
		
		return null;
	}

	public static boolean annotationsExistForFile(File MusicXMLFile) {
		String filename = MusicXMLFile.getName().replaceFirst("mxl(\\.[\\d])?", "txt");
		File file =  new File(GlobalStructure.GLOBAL_STRUCTURE_ANNOTATIONS_DIR + "/" + filename);
		return file.exists();
	}

}
