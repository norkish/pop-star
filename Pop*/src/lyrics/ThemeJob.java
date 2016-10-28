package lyrics;


import java.util.ArrayList;

public class ThemeJob extends W2vJob {

    private ArrayList<SmartWord> relevantWords;

    public ArrayList<SmartWord> getRelevantWords() {
        return relevantWords;
    }

    public ThemeJob(ArrayList<SmartWord> relevantWords) {
        this.setRelevantWords(relevantWords);
    }

    private void setRelevantWords(ArrayList<SmartWord> relevantWords) {
        this.relevantWords = relevantWords;
    }

    @Override
    public String[] toStrArray() {
        String[] result = new String[relevantWords.size()];
        int i = 0;
        for (SmartWord tw : relevantWords) {
            result[i] = relevantWords.get(i).getText();
            i++;
        }
        return result;
    }

    @Override
    public String explain() {
        String[] array = this.toStrArray();
        String result = "The theme of ";
        for (int i = 0; i < array.length; i++) {
            if (i == array.length - 1)
                result += "and " + array[i];
            else
                result += array[i] + ", ";

        }
        return result + " is...";
    }

    @Override
    public int size() {
        return relevantWords.size();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o)
            return true;
        if (o == null || this.getClass() != o.getClass())
            return false;
        final ThemeJob themeJob = (ThemeJob) o;
        return this.getRelevantWords().equals(themeJob.getRelevantWords());
    }

}












































































