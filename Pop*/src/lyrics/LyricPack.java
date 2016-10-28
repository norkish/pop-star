package lyrics;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;


import edu.stanford.nlp.ling.TaggedWord;

import static com.sun.tools.internal.xjc.reader.Ring.add;

public class LyricPack {

	private ArrayList<ArrayList<SmartWord>> taggedWords;
	private HashMap<String, ArrayList<LyricTuple>> partmap = new HashMap<String, ArrayList<LyricTuple>>();
	private ArrayList<LyricTuple> tupleList = new ArrayList<>();
	
	
	//could have autopilot method that calls all functions from inside the LyricPack
	
	public LyricPack(ArrayList<ArrayList<SmartWord>> taggedWords) {
		this.setSmartLyrics(taggedWords);
	}
			
	//periods mean new line
	public void fillPartmapAndList() {		
		for (int i = 0; i < taggedWords.size(); i++) {
			for (int j = 0; j < taggedWords.get(i).size(); j++) {
                SmartWord tw = taggedWords.get(i).get(j);
				String tag = tw.getPos();
				if (tag.equals(".")) {
					tupleList.add(new Newline());
				}
				else {
					LyricTuple tuple = new LyricTuple(tw);
					ArrayList<LyricTuple> tuples;
					if (partmap.get(tag) == null)
						tuples = new ArrayList<LyricTuple>();
					else
						tuples = partmap.get(tag);			
					tuples.add(tuple);
					partmap.put(tag, tuples);
					
					tupleList.add(tuple);
				}
			}
		}
	}
		
	public void markPartmapForReplacements(double replacementFrequency) {
		for (Map.Entry<String, ArrayList<LyricTuple>> entry : partmap.entrySet()) {
		    String pos = entry.getKey();
		    ArrayList<LyricTuple> words = entry.getValue();
		    
		    int amountToReplace = (int) (words.size() * (replacementFrequency / 100.0)); //truncates, so it rounds odd numbers down
		    
		    ArrayList<LyricTuple> tuples;
			if (partmap.get(pos) == null)
				tuples = new ArrayList<LyricTuple>();
			else
				tuples = partmap.get(pos);			
		    
			for (int i = 0; i < amountToReplace; i++) {
				int indexToMark = rndInt(words.size());
				while (tuples.get(indexToMark).isMarked()) {
					indexToMark = rndInt(words.size());
				}
				tuples.get(i).mark();
			}
		}
	}
	
	public void replaceMarked() {
		for (int i = 0; i < tupleList.size(); i++) {
			LyricTuple tuple = tupleList.get(i);
			if (tuple.isMarked()) {
				String pos = tuple.getOriginalWord().getPos();
				boolean isRepeat = false;
				for (int j = 0; j < tupleList.size(); j++) {
					if (j != i && tupleList.get(j).getNewWord() != null && tuple.getOriginalWord().getText().equals(tupleList.get(j).getOriginalWord().getText())) {
						isRepeat = true;
						tuple.setNewWord(tupleList.get(j).getNewWord());
						break;
					}
				}
				if (!isRepeat && (pos.equals("NN") || pos.equals("NNS") || pos.equals("VB") || pos.equals("VBG") || pos.equals("JJ"))) {
                    SmartWord newWord = getMockRndReplacementWord(pos);
					while (!isNew(newWord)) {
						newWord = getMockRndReplacementWord(pos);
					}

					tupleList.get(i).setNewWord(newWord);
				}
			}
		}
	}

