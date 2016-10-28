package lyrics;

public class SimilarJob extends W2vReplacementJob {

    @Override
    public int size() {
        return 1;
    }

    @Override
    public String[] toStrArray() {
        String[] result = new String[] {oldWord.getText()};
        return result;
    }

    @Override
    public String explain() {
        return oldWord.getText() + " is similar to..";
    }


}
