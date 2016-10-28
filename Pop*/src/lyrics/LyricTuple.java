package lyrics;

public class LyricTuple {
	
	private SmartWord originalWord;
	private SmartWord newWord;
	private boolean marked = false;

	public LyricTuple() {}

	public LyricTuple(SmartWord taggedWord) {
		this.setOriginalWord(taggedWord);
	}
	
	public SmartWord getOriginalWord() {
		return originalWord;
	}
	
	public void setOriginalWord(SmartWord originalWord) {
		this.originalWord = originalWord;
	}
	
	public SmartWord getNewWord() {
		return newWord;
	}

	public void setNewWord(SmartWord newWord) {
		this.newWord = newWord;
	}

	public boolean isMarked() {
		return marked;
	}
	
	public void mark() {
		this.marked = true;
	}
	
	public boolean isNewline() {
		return false;
	}
	
	public String toString(LyricVersion lv) {
		if (originalWord == null && isNewline()) 
			return "\n";
		if (lv == LyricVersion.ORIGINAL)
			return originalWord.getText();
		else if (lv == LyricVersion.NEW)
			return newWord.toString();
		return null;
	}

	public SmartWord getWord(LyricVersion lv) {
        if (lv == LyricVersion.ORIGINAL)
            return this.originalWord;
        if (lv == LyricVersion.NEW)
            return this.newWord;
        return null;
    }

	@Override
	public String toString() {
		if (originalWord == null && isNewline()) 
			return "\n";
		if (newWord == null || !isMarked())
			return originalWord.getText();
		return newWord.getText();
	}
	
	@Override
	public boolean equals(final Object obj) {
        if (this == obj)
            return true;
		if (obj == null)
			return false;
        final LyricTuple t = (LyricTuple) obj;
		if (this.marked != t.isMarked())
			return false;
		if (this.originalWord.getStanfordWord().beginPosition() != t.getOriginalWord().getStanfordWord().beginPosition())
			return false;
		if (this.originalWord.getStanfordWord().endPosition() != t.getOriginalWord().getStanfordWord().endPosition())
			return false;
        if (!this.originalWord.equals(t.getOriginalWord()))
            return false;
        if (!this.newWord.equals((t.getNewWord())))
            return false;
		return true;
        //Checks the position of original words
	}
		
}




















































































