	public void replaceMarkedW2vAnalogy() {

        HashSet<W2vJob> word2vecJobs = new HashSet<W2vJob>();
        LyricFilterCommander filter = new LyricFilterCommander();

        //Add a theme-finding job first
        ArrayList<SmartWord> markedWords = new ArrayList<SmartWord>();//TODO find better way to do this
        for (int i = 0; i < tupleList.size(); i++) {
            LyricTuple tuple1 = tupleList.get(i);
            if (tuple1.isMarked())
                markedWords.add(tuple1.getOriginalWord());
        }
        word2vecJobs.add(new ThemeJob(markedWords));
//        SmartWord oldTheme = new SmartWord(new TaggedWord("NULL", "NN"));    TODO: uncomment when w2v theme finding works
        SmartWord oldTheme = new SmartWord(new TaggedWord("depression", "NN"));
        SmartWord newTheme = this.generateNewTheme();

        for (int i = 0; i < tupleList.size(); i++) {
			LyricTuple tuple1 = tupleList.get(i);
			if (tuple1.isMarked()) {
                //if it's marked, make an AnalogyJob and put it in the W2vJob array.
                AnalogyJob tempAnalogy = new AnalogyJob(oldTheme, newTheme, tuple1.getOriginalWord());

                // ensure no duplicate analogy jobs are added, add new job
                word2vecJobs.add(tempAnalogy);
            }

            // after the AnalogyJob array is full, load W2v and run each W2vJob operation on it.
            W2vCommander w2v = new W2vCommander();
            w2v.setupAll("models/vectors-phrase.bin", word2vecJobs);

            // Return 1 suggestion list for each W2vJob: ArrayList<HashSet<SmartWord>> w2vSuggestions.
            HashSet<W2vJob> w2vResults = w2v.runAll();

            // Filter each set of suggestions with LyricFilterCommander.
            HashSet<W2vJob> filteredW2vResults = filter.filterSuggestions(w2vResults);

            // Return 1 suggestion SmartWord for each filtered suggestion list.
            HashSet<W2vJob> selectedW2vResults = new HashSet<SmartWord>();
            for (SmartWord sl : filteredW2vResults) {
                selectedW2vResults.add(this.getTopSuggestion(filteredW2vResults.get(i)));
            }
            final HashSet<SmartWord> finalReplacements = selectedW2vResults;

            // For each marked tuple, (later combine LyricTuples and jobs) find its corresponding job, grab the new word, and update the tuple.

        }
	}

	private SmartWord generateNewTheme() {
        // TODO > implement this
        return new SmartWord(new TaggedWord("excitement", "NN"));
    }

	private SmartWord getTopSuggestion(HashSet<SmartWord> hashSet) {
        //to be run after filtering
        TreeSet<SmartWord> treeSet = new TreeSet<SmartWord>(hashSet);

        //TODO > Should this wipe the cosine distance? I think so, it shouldn't matter after it's chosen.
        //treeSet.first().setCurrentW2vDistance(-1);

        return treeSet.first();
    }

	private boolean isNew(String s) {
		for (int i = 0; i < tupleList.size(); i++) {
			if (tupleList.get(i) == null || tupleList.get(i).getNewWord() == null) {
				// do nothing
			}
			else if (tupleList.get(i).getNewWord().equals(s))
				return false;
		}
		return true;
	}

    private boolean isNew(SmartWord sl) {
        if (sl == null || sl.getText() == null || sl.getText() == "") {
            System.out.println("Error: empty SmartWord passed into isNew()");
            return true;
        }
        if (tupleList.contains(sl))
            return false;
        return true;
    }

    private SmartWord getMockRndReplacementWord(String pos) {
		HashSet<SmartWord> set = getSmartLyricsByPart(pos);
		return chooseReplacement(set);
	}

