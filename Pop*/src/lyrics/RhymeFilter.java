package lyrics;

import java.util.HashSet;

public final class RhymeFilter extends WordFilter {

    //@Override
    public HashSet<SmartWord> filter(HashSet<SmartWord> unfilteredWords, SmartWord original) {
        this.setPreFilterWords(unfilteredWords);
        HashSet<SmartWord> result = new HashSet<SmartWord>();

        //implement, build result

        return this.setGetFilteredOutWords(result);
    }

}






























































