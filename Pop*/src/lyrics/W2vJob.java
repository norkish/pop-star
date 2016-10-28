package lyrics;

import java.util.HashSet;

public abstract class W2vJob {

    protected HashSet<SmartWord> w2vSuggestions;
    protected HashSet<SmartWord> unpermissibleW2vSuggestions;
    protected HashSet<SmartWord> permissibleW2vSuggestions;
    protected SmartWord chosenWord;

    public abstract int size();//number of SmartLyrics used for this type of W2v operation
    public abstract String[] toStrArray();
    public abstract String explain();

    public HashSet<SmartWord> getW2vSuggestions() {
        return w2vSuggestions;
    }

    public void setW2vSuggestions(HashSet<SmartWord> w2vSuggestions) {
        this.w2vSuggestions = w2vSuggestions;
    }

    public HashSet<SmartWord> getPermissibleW2vSuggestions() {
        return permissibleW2vSuggestions;
    }

    public void setPermissibleW2vSuggestions(HashSet<SmartWord> permissibleW2vSuggestions) {
        this.permissibleW2vSuggestions = permissibleW2vSuggestions;
    }

    public SmartWord getChosenWord() {
        return chosenWord;
    }

    public void setChosenWord(SmartWord chosenWord) {
        this.chosenWord = chosenWord;
    }

    public HashSet<SmartWord> getUnpermissibleW2vSuggestions() {
        return unpermissibleW2vSuggestions;
    }

    public void setUnpermissibleW2vSuggestions(HashSet<SmartWord> unpermissibleW2vSuggestions) {
        this.unpermissibleW2vSuggestions = unpermissibleW2vSuggestions;
    }
}























































