    private SmartWord getW2vRndReplacementWord(ArrayList<SmartWord> list) {
        HashSet<SmartWord> set = new HashSet<SmartWord>();
        for (SmartWord tw : list)
            set.add(tw);
        return chooseReplacement(set);
    }

//	private HashSet<String> constrainByRhyme(String rhyme) {
//		
//	}
	
	
	private HashSet<String> getWordsByPart(String pos) {
		HashSet<String> set = new HashSet<String>();
		try {
			BufferedReader br;
			br = new BufferedReader(new FileReader("/Users/Benjamin/Documents/workspace/pop-star/Pop*/src/" + pos + ".txt"));
			
		    String line = br.readLine();
		
		    while (line != null) {
		    	set.add(line);
		        line = br.readLine();
		    }
		    br.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		return set;
	}

    private HashSet<SmartWord> getSmartLyricsByPart(String pos) {
        HashSet<SmartWord> set = new HashSet<SmartWord>();
        try {
            BufferedReader br;
            br = new BufferedReader(new FileReader("/Users/Benjamin/Documents/workspace/pop-star/Pop*/src/" + pos + ".txt"));

            String line = br.readLine();

            while (line != null) {
                SmartWord taggedLine = sloppy(line).get(0);
                set.add(taggedLine);
                line = br.readLine();
            }
            br.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return set;
    }

    public static HashSet<SmartWord> stringsToSmartLyrics_Stanford(ArrayList<String> array) {
        // TODO: Do something useful with cosine distance instead of trashing it here
        // TODO: FIX!! THIS IS SLOPPY. Uses Stanford parser inefficiently to tag words. Change LyricSegment class.
        // TODO: is it best to return each word2vec result as its own line of a stanza as it's done here?
        // It removes any grammatical context, which is good.


//        // 2 seconds on normal-already written
//        // 43 seconds on normal-not yet written (had errors)
//        // 47 seconds on alternate-already written
        // 91 seconds on alternate-not yet written (NO ERRORS)
        List<List<Lyric>> lll = new ArrayList<List<Lyric>>();
        List<Lyric> ll;
        Lyric l;
        for (int i = 0; i < array.size(); i += 2) {
            l = new Lyric(array.get(i));
            ll = new ArrayList<Lyric>();
            ll.add(l);
            lll.add(ll);
        }
        LyricSegment ls = new LyricSegment(lll);
        StanfordPosTagger spt = new StanfordPosTagger(ls);
        HashSet<SmartWord> tagged = spt.getPosEzSmart();
        return tagged;
    }

    private SmartWord chooseReplacement(HashSet<SmartWord> set) {
		int size = set.size();
		if (size == 0) {
            System.out.println("Set is empty!");
            return null;
        }
		int item = new Random().nextInt(size);
		int i = 0;
        SmartWord result = new SmartWord();
		for (SmartWord tw : set) {
		    if (i == item)
		    	result = tw;
		    i++;
		}
		return result;
	}
	
	public void addReplacements() {
		for (Map.Entry<String, ArrayList<LyricTuple>> entry : partmap.entrySet()) {
		    String pos = entry.getKey();
		    ArrayList<LyricTuple> tuples = entry.getValue();
		    for (LyricTuple t : tuples) {
		    	if (t.isMarked()) {
		    		if (pos.equals("NN") || pos.equals("NNS") || pos.equals("VB") || pos.equals("VBG") || pos.equals("JJ")) //TODO fix this
		    			t.setNewWord(getMockRndReplacementWord(pos));
		    	}
		    }
		}
	}
	
	public void print() {
		System.out.print(listToString());
	}
		
	private String listToString() {
		StringBuilder sb = new StringBuilder();
		boolean deleteSpace = true;
		for (int i = 0; i < tupleList.size(); i++) {
			LyricTuple tuple = tupleList.get(i);
			if (sb.toString().endsWith("\n") || tuple.toString().charAt(0) == ('\''))
				deleteSpace = true;
			
			if (deleteSpace || tuple.toString().equals(","))
				sb.append(tuple.toString());
			else
				sb.append(" " + tuple.toString());
			deleteSpace = false;
		}
		return sb.toString();
	}
		
	private int rndInt(int max) {
		Random rand = new Random();
		return rand.nextInt(max);
	}

	public ArrayList<ArrayList<SmartWord>> getSmartLyrics() {
		return taggedWords;
	}

	public void setSmartLyrics(ArrayList<ArrayList<SmartWord>> taggedWords) {
		this.taggedWords = taggedWords;
	}

	public HashMap<String, ArrayList<LyricTuple>> getPartmap() {
		return partmap;
	}

	public void setPartmap(HashMap<String, ArrayList<LyricTuple>> partmap) {
		this.partmap = partmap;
	}

	public ArrayList<LyricTuple> getTupleList() {
		return tupleList;
	}

	public void setTupleList(ArrayList<LyricTuple> tupleList) {
		this.tupleList = tupleList;
	}

	private ArrayList<TaggedWord> sloppy(String newWord) {
        // TODO: FIX!! THIS IS SLOPPY. Uses Stanford parser inefficiently to tag word. Change LyricSegment class.
        List<List<Lyric>> lll = new ArrayList<List<Lyric>>();
        List<Lyric> ll = new ArrayList<Lyric>();
        Lyric l = new Lyric(newWord);
        ll.add(l);
        lll.add(ll);
        LyricSegment ls = new LyricSegment(lll);
        StanfordPosTagger spt = new StanfordPosTagger(ls);
        ArrayList<TaggedWord> tagged = spt.getPosEz();
        return tagged;
    }

}



/*
 * Each time a lyric is replaced, I should keep track of that lyric (or rather, that TaggedWord). Whenever that TaggedWord comes up again, with the same spelling and POS (different position), I give it the same replacement.
 * I could save all replaced lyrics in a map: key = original word, value = replaced word. If, upon replacement, that word is already in the map, just use its value! But if not, make a new key in the map.
 */ 

//private Map<SegmentType, Map<Integer, BackedDistribution<ConstraintBlock<Lyric>>>> lyricConstraintsDistribution = DataLoader.getLyricConstraintsDistribution();
//private Map<SegmentType, SparseSingleOrderMarkovModel<Lyric>> mModel = DataLoader.getLyricMarkovModel();



















































































