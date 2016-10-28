package lyrics;

import edu.stanford.nlp.ling.TaggedWord;

import java.util.ArrayList;
import java.util.Comparator;

import static java.awt.SystemColor.text;

public class SmartWord implements Comparator<SmartWord> {

    private TaggedWord stanfordWord;//TODO: change this to whatever object does POS tagging best
//    private String text;
//    private String pos;//TODO: change from String to enum once I know what Parts of Speech to use

    private ArrayList<String> phonemes; //TODO: change from array of strings to array of enum phonemes (or classes!)

    private int occurences; // TODO: make this more specfic to the data source

    private double currentW2vDistance;

    public SmartWord(TaggedWord stanfordWord) {
        this.setStanfordWord(stanfordWord);
        this.setCurrentW2vDistance(-1);
    }

    public TaggedWord getStanfordWord() {
        return this.stanfordWord;
    }

    public void setStanfordWord(TaggedWord stanfordWord) {
        this.setStanfordWord(stanfordWord);
    }

    public double getCurrentW2vDistance() {
        return this.currentW2vDistance;
    }

    public void setCurrentW2vDistance(double currentW2vDistance) {
        this.currentW2vDistance = currentW2vDistance;
    }

    public String getText() {
        return this.getStanfordWord().value();
    }

    public void setText(String text) {
        this.getStanfordWord().setWord(text);
    }

    public String getPos() {
        return this.getStanfordWord().tag();
    }

    public void setPos(String pos) {
        this.getStanfordWord().setTag(pos);
    }

    public boolean hasDistance() {
        if (this.currentW2vDistance == -1)
            return false;
        return true;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        final SmartWord that = (SmartWord) o;

        if (!this.getText().equals(that.getText()))
            return false;
        return this.getPos().equals(that.getPos());
        //this method doesn't consider word position nor w2v distance
    }

    @Override
    public int compare(final SmartWord o1, final SmartWord o2) {
        if (o1.getCurrentW2vDistance() == o2.getCurrentW2vDistance())
            return Integer.valueOf(o1.getText()).compareTo(Integer.valueOf(o2.getText()));
        return Double.valueOf(o1.getCurrentW2vDistance()).compareTo(Double.valueOf(o2.getCurrentW2vDistance()));
    }

    @Override
    public int hashCode() {
        int result = this.getText().hashCode();
        result = 31 * result + this.getPos().hashCode();
        return result;
    }
}







/*
Use HashSets, convert them to TreeSets whenever order matters.
Order matters when deciding the best suggestion to use.
 */


/*
TODO > Keep track of which parts of software are probabalistic
 */


/*
TODO > Follow the below guide to ensure I have ALL the complexities of parts of speech covered
NOUN,
	VERB,
	ADJECTIVE,
	ADVERB,
	PREPOSITION,
	//PRONOUN,
	NORMAL_PRONOUN,
	INTERROGATIVE_PRONOUN,
	INDEFINITE_PRONOUN,

	 * Interrogative Pronoun (who, what, where, when, why, how, how much, how many, whither, wherefore, whence)
	 * Indefinite (whoso, whosoever) So many, look them all up

//CONJUNCTION,
	COORDINATING_CONJUNCTION,
            SUBORDINATING_CONJUNCTION,
            RELATIVE_PRONOUN,

	 * Coordinating (only ones: for, and, nor, but, or, yet, so, wherefore) combines two equal elements
	 * Subordinating (although, when, before, because, though, since, inasmuch as, that) independent + (Subordinating) dependent clause, in any order
	 * Relative Pronoun (only ones: who, that, which, whom, whose, when, where)
	 *

            INTERJECTION,
            //DETERMINER,

	 * Article
	 * 	Definite
	 * 	Indefinite
	 * 		Number
	 * Numeral
	 * Possessive Adjectives (my, thy, our)
	 * 		Person
	 * 		Number
	 * Demonstrative Adjectives (this, these, that)
	 * 		Proximal Distance
	 * 		Number

            POSSESSIVE_ADJ,
            DEMONSTRATIVE_ADJ,
            NUMERAL,
            ARTICLE
 */















































































































