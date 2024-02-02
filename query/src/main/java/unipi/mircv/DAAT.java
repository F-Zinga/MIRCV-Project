package unipi.mircv;


import java.util.ArrayList;
import java.util.HashMap;

/**
    * Represents a Document-At-A-Time (DAAT) query processing strategy, handling conjunctive or disjunctive queries.
   */
public class DAAT {
    String queryType; // type of process query: conjunctive or disjunctive
    QueryProcessor queryProcessor; // Processor for handling queries and lexicon information

    /**
     * Constructs a DAAT instance with the specified query type and query processor.
     *
     * @param queryType The type of query, either "conjunctive" or "disjunctive."
     * @param queryProcessor The QueryProcessor responsible for managing queries and lexicon information.
     */
    public DAAT(String queryType, QueryProcessor queryProcessor){
        this.queryType = queryType;
        this.queryProcessor = queryProcessor;
    }

    /**
     * Scores documents based on the given query terms, posting lists, score function, k value, encoding type, and score type.
     *
     * @param queryTerms An array of query terms.
     * @param postingLists A HashMap containing posting lists for each query term.
     * @param scoreFunction The ScoreFunction used for computing document scores.
     * @param k The maximum number of top documents to consider.
     * @param encodingType The encoding type for processing postings.
     * @param scoreType The type of score computation.
     * @return A PQueue containing the top-k documents and their scores.
     */
    public PQueue scoreDocuments(String[] queryTerms, HashMap<String, ArrayList<Posting>> postingLists, ScoreFunction scoreFunction, int k, String encodingType, String scoreType){
        PQueue scores = new PQueue(k); //Initialize a new PriorityQueue with a capacity of k
        ArrayList<PLI> Iterators = new ArrayList<>(); //List of iterators

        //Create an iterator through the posting list related to each query term
        for(String term : queryTerms){
            Iterators.add(new PLI(term, postingLists.get(term), scoreFunction, queryProcessor, "daat"));
        }

        //Check if the query is conjunctive
        if(queryType.equals("conjunctive")){
            processConjunctive(scores,Iterators, encodingType,scoreType);
            return scores;
        }

        //Get minimum docID over all posting lists
        while(!notFinished(Iterators)){
            int minDocid = minDocId(Iterators, encodingType);

            double score = 0.0;

            //For each posting list check if the current posting correspond to the minimum docID
            for(int i = 0; i < queryTerms.length; i++){
                PLI term_iterator = Iterators.get(i);
                if (!term_iterator.isFinished(encodingType)) {
                    //If the current posting has the docID equal to the minimum docID
                    if (term_iterator.docid() == minDocid) {
                        score += term_iterator.score(queryTerms[i], scoreType); //Compute the score using the posting score function
                        term_iterator.next(); //Navigate to the next element of the posting list
                    }
                }
            }
            //Add the final score to the priorityQueue
            scores.add(new DocsRanked(minDocid,score));
        }
        return scores; //Return the top K scores
    }

    /**
     * Processes conjunctive query to score documents based on posting lists and iterators.
     *
     * @param scores A PQueue to store the scores of top documents.
     * @param Iterators An ArrayList of Posting List Iterators.
     * @param encodingType The encoding type for processing postings.
     * @param scoreType The type of score computation.
     */
    public void processConjunctive(PQueue scores, ArrayList<PLI> Iterators, String encodingType, String scoreType){
        //Find the smallest postingList
        int minPostingListIndex = 0;
        int minPostingListLength = queryProcessor.getLexicon().getLexicon().get(Iterators.get(0).getTerm()).getPostingListLength();
        for(int i=1; i<Iterators.size(); i++){
            if(minPostingListLength>queryProcessor.getLexicon().getLexicon().get(Iterators.get(i).getTerm()).getPostingListLength()){
                minPostingListIndex = i;
                minPostingListLength = queryProcessor.getLexicon().getLexicon().get(Iterators.get(i).getTerm()).getPostingListLength();
            }
        }

        PLI minPostingListIterator = Iterators.get(minPostingListIndex);
        //Continue while there are posting to be processed
        while(!minPostingListIterator.isFinished(encodingType)){
            boolean toAdd = true;
            int docId = minPostingListIterator.docid();
            double score = minPostingListIterator.score(minPostingListIterator.getTerm(), scoreType);
            minPostingListIterator.next();

            //for each other posting list call the nextGEQ on the docID of the smallest postingList
            for(int i=0;i<Iterators.size();i++){
                if(i!=minPostingListIndex){
                    Iterators.get(i).nextGEQ(docId, encodingType);
                    if(docId == Iterators.get(i).docid()){
                        score += Iterators.get(i).score(Iterators.get(i).getTerm(), scoreType);
                    }else{
                        toAdd = false;
                        break;
                    }
                }
            }
            if(toAdd){
                scores.add(new DocsRanked(queryProcessor.getDocIndex().getDocIndex().get(docId).getDocNo(),score));
            }
        }

    }


    /**
     * Gets the minimum document ID over all the posting lists.
     *
     * @param Iterators An ArrayList of Posting List Iterators.
     * @param encodingType The encoding type for processing postings.
     * @return The minimum document ID.
     */
    public int minDocId(ArrayList<PLI> Iterators, String encodingType){
        int minDocId = Integer.MAX_VALUE; //maximum docID
        for(PLI postingListIterator : Iterators){
            if(!postingListIterator.isFinished(encodingType)){
                if(postingListIterator.docid() < minDocId){
                    minDocId = postingListIterator.docid();
                }
            }
        }
        return minDocId;
    }

    /**
     * Checks if the query processing is finished, i.e., all the posting lists have been fully processed.
     *
     * @param Iterators An ArrayList of Posting List Iterators.
     * @return True if the processing is finished, false otherwise.
     */
    public boolean notFinished(ArrayList<PLI> Iterators){
        boolean finished = true;
        for(PLI postingListIterator : Iterators){
            if (postingListIterator.hasNext()) {
                finished = false;
                break;
            }
        }
        return finished;
    }
}
