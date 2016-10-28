package lyrics;

import java.util.HashSet;

public abstract class WordFilter {

    protected HashSet<SmartWord> preFilterWords;
    protected HashSet<SmartWord> filteredOutWords;
    protected HashSet<SmartWord> remainingWords;

    //public abstract HashSet<SmartWord> filter(HashSet<SmartWord> preFilterWords);

    public HashSet<SmartWord> getPreFilterWords() {
        return preFilterWords;
    }

    public void setPreFilterWords(HashSet<SmartWord> preFilterWords) {
        this.preFilterWords = preFilterWords;
    }

    private HashSet<SmartWord> getFilteredOutWords() {
        return filteredOutWords;
    }

    private void setFilteredOutWords(HashSet<SmartWord> filteredOutWords) {
        this.filteredOutWords = filteredOutWords;
    }

    public HashSet<SmartWord> setGetFilteredOutWords(HashSet<SmartWord> filteredOutWords) {
        this.setFilteredOutWords(filteredOutWords);
        return this.getFilteredOutWords();
    }

    public HashSet<SmartWord> getRemainingWords() {
        return remainingWords;
    }

    public void setRemainingWords(HashSet<SmartWord> remainingWords) {
        this.remainingWords = remainingWords;
    }
}




/*
TODO > (maybe) Make sure only WordFilterCommander can access WordFilter methods
 */







































































