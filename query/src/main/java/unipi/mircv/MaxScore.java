package unipi.mircv;


import org.javatuples.Pair;

import java.util.*;
import java.util.stream.Collectors;

/**
 * The MaxScore class implements scoring of documents using the Document-At-a-Time retrieval model.
 * It supports both disjunctive and conjunctive queries, utilizing either TFIDF or BM25 scoring functions.
 */

public class MaxScore {

    String queryType; // conjunctive or disjunctive
    QueryProcessor queryProcessor; // Processor for handling queries and lexicon information


    /**
     * Constructs a MaxScore instance with the specified query type and query processor.
     *
     * @param queryType        Type of the query (conjunctive or disjunctive)
     * @param queryProcessor   Processor for handling queries and lexicon information
     */
    public MaxScore(String queryType, QueryProcessor queryProcessor){
        this.queryType = queryType;
        this.queryProcessor = queryProcessor;
    }

    /**
     * Main function for scoring documents based on the Document-At-a-Time retrieval model.
     *
     * @param queryTerms      Array of query terms
     * @param postingLists    Mapping of query terms to their corresponding posting lists
     * @param scoreFunction   Scoring function (TFIDF or BM25)
     * @param k               Number of top documents to retrieve
     * @param encodingType    Encoding type for document and query representation
     * @param scoreType       Type of score to calculate (e.g., raw score or normalized score)
     * @return                Priority queue containing the top K scored documents
     */
    public PQueue scoreDocuments(String[] queryTerms, HashMap<String, ArrayList<Posting>> postingLists, ScoreFunction scoreFunction, int k,String encodingType,String scoreType){
        PQueue scores = new PQueue(k); //Initialize a new PriorityQueue with a capacity of k
        HashMap<String, Double> termUpperBounds = new HashMap<>(); //Create a HashMap of term upper bounds
        double threshold = 0;

        // Determine term upper bounds for each query term
        for(String term : queryTerms){
            termUpperBounds.put(term,(double) queryProcessor.getLexicon().getLexicon().get(term).getTermUpperBound());
        }

        // Sort the posting lists based on the term upper bounds
        postingLists = postingLists.entrySet().stream()
                .sorted(Comparator.comparingDouble(e -> termUpperBounds.get(e.getKey())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        // Create an array of terms in the order they appear in the posting lists
        String[] termsOrder = postingLists.keySet().toArray(new String[0]);

        // Create an array of booleans to keep track of essential posting lists and mark them accordingly
        boolean[] essentialPostingList = new boolean[termsOrder.length];
        double counter = 0;
        for(int i = 0; i < termsOrder.length; i++){
            counter += termUpperBounds.get(termsOrder[i]);
            essentialPostingList[i] = counter >= threshold;
        }

        //Create an array list of PostingListIterators, one for each query term
        ArrayList<PLI> postingIterators = new ArrayList<>();
        for(String term : termsOrder){
            postingIterators.add(new PLI(term, postingLists.get(term), scoreFunction, queryProcessor, "maxscore"));
        }

        // Check if the query is conjunctive and process it accordingly
        if(queryType.equals("conjunctive")){
            processConjunctive(scores,postingIterators,encodingType,scoreType);
            return scores;
        }

        // Iterate through the posting lists until all are finished
        while(!notFinished(postingIterators, essentialPostingList,encodingType)){
            int minDocid = minDocId(postingIterators, essentialPostingList,encodingType); //Get minimum docID over all posting lists
            double score = 0.0;
            boolean checkDocUpperBound = false;

            // Loop through the posting lists in reverse order
            for(int i = termsOrder.length-1; i >= 0; i--){ //Foreach posting list check if the current posting corresponds to the minimum docID
                PLI term_iterator = postingIterators.get(i);
                if (term_iterator.getPostingList().isEmpty()) continue;

                // If the current posting list is not essential
                if (!essentialPostingList[i]) {
                    if(!checkDocUpperBound) { // Check if the document upper bound has been reached
                        if(!checkDocumentUpperBound(score, termUpperBounds, termsOrder,  i, threshold)){
                            break; // If it has reached, break out of the loop
                        }else{
                            checkDocUpperBound = true;
                        }
                    }
                    term_iterator.nextGEQ(minDocid,encodingType); // Move the iterator to the next element with a docID greater or equal to the minimum docID
                }
                // If the iterator has not reached the end of the posting list
                if (term_iterator.hasNext()) {
                    if (term_iterator.docid() == minDocid) {  // If the current posting has the same docID as the minimum docID
                        score += term_iterator.score(termsOrder[i],scoreType); // Add the score for the posting to the total score for the document
                        term_iterator.next(); // Move the iterator to the next element
                    }
                }
            }

            // Add the final score as a pair for the document to the priority queue
            scores.add(new DocsRanked(minDocid, score));
            // If the priority queue is full
            if(scores.isFull()){
                threshold = scores.peek().getValue();  // Set the threshold to the minimum score in the priority queue
            }

            counter = 0;
            for(int i = 0; i < termsOrder.length; i++){
                counter += termUpperBounds.get(termsOrder[i]);
                essentialPostingList[i] = counter >= threshold;
            }
        }

        return scores; //Return the top K scores


    }


    public void processConjunctive(PQueue scores, ArrayList<PLI> postingListIterators, String encodingType, String scoreType){
    /**
     * Process conjunctive query, finding the common documents among posting lists and calculating their scores.
     *
     * @param scores              Priority queue to store the top K scored documents
     * @param postingListIterators List of posting list iterators for each query term
     * @param encodingType        Encoding type for document and query representation
     * @param scoreType           Type of score to calculate (e.g., raw score or normalized score)
     */
        //Find the smallest postingList
        int minPostingListIndex = 0;
        int minPostingListLength = queryProcessor.getLexicon().getLexicon().get(postingListIterators.get(0).getTerm()).getPostingListLength();
        for(int i=1; i<postingListIterators.size(); i++){
            if(minPostingListLength>queryProcessor.getLexicon().getLexicon().get(postingListIterators.get(i).getTerm()).getPostingListLength()){
                minPostingListIndex = i;
                minPostingListLength = queryProcessor.getLexicon().getLexicon().get(postingListIterators.get(i).getTerm()).getPostingListLength();
            }
        }

        PLI minPostingListIterator = postingListIterators.get(minPostingListIndex);
        while(!minPostingListIterator.isFinished(encodingType)){//While there are posting to be processed
        // Set the iterator to the posting list with the smallest length and process its entries
            boolean toAdd = true;
            int docId = minPostingListIterator.docid();
            double score = minPostingListIterator.score(minPostingListIterator.getTerm(),scoreType);
            minPostingListIterator.next();

            // Iterate over other posting lists and synchronize on the document ID
            for(int i=0;i<postingListIterators.size();i++){ //foreach other posting list call the nextGEQ on the docID of the smallest postingList
                if(i!=minPostingListIndex){
                    postingListIterators.get(i).nextGEQ(docId,encodingType);
                    if(docId == postingListIterators.get(i).docid()){
                        score += postingListIterators.get(i).score(postingListIterators.get(i).getTerm(),scoreType);
                    }else{
                        toAdd = false;
                        break;
                    }
                }
            }
            // Add the document with its aggregated score if it satisfies conditions
            if(toAdd){
                scores.add(new DocsRanked(queryProcessor.getDocIndex().getDocIndex().get(docId).getDocNo(),score));
            }
        }

    }

    // Check if the remaining upper bound score of the terms exceeds the threshold
    public boolean checkDocumentUpperBound(double score, HashMap<String, Double> termUpperBounds, String[] termsOrder, int i, double threshold){
        for(int j=i; j>=0; j--){
            score += termUpperBounds.get(termsOrder[j]);
        }
        return score >= threshold;
    }

    // Determine the minimum document ID across all posting lists
    public int minDocId(ArrayList<PLI> postingIterators, boolean[] essentialPostingList, String encodingType){
        int minDocId = Integer.MAX_VALUE;

        for(int i=essentialPostingList.length-1; i>=0; i--){
            if(essentialPostingList[i]){
                PLI postingIterator = postingIterators.get(i);
                if(!postingIterator.isFinished(encodingType)) {
                    if(postingIterator.docid() < minDocId){
                        minDocId = postingIterator.docid();
                    }
                }
            }
        }

        return minDocId;
    }

    //Check if the query processing is finished, i.e. all the posting lists has been fully processed
    public boolean notFinished(ArrayList<PLI> postingIterators, boolean[] essentialPostingList, String encodingType){
        boolean finished = true;
        for(int i=essentialPostingList.length-1; i>=0; i--){
            if(essentialPostingList[i]){
                if(!postingIterators.get(i).isFinished(encodingType)) {
                    finished = false;
                    break;
                }
            }
        }
        return finished;
    }




}
