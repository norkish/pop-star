package lyrics;

import edu.stanford.nlp.ling.TaggedWord;

import java.util.HashMap;
import java.util.HashSet;

public final class DistastefulnessFilter extends WordFilter {

    //@Override
    public HashSet<SmartWord> filter(HashSet<SmartWord> unfilteredWords) {
        this.setPreFilterWords(unfilteredWords);
        HashSet<SmartWord> result = new HashSet<SmartWord>();

        //implement, build result

        return this.setGetFilteredOutWords(result);
    }

}

































































