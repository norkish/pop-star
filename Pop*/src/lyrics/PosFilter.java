package lyrics;

import java.util.HashSet;

public final class PosFilter extends WordFilter {

    //@Override
    public HashSet<SmartWord> filter(HashSet<SmartWord> unfilteredWords, SmartWord original) {
        this.setPreFilterWords(unfilteredWords);
        HashSet<SmartWord> result = new HashSet<SmartWord>();

        //implement, build result
        String pos = original.getPos();
        for (SmartWord sl : unfilteredWords) {
            if (pos.equals(sl.getPos()) && (pos.equals("NN") || pos.equals("NNS") || pos.equals("VB") || pos.equals("VBG") || pos.equals("JJ") || pos.equals("RB")))
                result.add(sl);
        }

        return this.setGetFilteredOutWords(result);
    }

}






































































