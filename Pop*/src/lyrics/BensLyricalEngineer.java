package lyrics;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import constraint.ConstraintBlock;
import data.BackedDistribution;
import data.DataLoader;
import globalstructure.SegmentType;
import inspiration.Inspiration;
import markov.SparseSingleOrderMarkovModel;
import substructure.SegmentSubstructure;
import utils.Utils;
import edu.stanford.nlp.*;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.Tag;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.parser.nndep.DependencyParser;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import edu.stanford.nlp.trees.GrammaticalStructure;

public class BensLyricalEngineer extends LyricalEngineer {

	private static double replacementFrequency;
	private static HashMap<String, ArrayList<TaggedWord>> allWordsByPart = new HashMap<String, ArrayList<TaggedWord>>();
	private static HashMap<String, ArrayList<TaggedWord>> markedWordsByPart = new HashMap<String, ArrayList<TaggedWord>>();

	//private Map<SegmentType, Map<Integer, BackedDistribution<ConstraintBlock<Lyric>>>> lyricConstraintsDistribution = DataLoader.getLyricConstraintsDistribution();
	//private Map<SegmentType, SparseSingleOrderMarkovModel<Lyric>> mModel = DataLoader.getLyricMarkovModel();
	
	@Override
	protected void applyVariationToChorus(LyricSegment lyricSegment, Inspiration inspiration,
			SegmentSubstructure segmentSubstructures, SegmentType segmentKey, boolean isLast) {
		// TODO Auto-generated method stub

	}

	public static void main(String[] args) {
		BensLyricalEngineer lEngineer = new BensLyricalEngineer();
		LyricSegment lyrics = lEngineer.generateSegmentLyrics(null, null, null);
		
		setReplacementFrequency(Integer.parseInt(args[0]));
		
		print(lyrics);
		
		StanfordPosTagger posLyrics = new StanfordPosTagger(lyrics);
		
		LyricPack pack = new LyricPack(posLyrics.getPos());
		pack.fillPartmapAndList();
		pack.markPartmap(replacementFrequency);
		pack.replaceMarked();
		pack.printList();
		
//		fillAllByPart(posLyrics.getPosEz());
//		markByPart();
//		printReplacements(lyrics);
	}
	
