package unipi.mircv;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Represents a component responsible for building the inverted index for each block.
 */
public class IndexBuilder {

    //the inverted index is a hash map between term and a posting list.
    private HashMap<String, ArrayList<Posting>> invertedIndex;

    public IndexBuilder(){
        this.invertedIndex = new HashMap<>();
    }

    public HashMap<String, ArrayList<Posting>> getInvertedIndex(){ return invertedIndex; }
    public void setInvertedIndex(HashMap<String, ArrayList<Posting>> invertedIndex){ this.invertedIndex = invertedIndex; }


    //function used to add a posting to the inverted index.
    public void addPosting(String term, int docId, int freq){
        if (!invertedIndex.containsKey(term)){
            invertedIndex.put(term, new ArrayList<>());
        }
        invertedIndex.get(term).add(new Posting(docId, freq));
    }


}