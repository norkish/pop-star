package lyrics;

import java.util.*;

public class LyricFilterCommander {

    private ArrayList<WordFilter> activeFilterList;
    private final int n_filterTypes = 6;

    public LyricFilterCommander(Class... filtersToActivate) {
        this.setupActiveFilterList(filtersToActivate);
    }

    public HashSet<SmartWord> filterSuggestions(HashSet<SmartWord> unfilteredWords) {
        return this.filter(unfilteredWords);
    }

    public HashSet<SmartWord> filterSuggestions(HashSet<SmartWord> unfilteredWords, Class... filtersToActivate) {
        setupActiveFilterList(filtersToActivate);
        return this.filter(unfilteredWords);
    }

    //TODO > Find better way than passing in a class for this method
    private void setupActiveFilterList(Class... filtersToActivate) {
        ArrayList<WordFilter> filters = new ArrayList<WordFilter>();
        for (int i = 0; i < filtersToActivate.length; i++) {
            switch (filtersToActivate[i].getSimpleName()) {
                case "DictionaryFilter":
                    filters.add(new DictionaryFilter());
                    break;
                case "FrequencyFilter":
                    filters.add(new FrequencyFilter());
                    break;
                case "BallparkFilter":
                    filters.add(new BallparkFilter());
                    break;
                case "PosFilter":
                    filters.add(new PosFilter());
                    break;
                case "DistastefulnessFilter":
                    filters.add(new DistastefulnessFilter());
                    break;
                case "RhymeFilter":
                    filters.add(new RhymeFilter());
                    break;
                default:
                    System.out.println("ERROR, DIDN'T RECOGNIZE FILTER NAME!");
                    break;
            }
        }
        this.setActiveFilterList(filters);
    }

    private HashSet<SmartWord> filter(HashSet<SmartWord> unfilteredWords) {
        ArrayList<HashSet<SmartWord>> filteredSets = new ArrayList<HashSet<SmartWord>>();
        for (int i = 0; i < activeFilterList.size(); i++) {
            HashSet tempSet = activeFilterList.get(i).filter(unfilteredWords);
            filteredSets.add(tempSet);
        }
        HashSet<SmartWord> intersection = new HashSet<SmartWord>();
        intersection.retainAll(filteredSets);
        return intersection;
    }

    public ArrayList<WordFilter> getActiveFilterList() {
        return this.activeFilterList;
    }

    public void setActiveFilterList(ArrayList<WordFilter> activeFilterList) {
        this.activeFilterList = activeFilterList;
    }
}


/*
TODO > Change ArrayList<TaggedWord> to HashSet<TaggedWord>, figure out how to use comparators
 */





/*
Similar to villian
Original suggestions: bretch, baddie, terrible
POS filter: returns bretch, baddie
Dirty word filter: baddie, terrible

Intersect sets for baddie

Set<String> s1;
Set<String> s2;
Set<String> s3;
Set<String> intersection = new HashSet<String>();
ArrayList<Set<String>> individualFilteredSuggestions = new ArrayList<Set<String>>();
individualFilteredSuggestions.add(s1);
individualFilteredSuggestions.add(s2);
individualFilteredSuggestions.add(s3);

intersection.retainAll(individualFilteredSuggestions);
 */


/*
The way to store uncosined lyrics
Set:
    TaggedWord
 */


/*
The way to store cosined lyrics
Map:
    Key:    cosine distance
    Value:  TaggedWord
 */













































































