	@Override
	protected LyricSegment generateSegmentLyrics(Inspiration inspiration, SegmentSubstructure segmentSubstructures,
			SegmentType segmentKey) {
		// SegmentKey is the type of stanza you're generating
		// segmentSubstructures are the constraints that you wanna satisfy
		// inspiration is the mood of the stanza
		List<List<Lyric>> stanza = new ArrayList<List<Lyric>>();
		List<Lyric> l1 = new ArrayList<Lyric>();
		List<Lyric> l2 = new ArrayList<Lyric>();
		List<Lyric> l3 = new ArrayList<Lyric>();
		List<Lyric> l4 = new ArrayList<Lyric>();
		List<Lyric> l5 = new ArrayList<Lyric>();
		List<Lyric> l6 = new ArrayList<Lyric>();
		List<Lyric> l7 = new ArrayList<Lyric>();
		List<Lyric> l8 = new ArrayList<Lyric>();

//		l1.add(new Lyric("When"));
//		l1.add(new Lyric("all"));
//		l1.add(new Lyric("waters"));
//		l1.add(new Lyric("still"));
//
//		l2.add(new Lyric("And"));
//		l2.add(new Lyric("flowers"));
//		l2.add(new Lyric("cover"));
//		l2.add(new Lyric("the"));
//		l2.add(new Lyric("earth"));
//
//		l3.add(new Lyric("When"));
//		l3.add(new Lyric("no"));
//		l3.add(new Lyric("tree's"));
//		l3.add(new Lyric("shivering"));
//
//		l4.add(new Lyric("And"));
//		l4.add(new Lyric("the"));
//		l4.add(new Lyric("dust"));
//		l4.add(new Lyric("settles"));
//		l4.add(new Lyric("in"));
//		l4.add(new Lyric("the"));
//		l4.add(new Lyric("desert"));
//
//		l5.add(new Lyric("When"));
//		l5.add(new Lyric("I"));
//		l5.add(new Lyric("can"));
//		l5.add(new Lyric("take"));
//		l5.add(new Lyric("your"));
//		l5.add(new Lyric("hand"));
//
//		l6.add(new Lyric("On"));
//		l6.add(new Lyric("any"));
//		l6.add(new Lyric("crowded"));
//		l6.add(new Lyric("street"));
//
//		l7.add(new Lyric("And"));
//		l7.add(new Lyric("hold"));
//		l7.add(new Lyric("you"));
//		l7.add(new Lyric("close"));
//		l7.add(new Lyric("to"));
//		l7.add(new Lyric("me"));
//
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
//
//		l2.add(new Lyric("sorrow"));
//		l2.add(new Lyric("waited,"));
//		l2.add(new Lyric("sorrow"));
//		l2.add(new Lyric("won"));
//
//		l3.add(new Lyric("sorrow"));
//		l3.add(new Lyric("they"));
//		l3.add(new Lyric("put"));
//		l3.add(new Lyric("me"));
//		l3.add(new Lyric("on"));
//		l3.add(new Lyric("the"));
//		l3.add(new Lyric("pill"));
//
//		l4.add(new Lyric("It's"));
//		l4.add(new Lyric("in"));
//		l4.add(new Lyric("my"));
//		l4.add(new Lyric("honey,"));
//		l4.add(new Lyric("it's"));
//		l4.add(new Lyric("in"));
//		l4.add(new Lyric("my"));
//		l4.add(new Lyric("milk"));
//		
//		stanza.add(l1);
//		stanza.add(l2);
//		stanza.add(l3);
//		stanza.add(l4);
//		stanza.add(l5);
		
		
		
		l1.add(new Lyric("It's"));
		l1.add(new Lyric("a"));
		l1.add(new Lyric("terrible"));
		l1.add(new Lyric("love"));
		l1.add(new Lyric("that"));
		l1.add(new Lyric("I'm"));
		l1.add(new Lyric("walking"));
		l1.add(new Lyric("with"));
		l1.add(new Lyric("spiders"));
		
		l2.add(new Lyric("It's"));
		l2.add(new Lyric("a"));
		l2.add(new Lyric("terrible"));
		l2.add(new Lyric("love"));
		l2.add(new Lyric("that"));
		l2.add(new Lyric("I'm"));
		l2.add(new Lyric("walking"));
		l2.add(new Lyric("in"));
		
		l3.add(new Lyric("It's"));
		l3.add(new Lyric("quiet"));
		l3.add(new Lyric("company"));
		
		stanza.add(l1);
		stanza.add(l2);
		stanza.add(l1);
		stanza.add(l2);
		stanza.add(l3);
		stanza.add(l3);
		
		//SparseSingleOrderMarkovModel<Lyric> segmentSpecificMM = mModel.get(segmentKey);
				
		return new LyricSegment(stanza);
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
		
	private static void fillAllByPart(List<TaggedWord> tagged) {		
		//I could add each word to 1 big group, or a subgroup of the tag's type
		for (int i = 0; i < tagged.size(); i++) {
			TaggedWord tw = tagged.get(i);
			String tag = tw.tag();
			ArrayList<TaggedWord> list;
			if (allWordsByPart.get(tag) == null)
				list = new ArrayList<TaggedWord>();
			else
				list = allWordsByPart.get(tag);			
			list.add(tw);
			allWordsByPart.put(tag, list);
		}
	}
	
	private static void markByPart() {
		for (Map.Entry<String, ArrayList<TaggedWord>> entry : allWordsByPart.entrySet()) {
		    String pos = entry.getKey();
		    ArrayList<TaggedWord> words = entry.getValue();
		    
		    int amountToReplace = (int) (words.size() * (replacementFrequency / 100.0)); //truncates, so it rounds odd numbers down?
		    
		    ArrayList<TaggedWord> list;
			if (markedWordsByPart.get(pos) == null)
				list = new ArrayList<TaggedWord>();
			else
				list = markedWordsByPart.get(pos);			
		    
			Set<Integer> alreadyMarked = new HashSet<Integer>();
			
			//Prevents adding the same word twice
			for (int i = 0; i < amountToReplace; i++) {
				int indexToMark = rndInt(words.size());
				while (alreadyMarked.contains(indexToMark)) {
					indexToMark = rndInt(words.size());
				}
				list.add(words.get(indexToMark));
				alreadyMarked.add(indexToMark);
			}
			
		    markedWordsByPart.put(pos, list);
		}
	}
	
	private static int rndInt(int max) {
		Random rand = new Random();
		return rand.nextInt(max);
	}
	
	private static void replace(List<TaggedWord> toMark) {
//		for (int i = 0; i < toMark.size(); i++) {
//			String tag = toMark.get(i).tag();
//			switch(tag) {
//				case "NN":
//					//add this noun to REPLACEABLE
//					//replace w/ noun
//					break;
//				case "NNS":
//					//replace w/ noun
//					break;
//				case "JJ":
//					//replace w/ adjective
//					break;
//				default:
//					//no replacement
//					break;
//			}
//		}
	}
	/*
	 * Each time a lyric is replaced, I should keep track of that lyric (or rather, that TaggedWord). Whenever that TaggedWord comes up again, with the same spelling and POS (different position), I give it the same replacement.
	 * I could save all replaced lyrics in a map: key = original word, value = replaced word. If, upon replacement, that word is already in the map, just use its value! But if not, make a new key in the map.
	 */ 
	private static void printReplacements(LyricSegment lyrics) {
		int index = 0;
		List<List<Lyric>> lines = lyrics.getLines();
		for (int i = 0; i < lines.size(); i++) {																					//for each stanza line
			List<Lyric> line = lines.get(i);
			for (int j = 0; j < line.size(); j++) {																						//for each lyric
				boolean replaced = false;
				Lyric lyric = line.get(j);
				int length = lyric.toString().length();
				
				for (Map.Entry<String, ArrayList<TaggedWord>> entry : markedWordsByPart.entrySet()) {										//for each marked POS type
				    String pos = entry.getKey();
				    ArrayList<TaggedWord> words = entry.getValue();
				    for (TaggedWord word : words) {																								//for each marked word
						if (index >= word.beginPosition() && index <= word.endPosition() && lyric.toString().equals(word.word())) {
							if (pos.equals("NN") || pos.equals("NNS") || pos.equals("VB") || pos.equals("VBG") || pos.equals("JJ"))
								System.out.print(getReplacement(pos) + " ");
							else
								//System.out.print("[replace] ");
								System.out.print(lyric.toString() + " ");
							replaced = true;
							break;
						}
				    }
				}
				
				if (!replaced) {
					System.out.print(lyric.toString() + " ");
				}
				index += lyric.toString().length() + 1;
			}
			System.out.print("\n");
			index++;
		}
	}
	
	private static String getReplacement(String pos) {
	    List<String> list = new ArrayList<String>();
		try {
			BufferedReader br;
			br = new BufferedReader(new FileReader("/Users/Benjamin/Documents/workspace/pop-star/Pop*/src/" + pos + ".txt"));
			
		    String line = br.readLine();
		
		    while (line != null) {
		    	list.add(line);
		        line = br.readLine();
		    }
		    br.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	    return list.get(rndInt(list.size()));
	}

}
























































