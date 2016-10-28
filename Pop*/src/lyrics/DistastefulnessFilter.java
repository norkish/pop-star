package lyrics;

import edu.stanford.nlp.ling.TaggedWord;

import java.util.HashMap;

public final class DistastefulnessFilter extends WordFilter {

    @Override
    public HashMap<Double,TaggedWord> filter(HashMap<Double,TaggedWord> unfilteredLyrics) {
        this.setPreFilterWords(unfilteredLyrics);
        HashMap<Double,TaggedWord> result = new HashMap<Double,TaggedWord>();

        //implement, build result

        this.setFilteredOutWords(result);
        return filteredOutWords;
    }

}

































































