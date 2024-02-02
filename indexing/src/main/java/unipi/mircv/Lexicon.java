package unipi.mircv;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 * Represents the lexicon of terms used in the inverted index.
 */
public class Lexicon {

    //the lexicon is a hashmap  between a string (term) and the class term (posting list information).
    private HashMap<String, Term> lexicon = new HashMap<>();

    /**
     * Sets the lexicon with a new hashmap of terms.
     *
     * @param lexicon The new lexicon hashmap.
     */
    public void setLexicon(HashMap<String, Term> lexicon){ this.lexicon = lexicon; }
    public HashMap<String, Term> getLexicon(){ return lexicon; }

    /**
     * Adds information for a term in the lexicon. If the term already exists, it updates the upper bound for term frequency.
     *
     * @param term               The term to add or update.
     * @param offsetDocIds       Offset for document IDs in the posting list.
     * @param offsetFreq         Offset for term frequencies in the posting list.
     * @param offsetLastDocIds   Offset for the last document IDs in the posting list.
     * @param offsetSkipBlock    Offset for skip block information in the posting list.
     * @param postingListLength  Length of the posting list for the term.
     * @param termUpperBound     Upper bound for term frequency.
     */
    public void addInformation(String term, int offsetDocIds, int offsetFreq, int offsetLastDocIds, int offsetSkipBlock, int postingListLength, float termUpperBound){
        if(!lexicon.containsKey(term)){
            lexicon.put(term, new Term(offsetDocIds, offsetFreq, offsetLastDocIds, offsetSkipBlock, postingListLength, termUpperBound));
        }
        else{ //it computes the highest term frequency
            lexicon.get(term).setUpperBound(Math.max(termUpperBound, lexicon.get(term).getUpperBound()));
        }
    }

    /**
     * Returns a list of sorted terms taken from the lexicon.
     *
     * @return A sorted list of terms.
     */
    public ArrayList<String> sortLexicon(){
        ArrayList<String> sortedTerms = new ArrayList<>(lexicon.keySet());
        Collections.sort(sortedTerms);
        return sortedTerms;
    }

}
