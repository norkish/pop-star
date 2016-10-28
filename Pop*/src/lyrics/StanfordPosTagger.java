package lyrics;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import edu.stanford.nlp.trees.GrammaticalStructure;

public class StanfordPosTagger {

	ArrayList<ArrayList<TaggedWord>> taggedWords = new ArrayList<ArrayList<TaggedWord>>();
	ArrayList<GrammaticalStructure> gs = new ArrayList<GrammaticalStructure>();
	
	public StanfordPosTagger(LyricSegment lyrics) {
		this.tagPoS(lyrics);
	}

	public StanfordPosTagger(String lyric) {
		this.tagPoS(lyric);
	}
	
	public ArrayList<ArrayList<TaggedWord>> getTaggedWords() {
		return taggedWords;
	}
	
	public ArrayList<GrammaticalStructure> getGrammaticalStructure() {
		return gs;
	}
	
	public ArrayList<TaggedWord> getPosEz() {
		ArrayList<TaggedWord> result = new ArrayList<TaggedWord>();
		for (int i = 0; i < taggedWords.size(); i++) {
			result.addAll(taggedWords.get(i));
		}
		return result;
	}

    public HashSet<SmartWord> getPosEzSmart() {
        HashSet<SmartWord> result = new HashSet<SmartWord>();
        for (int i = 0; i < taggedWords.size(); i++) {
            for (int j = 0; j < taggedWords.get(i).size(); j++) {
                result.add(new SmartWord(taggedWords.get(i).get(j)));
            }
        }
        return result;
    }


    private TaggedWord tagPoS(String lyric) {
		String taggerPath = "/Applications/Cellar/stanford-parser/3.6.0/libexec/models/wsj-0-18-bidirectional-nodistsim.tagger";
		MaxentTagger tagger = new MaxentTagger(taggerPath);
		TaggedWord tw = new TaggedWord(lyric);
		return tw;
	}

	private void tagPoS(LyricSegment lyrics) {
	    //String modelPath = DependencyParser.DEFAULT_MODEL;
	    String taggerPath = "/Applications/Cellar/stanford-parser/3.6.0/libexec/models/wsj-0-18-bidirectional-nodistsim.tagger";
	    
//	    for (int argIndex = 0; argIndex < args.length; ) {
//	      switch (args[argIndex]) {
//	        case "-tagger":
//	          taggerPath = args[argIndex + 1];
//	          argIndex += 2;
//	          break;
//	        case "-model":
//	          modelPath = args[argIndex + 1];
//	          argIndex += 2;
//	          break;
//	        default:
//	          throw new RuntimeException("Unknown argument " + args[argIndex]);
//	      }
//	    }

	    //String text = "I can almost always tell when movies use fake dinosaurs.";


        //THIS CLASS IS THE ROOT OF NO-ANALOGY ERRORS


//          MUCH SLOWER THAN BELOW VERSION
//        for (int i = 0; i < lyrics.getLines().size(); i++) {
//            String text = "";
//            for (int j = 0; j < lyrics.getLines().get(i).size(); j++) {
//                text += lyrics.getLines().get(i).get(j) + " ";
//            }
//            text += ".";
//            DocumentPreprocessor tokenizer = new DocumentPreprocessor(new StringReader(text));
//            MaxentTagger tagger = new MaxentTagger(taggerPath);
//            for (List<HasWord> sentence : tokenizer) {
//                List<TaggedWord> tagged = tagger.tagSentence(sentence);
//                this.taggedWords.add((ArrayList<TaggedWord>) tagged);
//            }
//        }






            String text = "";

            for (int i = 0; i < lyrics.getLines().size(); i++) {
                for (int j = 0; j < lyrics.getLines().get(i).size(); j++) {
                    text += lyrics.getLines().get(i).get(j) + " ";
                }
                text += ".";
            }

            MaxentTagger tagger = new MaxentTagger(taggerPath);
            //DependencyParser parser = DependencyParser.loadFromModelFile(modelPath);

            DocumentPreprocessor tokenizer = new DocumentPreprocessor(new StringReader(text));
            for (List<HasWord> sentence : tokenizer) {
                List<TaggedWord> tagged = tagger.tagSentence(sentence);
                this.taggedWords.add((ArrayList<TaggedWord>) tagged);
                //GrammaticalStructure gs1 = parser.predict(tagged);
//                ArrayList<TaggedWord> periodlessTaggedSentence = new ArrayList();
//                for (int i = 0; i < tagged.size(); i++) {
//                    if (!tagged.get(i).value().equals(".")) {
//                        periodlessTaggedSentence.add(tagged.get(i));
//                    }
//                }
//                this.taggedWords.add(periodlessTaggedSentence);

                //this.gs.add(gs1);

                // Print typed dependencies
                // System.err.println(gs);
            }


	    }
}
	

















































































