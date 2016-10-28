package lyrics;

import java.util.HashSet;

public final class RhymeFilter extends WordFilter {

    @Override
    public HashSet<SmartWord> filter(HashSet<SmartWord> unfilteredLyrics) {
        this.setPreFilterWords(unfilteredLyrics);
        HashSet<SmartWord> result = new HashSet<SmartWord>();

        //implement, build result

        this.setFilteredOutWords(result);
        return this.filteredOutWords;
    }

}






























































