package lyrics;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import globalstructure.SegmentType;
import inspiration.Inspiration;
import substructure.SegmentSubstructure;
import tabcomplete.rhyme.RhymeStructureAnalyzer;

public class BensLyricalEngineer extends LyricalEngineer {

	private static double replacementFrequency;

	
	@Override
	protected void applyVariationToChorus(LyricSegment lyricSegment, Inspiration inspiration,
			SegmentSubstructure segmentSubstructures, SegmentType segmentKey, boolean isLast) {
		// TODO Auto-generated method stub

	}

	public static void main(String[] args) {
        long startTime = System.nanoTime();
        //SegmentSubstructure segsub = new SegmentSubstructure(4);


		BensLyricalEngineer lEngineer = new BensLyricalEngineer();
		LyricSegment lyrics = lEngineer.generateSegmentLyrics(null, null, null);

        tryRhymes(lyrics);
		
		setReplacementFrequency(Integer.parseInt(args[0]));
		print(lyrics);
		StanfordPosTagger posLyrics = new StanfordPosTagger(lyrics);
		
		LyricPack pack = new LyricPack(posLyrics.getTaggedWords());
		pack.fillPartmapAndList();
		pack.markPartmapForReplacements(replacementFrequency);
		//pack.replaceMarked();
		pack.replaceMarkedW2vAnalogy();
		//pack.replaceMarkedW2vSimilar();
		//pack.replaceMarkedW2vSubtract();
		//pack.replaceMarkedW2vAdd();
		pack.print();
		
//		try {
//			System.out.println("TRYING STANFORD TOKENIZER BELOW");
//			new StanfordTokenizer().run();
//		}
//		catch (IOException e) {
//			e.printStackTrace();
//		}
        long endTime = System.nanoTime();
        System.out.println("Completed in " + ((endTime - startTime) / 1000000) + " milliseconds (" + ((endTime - startTime) / 1000000000) + " seconds).");
	}
	
	@Override
	protected LyricSegment generateSegmentLyrics(Inspiration inspiration, SegmentSubstructure segmentSubstructures,
			SegmentType segmentKey) {
		// SegmentKey is the type of stanza you're generating
		// segmentSubstructures are the constraints that you wanna satisfy
		// inspiration is the mood of the stanza

        return readInLyrics("terrible-love-short.txt");

//		List<List<Lyric>> song = new ArrayList<List<Lyric>>();
//		List<Lyric> l1 = new ArrayList<Lyric>();
//		List<Lyric> l2 = new ArrayList<Lyric>();
//		List<Lyric> l3 = new ArrayList<Lyric>();
//		List<Lyric> l4 = new ArrayList<Lyric>();
//		List<Lyric> l5 = new ArrayList<Lyric>();
//		List<Lyric> l6 = new ArrayList<Lyric>();
//		List<Lyric> l7 = new ArrayList<Lyric>();
//		List<Lyric> l8 = new ArrayList<Lyric>();

//		l1.add(new Lyric("When"));
//		l1.add(new Lyric("all"));
//		l1.add(new Lyric("waters"));
//		l1.add(new Lyric("still"));
//		l2.add(new Lyric("And"));
//		l2.add(new Lyric("flowers"));
//		l2.add(new Lyric("cover"));
//		l2.add(new Lyric("the"));
//		l2.add(new Lyric("earth"));
//		l3.add(new Lyric("When"));
//		l3.add(new Lyric("no"));
//		l3.add(new Lyric("tree's"));
//		l3.add(new Lyric("shivering"));
//		l4.add(new Lyric("And"));
//		l4.add(new Lyric("the"));
//		l4.add(new Lyric("dust"));
//		l4.add(new Lyric("settles"));
//		l4.add(new Lyric("in"));
//		l4.add(new Lyric("the"));
//		l4.add(new Lyric("desert"));
//		l5.add(new Lyric("When"));
//		l5.add(new Lyric("I"));
//		l5.add(new Lyric("can"));
//		l5.add(new Lyric("take"));
//		l5.add(new Lyric("your"));
//		l5.add(new Lyric("hand"));
//		l6.add(new Lyric("On"));
//		l6.add(new Lyric("any"));
//		l6.add(new Lyric("crowded"));
//		l6.add(new Lyric("street"));
//		l7.add(new Lyric("And"));
//		l7.add(new Lyric("hold"));
//		l7.add(new Lyric("you"));
//		l7.add(new Lyric("close"));
//		l7.add(new Lyric("to"));
//		l7.add(new Lyric("me"));
//		l8.add(new Lyric("With"));
//		l8.add(new Lyric("no"));
//		l8.add(new Lyric("hesitating"));
//		stanza.add(l1);
//		stanza.add(l2);
//		stanza.add(l3);
//		stanza.add(l4);
//		stanza.add(l5);
//		stanza.add(l6);
//		stanza.add(l7);
//		stanza.add(l8);
		
		
		
//		l1.add(new Lyric("sorrow"));
//		l1.add(new Lyric("found"));
//		l1.add(new Lyric("me"));
//		l1.add(new Lyric("when"));
//		l1.add(new Lyric("I"));
//		l1.add(new Lyric("was"));
//		l1.add(new Lyric("young"));
//		l2.add(new Lyric("sorrow"));
//		l2.add(new Lyric("waited,"));
//		l2.add(new Lyric("sorrow"));
//		l2.add(new Lyric("won"));
//		l3.add(new Lyric("sorrow"));
//		l3.add(new Lyric("they"));
//		l3.add(new Lyric("put"));
//		l3.add(new Lyric("me"));
//		l3.add(new Lyric("on"));
//		l3.add(new Lyric("the"));
//		l3.add(new Lyric("pill"));
//		l4.add(new Lyric("It's"));
//		l4.add(new Lyric("in"));
//		l4.add(new Lyric("my"));
//		l4.add(new Lyric("honey,"));
//		l4.add(new Lyric("it's"));
//		l4.add(new Lyric("in"));
//		l4.add(new Lyric("my"));
//		l4.add(new Lyric("milk"));
//		stanza.add(l1);
//		stanza.add(l2);
//		stanza.add(l3);
//		stanza.add(l4);
//		stanza.add(l5);
		
		
		
//		l1.add(new Lyric("It's"));
//		l1.add(new Lyric("a"));
//		l1.add(new Lyric("terrible"));
//		l1.add(new Lyric("love"));
//		l1.add(new Lyric("that"));
//		l1.add(new Lyric("I'm"));
//		l1.add(new Lyric("walking"));
//		l1.add(new Lyric("with"));
//		l1.add(new Lyric("spiders"));
//		l2.add(new Lyric("It's"));
//		l2.add(new Lyric("a"));
//		l2.add(new Lyric("terrible"));
//		l2.add(new Lyric("love"));
//		l2.add(new Lyric("that"));
//		l2.add(new Lyric("I'm"));
//		l2.add(new Lyric("walking"));
//		l2.add(new Lyric("in"));
//		l3.add(new Lyric("It's"));
//		l3.add(new Lyric("quiet"));
//		l3.add(new Lyric("company"));
//		stanza.add(l1);
//		stanza.add(l2);
//		stanza.add(l1);
//		stanza.add(l2);
//		stanza.add(l3);
//		stanza.add(l3);
		
		//SparseSingleOrderMarkovModel<Lyric> segmentSpecificMM = mModel.get(segmentKey);
    }

