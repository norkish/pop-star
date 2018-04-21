package semantic;

import java.util.List;
import java.util.Properties;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreSentence;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.trees.Tree;

public class BasicPipelineExample {

  public static String text = "Horse; Hat; Trail; The cowboy rides. " +
      "In 2017, he went to Paris, France in the summer. " +
      "His flight left at 3:00pm on July 10th, 2017. " +
      "After eating some escargot for the first time, Joe said, \"That was delicious!\" " +
      "He sent a postcard to his sister Jane Smith. " +
      "After hearing about Joe's trip, Jane decided she might go to France one day.";

  public static void main(String[] args) {
    // set up pipeline properties
    Properties props = new Properties();
	props.put("threads", 16);
    // set the list of annotators to run
    props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner,parse,depparse,coref,sentiment");
    // set a property for an annotator, in this case the coref annotator is being set to use the neural algorithm
    props.setProperty("coref.algorithm", "neural");
    // build pipeline
    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
    // create a document object
    CoreDocument document = new CoreDocument(text);
    // annnotate the document
    pipeline.annotate(document);
    // examples

    // 10th token of the document
    CoreLabel token = document.tokens().get(10);
    System.out.println("Example: token");
    System.out.println(token);
    System.out.println();

    // text of the first sentence
    String sentenceText = document.sentences().get(0).text();
    System.out.println("Example: sentence");
    System.out.println(sentenceText);
    System.out.println();

    // second sentence
    CoreSentence sentence = document.sentences().get(0);

    // list of the part-of-speech tags for the second sentence
    List<String> posTags = sentence.posTags();
    System.out.println("Example: pos tags");
    System.out.println(posTags);
    System.out.println();

    // list of the ner tags for the second sentence
    List<String> nerTags = sentence.nerTags();
    System.out.println("Example: ner tags");
    System.out.println(nerTags);
    System.out.println();

    // constituency parse for the second sentence
    Tree constituencyParse = sentence.constituencyParse();
    System.out.println("Example: constituency parse");
    System.out.println(constituencyParse);
    System.out.println();

    // dependency parse for the second sentence
    SemanticGraph dependencyParse = sentence.dependencyParse();
    System.out.println("Example: dependency parse");
    System.out.println(dependencyParse);
    System.out.println();

    // sentiments found in fifth sentence
    String sentiments =
        document.sentences().get(4).sentiment();
    System.out.println("Example: sentiments");
    System.out.println(sentiments);
    System.out.println();

    // sentiments found in fifth sentence
    Tree sentimentTree =
    		document.sentences().get(4).sentimentTree();
    System.out.println("Example: sentimentTree");
    System.out.println(sentimentTree);
    System.out.println();

  }

}
