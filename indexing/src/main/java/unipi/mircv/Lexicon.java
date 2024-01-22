package unipi.mircv;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 * Represents the lexicon of terms used in the inverted index. Extends HashMap to provide a mapping from terms to their corresponding Term objects.
 */
public class Lexicon {

    //the lexicon is represented as a hashmap  between a term and the posting list information.
    private HashMap<String, Term> lexicon = new HashMap<>();

    public class Term {
        public int offsetDocId;
        public int offsetFreq;
        public int offsetLastDocIds;
        public int offsetSkipBlock;
        public int postingListLength;
        public float termUpperBound;

        public Term(int offsetDocId, int offsetFreq, int offsetLastDocIds, int offsetSkipBlock, int postingListLength, float termUpperBound) {
            this.offsetDocId = offsetDocId;
            this.offsetFreq = offsetFreq;
            this.offsetLastDocIds = offsetLastDocIds;
            this.offsetSkipBlock = offsetSkipBlock;
            this.postingListLength = postingListLength;
            this.termUpperBound = termUpperBound;
        }

        public float getTermUpperBound() {
            return termUpperBound;
        }

        public void setTermUpperBound(float termUpperBound) {
            this.termUpperBound = termUpperBound;
        }


        public int getPostingListLength() {
            return postingListLength;
        }

        public void setPostingListLength(int postingListLength) {
            this.postingListLength = postingListLength;
        }

        public int getOffsetDocId() {
            return offsetDocId;
        }

        public void setOffsetDocId(int offsetDocId) {
            this.offsetDocId = offsetDocId;
        }

        public int getOffsetFreq() {
            return offsetFreq;
        }

        public void setOffsetFreq(int offsetFreq) {
            this.offsetFreq = offsetFreq;
        }

        public int getOffsetSkipBlock() {
            return offsetSkipBlock;
        }

        public void setOffsetSkipBlock(int offsetSkipBlock) {
            this.offsetSkipBlock = offsetSkipBlock;
        }

        public int getOffsetLastDocIds() {
            return offsetLastDocIds;
        }

        public void setOffsetLastDocIds(int offsetDocId) {
            this.offsetLastDocIds = offsetLastDocIds;
        }

        @Override
        public String toString() {
            return offsetDocId + " " + offsetFreq + " " + offsetLastDocIds + " "
                    + offsetSkipBlock + " " + postingListLength + " " + termUpperBound;
        }
    }
    public void setLexicon(HashMap<String, Term> lexicon){ this.lexicon = lexicon; }
    public HashMap<String, Term> getLexicon(){ return lexicon; }

    public void addInformation(String term, int offsetDocIds, int offsetFreq, int offsetLastDocIds, int offsetSkipBlock, int postingListLength, float termUpperBound){
        if(!lexicon.containsKey(term)){
            lexicon.put(term, new Term(offsetDocIds, offsetFreq, offsetLastDocIds, offsetSkipBlock, postingListLength, termUpperBound));
        }
        else{ //it computes the highest term frequency
            lexicon.get(term).setTermUpperBound(Math.max(termUpperBound, lexicon.get(term).getTermUpperBound()));
        }
    }

    //function that returns a list of sorted terms taken from the lexicon.
    public ArrayList<String> sortLexicon(){
        ArrayList<String> sortedTerms = new ArrayList<>(lexicon.keySet());
        Collections.sort(sortedTerms);
        return sortedTerms;
    }

}
