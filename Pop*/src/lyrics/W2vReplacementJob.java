package lyrics;

import java.util.Comparator;

public abstract class W2vReplacementJob extends W2vJob implements Comparator<W2vReplacementJob> {

    protected SmartWord oldWord;

    @Override
    public int compare(W2vReplacementJob o1, W2vReplacementJob o2) {
        return Integer.valueOf(o1.getOldWord().getStanfordWord().beginPosition()).compareTo(Integer.valueOf(o2.getOldWord().getStanfordWord().beginPosition()));
        // W2v single replacement jobs are sorted by the order that the original job was found in the original song lyrics.
    }

    public SmartWord getOldWord() {
        return oldWord;
    }

    public void setOldWord(SmartWord oldWord) {
        this.oldWord = oldWord;
    }
}




































