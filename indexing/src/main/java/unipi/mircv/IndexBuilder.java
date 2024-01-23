package unipi.mircv;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Represents a component responsible for building the inverted index for each block.
 */
public class IndexBuilder {

    //the inverted index is a hash map between term and a posting list.
    private HashMap<String, ArrayList<Posting>> indexBuilder;

    public IndexBuilder(){
        this.indexBuilder = new HashMap<>();
    }

    public HashMap<String, ArrayList<Posting>> getIndexBuilder(){ return indexBuilder; }
    public void setIndexBuilder(HashMap<String, ArrayList<Posting>> indexBuilder){ this.indexBuilder = indexBuilder; }


    //function used to add a posting to the inverted index.
    public void addPosting(String term, int docId, int freq){
        if (!indexBuilder.containsKey(term)){
            indexBuilder.put(term, new ArrayList<>());
        }
        indexBuilder.get(term).add(new Posting(docId, freq));
    }


}