	//TODO > having a LyricSegment have lyrics instead of Strings is a pain in the neck
	private static void tryRhymes(LyricSegment lyrics) {
        //ArrayList<ArrayList<String>> segmentString = new ArrayList<ArrayList<String>>();
        ArrayList<int[]> rhymeSchemes = new ArrayList<int[]>();
        for (int i = 0; i < lyrics.getLines().size(); i++) {
            List<Lyric> line = lyrics.getLine(i);
            ArrayList<String> lineString = new ArrayList<String>();
            for (int j = 0; j < lyrics.getLine(i).size(); j++) {
                Lyric lyric = line.get(j);
                String lyricString = lyric.toString();
                lineString.add(lyricString);
            }
            int[] lineRhymeScheme = RhymeStructureAnalyzer.extractRhymeScheme(lineString);
            rhymeSchemes.add(lineRhymeScheme);
        }

    }

	private static void setReplacementFrequency(int n) {
		replacementFrequency = n;
	}
	
	private static void print(LyricSegment lyrics) {
		List<List<Lyric>> lines = lyrics.getLines();
		for (int i = 0; i < lines.size(); i++) {
			List<Lyric> line = lines.get(i);
			for (int j = 0; j < line.size(); j++) {
				Lyric lyric = line.get(j);
				System.out.print(lyric.toString() + " ");
			}
			System.out.print("\n");
		}
	}

	private static LyricSegment readInLyrics(String songFile) {
        List<List<Lyric>> song = new ArrayList<List<Lyric>>();
        try {
            BufferedReader br;
//            br = new BufferedReader(new FileReader("../../../songs/"));
            br = new BufferedReader(new FileReader("/Users/Benjamin/Documents/workspace/pop-star/songs/" + songFile));
            String line = br.readLine();

            while (line != null) {
                String[] splitLine = line.split("\\s");
                List<Lyric> tempLine = new ArrayList<Lyric>();
                //TODO sloppy: change this somehow
                for (int i = 0; i < splitLine.length; i++) {
                    tempLine.add(new Lyric(splitLine[i]));
                }
                song.add(tempLine);
                line = br.readLine();
            }
            br.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            return new LyricSegment(song);
        }
    }

}


























































