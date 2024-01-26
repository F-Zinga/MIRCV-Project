package unipi.mircv;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Represents a component responsible for building the inverted index for each block.
 */
public class IndexBuilder {

    //the inverted index is a hash map between a string (term) and a posting list.
    private HashMap<String, ArrayList<Posting>> indexBuilder;

    public IndexBuilder(){
        this.indexBuilder = new HashMap<>();
    }

    public HashMap<String, ArrayList<Posting>> getIndexBuilder(){ return indexBuilder; }

    /**
     * Sets the index builder with a new HashMap of terms and posting lists.
     *
     * @param indexBuilder The new index builder HashMap.
     */
    public void setIndexBuilder(HashMap<String, ArrayList<Posting>> indexBuilder){ this.indexBuilder = indexBuilder; }

    /**
     * Adds a posting to the inverted index for the specified term, document ID, and term frequency.
     *
     * @param term  The term to add the posting for.
     * @param docId The document ID associated with the posting.
     * @param freq  The term frequency in the document.
     */
    public void addPosting(String term, int docId, int freq){
        if (!indexBuilder.containsKey(term)){
            indexBuilder.put(term, new ArrayList<>());
        }
        indexBuilder.get(term).add(new Posting(docId, freq));
    }


}