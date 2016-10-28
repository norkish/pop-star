package lyrics;

import edu.stanford.nlp.ling.TaggedWord;

import java.util.Comparator;

public class SmartWord implements Comparator<SmartWord> {

    private TaggedWord stanfordWord;//TODO: change this to whatever object does POS tagging best
    private String text;
    private String pos;//TODO: change from String to enum once I know what Parts of Speech to use

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
        this.setText(this.getStanfordWord().value());
        this.setPos(this.getStanfordWord().tag());
    }

    public double getCurrentW2vDistance() {
        return this.currentW2vDistance;
    }

    public void setCurrentW2vDistance(double currentW2vDistance) {
        this.currentW2vDistance = currentW2vDistance;
    }

    public String getText() {
        return this.text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getPos() {
        return this.pos;
    }

    public void setPos(String pos) {
        this.pos = pos;
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

        if (!this.getStanfordWord().tag().equals(that.getStanfordWord().tag()))
            return false;
        if (!this.getStanfordWord().value().equals(that.getStanfordWord().value()))
            return false;
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















































































































