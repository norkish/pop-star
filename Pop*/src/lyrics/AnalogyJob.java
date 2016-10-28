package lyrics;

public class AnalogyJob extends W2vReplacementJob {

    private SmartWord oldTheme;
    private SmartWord newTheme;

    public AnalogyJob(SmartWord oldTheme, SmartWord newTheme, SmartWord oldWord) {
        this.setOldTheme(oldTheme);
        this.setNewTheme(newTheme);
        this.setOldWord(oldWord);
    }

    @Override
    public String[] toStrArray() {
        String[] result = new String[3];
        result[0] = oldTheme.getText();
        result[1] = newTheme.getText();
        result[2] = oldWord.getText();
        return result;
    }

    @Override
    public String explain() {
        return oldTheme.getText() + " is to " + newTheme.getText() + " as " + oldWord.getText() + " is to...";
    }

    public SmartWord getOldTheme() {
        return oldTheme;
    }

    public SmartWord getNewTheme() {
        return newTheme;
    }

    public void setOldTheme(SmartWord oldTheme) {
        this.oldTheme = oldTheme;
    }

    public void setNewTheme(SmartWord newTheme) {
        this.newTheme = newTheme;
    }

    @Override
    public boolean isAnalogyJob() {
        return true;
    }

    @Override
    public boolean isThemeJob() {
        return false;
    }

    @Override
    public boolean isSimilarJob() {
        return false;
    }

    @Override
    public int size() {
        return 3;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || this.getClass() != o.getClass())
            return false;
        final AnalogyJob that = (AnalogyJob) o;
        if (!this.getOldTheme().equals(that.getOldTheme()))
            return false;
        if (!this.getNewTheme().equals(that.getNewTheme()))
            return false;
        return this.getOldWord().equals(that.getOldWord());
    }

}






























