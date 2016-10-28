package lyrics;

import java.util.HashSet;

public final class PosFilter extends WordFilter {

    //@Override
    public HashSet<SmartWord> filter(HashSet<SmartWord> unfilteredLyrics, SmartWord original) {
        this.setPreFilterWords(unfilteredLyrics);
        HashSet<SmartWord> result = new HashSet<SmartWord>();

        //implement, build result
        String pos = original.getPos();
        for (SmartWord sl : unfilteredLyrics) {
            if (pos.equals(sl.getPos()) && (pos.equals("NN") || pos.equals("NNS") || pos.equals("VB") || pos.equals("VBG") || pos.equals("JJ") || pos.equals("RB")))
                result.add(sl);
        }

        this.setFilteredOutWords(result);
        return this.filteredOutWords;
    }

}






































































