package lyrics;

import java.util.HashSet;

public final class FrequencyFilter extends WordFilter {

    //@Override
    public HashSet<SmartWord> filter(HashSet<SmartWord> unfilteredLyrics) {
        this.setPreFilterWords(unfilteredLyrics);
        HashSet<SmartWord> result = new HashSet<SmartWord>();

        //implement, build result

        return this.setGetFilteredOutWords(result);
    }

}






























































