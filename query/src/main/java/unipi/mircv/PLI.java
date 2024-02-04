package unipi.mircv;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Represents a collection of postings in the inverted index, providing functionality to iterate through and manage
 * posting, in fact implements the interface Iterator.
 */
public class PLI implements Iterator<Posting> {

    private final String term;
    private ArrayList<Posting> postingList;
    private final ScoreFunction scoreFunction;
    private final QueryProcessor queryProcessor;
    private final String documentProcessor;

    // The current position of the iterator
    private int position;
    private boolean isFinished;

    // Constructor
public PLI(String term, ArrayList<Posting> postingList, ScoreFunction scoreFunction, QueryProcessor queryProcessor, String documentProcessor) {
        this.postingList = postingList;
        this.position = 0;
        this.scoreFunction = scoreFunction;
        this.term = term;
        this.queryProcessor = queryProcessor;
        this.documentProcessor = documentProcessor;
        this.isFinished = false;
    }

    @Override
    public boolean hasNext() {
        return position < postingList.size();
    }

    public int docid(){
        return postingList.get(position).getDocID();
    }

    /**
     * Computes the score for the current posting based on the specified score type.
     *
     * @param term      The term for which to compute the score.
     * @param scoreType The type of score computation.
     * @return The computed score.
     */
    public double score(String term,String scoreType){

        return scoreFunction.computeScore(term, postingList.get(position),scoreType);
    }

    /**
     * Checks if the iterator is finished processing postings.
     *
     * @param encodingType The encoding type for processing postings.
     * @return True if the iterator is finished, false otherwise.
     */
    public boolean isFinished(String encodingType){
        if (this.postingList.size() == 0) {
            return true;
        }
        //check if it's the last block or there are other blocks to load and then it's not "really" finished
        if (documentProcessor.equals("maxscore")) {
            if(this.isFinished) {
                return true;
            }
            if (position >= postingList.size()) {
                int docId = postingList.get(postingList.size() - 1).getDocID(); //Gets the docID of the last posting to use it to load the nextBlock (using loadNextBlock)
                HashMap<String, ArrayList<Posting>> newBlock = queryProcessor.loadNextBlock(this.term, docId,encodingType);
                if(!newBlock.containsKey(term)){
                    this.isFinished = true;
                    return true;
                }
                postingList = newBlock.get(term);
                position = 0;
                return false;
            }
            return false;

        }else{
            return position >= postingList.size();
        }
    }

    // Returns the next element in the iteration
    public Posting next() {
        return postingList.get(position++);
    }

    /**
     * Moves the iterator to the next posting with a document ID greater than or equal to the specified docID.
     *
     * @param docId        The document ID to search for.
     * @param encodingType The encoding type for processing postings.
     */
    public void nextGEQ(int docId,String encodingType) {

        //Load another block if the docID searched is not in the currentBlock
        if (docId > postingList.get(postingList.size()-1).getDocID()){
            HashMap<String,ArrayList<Posting>> newBlock =  queryProcessor.lookupDocId(this.term, docId, encodingType);
            if (newBlock.containsKey(term)){
                this.postingList = newBlock.get(term);
                this.position = 0;
            }else{
                return;
            }
        }

        while (hasNext()) {
            Posting posting = postingList.get(position);
            if (posting.getDocID() >= docId) {
                return;
            }

            next();
        }
    }

    public ArrayList<Posting> getPostingList() {
        return postingList;
    }

    public String getTerm() {
        return term;
    }
}
