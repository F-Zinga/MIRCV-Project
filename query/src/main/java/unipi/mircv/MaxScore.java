package unipi.mircv;


import org.javatuples.Pair;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.PriorityQueue;

/**
 * The MaxScore class is responsible for scoring documents based on the Document-At-a-Time retrieval model.
 * It supports both disjunctive and conjunctive queries using either TFIDF or BM25 scoring functions.
 */

public class MaxScore {

    // TODO: CREATE parameter interface for query processor
    //Constants for the BM25 scoring
    static final double K1 = 1.6;
    static final double B = 0.75;

    //Length of the final ranking
    static final int BEST_K_VALUE = 20;

    //Statistics of the inverted index
    static final Statistics statistics = new Statistics();

    /**
     * Scores documents for disjunctive queries using Document-At-a-Time retrieval model.
     *
     * @param postingLists   Array of posting lists corresponding to the query terms.
     * @param documentIndex  Document index containing information about documents.
     * @param BM25           Boolean indicating whether to use BM25 scoring (true) or TFIDF scoring (false).
     * @return An ordered list of tuples containing document ID and their corresponding scores.
     */
    public static ArrayList<Pair<Long,Double>> scoreCollectionDisjunctive(PostingList[] postingLists, DocIndex documentIndex, boolean BM25) {

        // Priority queue to store doc ID and its corresponding score in descending order of score
        DocsRanked rankedDocs = new DocsRanked(BEST_K_VALUE);
        // List of posting lists considered essential for scoring
        ArrayList<Integer> essential = new ArrayList<>();

        // Ordered posting lists based on term upper bounds
        ArrayList<PostingList> orderedPostingLists = new ArrayList<>();

        // Record the start time for performance measurement
        long begin = System.currentTimeMillis();

        //Move the iterators of each posting list to the first position
        for (PostingList postingList : postingLists) {
            if (postingList.hasNext()) {
                postingList.next();
                orderedPostingLists.add(postingList);
            }
        }

        //sort the posting list in ascending order of term upper bounds
        if(BM25) {
            orderedPostingLists.sort(Comparator.comparingInt(o -> o.getTermInfo().getBm25TermUpperBound()));
        }
        else{
            orderedPostingLists.sort(Comparator.comparingInt(o -> o.getTermInfo().getTfidfTermUpperBound()));
        }

        //Fill essential posting lists and update postingLists array
        for(int i = 0; i < orderedPostingLists.size(); i++){
            essential.add(i);
            postingLists[i] = orderedPostingLists.get(i);

        }


        //Pair containing the current minimum doc id and the list of posting lists containing it
        Pair<Long,ArrayList<Integer>> minDocidTuple;

        //Variables to accumulate score values over the iteration
        double tf_tfidf;
        double tf_BM25;
        double score = 0;


        //Iterate over posting lists in Document-At-a-Time until no more postings are available
        while (!allPostingListsEnded(postingLists)) {
            //if essential is empty no more docs can enter the top K ranking
            if(essential.isEmpty()){
                break;
            }

            //Retrieve the minimum document id and the list of posting lists containing it
            minDocidTuple = minDocID(postingLists);


            //check if some docs can enter the top K ranking
            if(!foundEssential(minDocidTuple.getValue1(), essential)){

                for(Integer index : minDocidTuple.getValue1()){
                    postingLists[index].next();
                }

                continue;
            }
            //For each index in the list of posting lists with min doc id
            for(int index = 0; index < minDocidTuple.getValue1().size(); index++){
                if(BM25){
                    //Compute the BM25's tf for the current posting
                    tf_BM25 = postingLists[minDocidTuple.getValue1().get(index)].getFreq()/ (K1 * (
                            (1-B) +
                                    B * ((double)documentIndex.get(postingLists[minDocidTuple.getValue1().get(index)].getDocId()).getDocLen() / statistics.getAvdl())
                                    + postingLists[minDocidTuple.getValue1().get(index)].getFreq()
                    ));

                    //Add the partial score to the accumulated score
                    score += tf_BM25*postingLists[minDocidTuple.getValue1().get(index)].getTermInfo().getIdf();


                    // Compute the new maximum score for the remaining posting lists
                    double newMaxScore = score;
                    for(int j = index + 1; j < minDocidTuple.getValue1().size(); j++){
                        newMaxScore += postingLists[minDocidTuple.getValue1().get(j)].getTermInfo().getBm25TermUpperBound();
                    }

                    // If the new max score is below the current threshold, skip to the next iteration
                    if(newMaxScore < rankedDocs.getThreshold()){
                        for(int j = index; j < minDocidTuple.getValue1().size(); j++){
                            postingLists[minDocidTuple.getValue1().get(j)].next();
                        }
                        break;
                    }
                }else {
                    //Compute the TFIDF'S tf for the current posting
                    tf_tfidf = 1 + Math.log(postingLists[minDocidTuple.getValue1().get(index)].getFreq()) / Math.log(2);

                    //Add the partial score to the accumulated score
                    score += tf_tfidf*postingLists[minDocidTuple.getValue1().get(index)].getTermInfo().getIdf();


                    // Calculate the new upper bound for the remaining posting lists
                    double newMaxScore = score;
                    for(int j = index + 1; j < minDocidTuple.getValue1().size(); j++){
                        newMaxScore += postingLists[minDocidTuple.getValue1().get(j)].getTermInfo().getTfidfTermUpperBound();
                    }

                    // Check if the new upper bound is below the current threshold, and if so, skip to the next document
                    if(newMaxScore < rankedDocs.getThreshold()){
                        for(int j = index; j < minDocidTuple.getValue1().size(); j++){
                            postingLists[minDocidTuple.getValue1().get(j)].next();
                        }
                        break;
                    }
                }

                //Move the cursor to the next posting, if there is one, otherwise the flag of the posting list is set to
                // true, marking the end of the posting list
                postingLists[minDocidTuple.getValue1().get(index)].next();
            }

            //Since we have a document in all the posting lists, add its score to the priority queue
            if(score > rankedDocs.getThreshold()){
                double old_threshold = rankedDocs.getThreshold();
                rankedDocs.add(new Pair<>(minDocidTuple.getValue0(), score));

                //update the non-essential and the essential posting lists
                if(rankedDocs.getThreshold() > 0) {
                    updateEssentialPL(essential, orderedPostingLists, rankedDocs.getThreshold(), BM25);
                }
            }

            //Clear the support variables for the next iteration
            score = 0;
        }

        //Print the time used to score the documents, so to generate an answer for the query
        System.out.println("\n ***Total scoring time: ***" + (System.currentTimeMillis() - begin) + "ms");

        return getBestKDocuments(rankedDocs, BEST_K_VALUE);
    }

    /**
     * Function to update the essential posting lists for MaxScore
     *
     * @param essential arraylist that contain the indexes of the essential posting lists
     * @param orderedPostingLists list of all the posting lists related to the query
     * @param threshold current threshold of the ranking
     * @param BM25 if BM25 is to be used or not (if not tfidf will be used)
     */
    private static void updateEssentialPL(ArrayList<Integer> essential, ArrayList<PostingList> orderedPostingLists, double threshold, boolean BM25) {
        int tmp_count = 0;
        essential.clear();

        // Iterate through the ordered posting lists to determine essential posting lists based on the ranking metric
        for(int i = 0; i < orderedPostingLists.size(); i++){
            //check the ranking metric
            if(BM25){
                tmp_count += orderedPostingLists.get(i).getTermInfo().getBm25TermUpperBound();
            }
            else {
                tmp_count += orderedPostingLists.get(i).getTermInfo().getTfidfTermUpperBound();
            }

            //check if the posting list is an essential or a non-essential based on the accumulated upper bounds
            if(tmp_count > threshold){
                essential.add(i);
            }
        }
    }


    /**
     * Get the maximum document id from the passed posting list array
     * @param postingLists posting list from which analyze the current docid to retrieve the maximum
     * @return the maximum document id
     */
    private static long maxDocID(PostingList[] postingLists){

        long max = -1;

        //Traverse the array of posting list and find the maximum document id among the current doc ids
        for(PostingList postingList : postingLists){
            if(postingList.getDocId() > max){
                max = postingList.getDocId();
            }
        }
        return max;
    }

    /**
     * Calculate the minimum document id in a collection of posting lists, fill an arrayList containing
     * the indices of the postingList array with the minimum document id. Returns a pair containing the minimum
     * doc id and the array of indices. The posting lists are sorted by document id, so we can optimize the search
     * accessing only the current term pointed by the iterator of each posting list.
     * @param postingLists Array of posting lists.
     * @return a tuple containing the minimum doc id and the array of indices of the postingList array with min doc id.
     */
    public static Pair<Long, ArrayList<Integer>>  minDocID(PostingList[] postingLists) {

        //Variable to store the minimum document id
        long minDocid = Long.MAX_VALUE;

        //Array to store the posting lists with the minimum document id
        ArrayList<Integer> postingListsWithMinDocid = new ArrayList<>();

        //FInd the minimum document id, checking the first element of each posting list (we have
        //the document id in the posting lists ordered by increasing doc id)
        //For each posting list we check the current document id
        for(int i = 0; i < postingLists.length; i++){

            //Skip the i-th posting list if the list don't contain more postings, we've reached the end of the list
            if(postingLists[i].getEndPosting()){
                continue;
            }

            //Update the minDocId if the current docid is smaller than the minDocId
            if (postingLists[i].getDocId() < minDocid) {

                //Store the new minimum document id
                minDocid = postingLists[i].getDocId();

                //Clear the list of posting lists with the minimum document id
                postingListsWithMinDocid.clear();

                //Add the current posting list to the list of posting lists with the minimum document id
                postingListsWithMinDocid.add(i);

                //Else if the current docid is equal to the min term, then add the current posting list
                // to the list of posting lists with the min docid.
            }else if (postingLists[i].getDocId() == minDocid) {

                //Add the current posting list to the list of posting lists with the min docid
                postingListsWithMinDocid.add(i);
            }
        }

        //Return the minimum document id and the list of posting lists with the minimum document id
        return new Pair<>(minDocid, postingListsWithMinDocid);
    }



    /**
     * Implementation of the algorithm Document-At-a-Time, iterating over all the posting lists and scoring
     * the document with the minimum document id in the conjunctive query.
     * @param postingLists Array of posting lists.
     * @param documentIndex document index containing the information of the documents.
     * @param BM25 if it is true, then the BM25 scoring is applied, otherwise the scoring is TFIDF.
     * @return an ordered array of tuples containing the document id and the score associated with the document.
     */
    public static ArrayList<Pair<Long,Double>> scoreCollectionConjunctive(PostingList[] postingLists, DocIndex documentIndex, boolean BM25) {

        //Priority queue to store the document id and its score, based on the priority of the document
        DocsRanked docsRanked = new DocsRanked(BEST_K_VALUE);
        ArrayList<Integer> essential = new ArrayList<>();

        ArrayList<PostingList> orderedPostingLists = new ArrayList<>();

        // Record the start time for performance measurement
        long begin = System.currentTimeMillis();

        //Move the iterators of each posting list to the first position
        for (PostingList postingList : postingLists) {
            if (postingList.hasNext()) {
                postingList.next();
                orderedPostingLists.add(postingList);
            }
        }

        //sort the posting list in ascending order
        if(BM25) {
            orderedPostingLists.sort(Comparator.comparingInt(o -> o.getTermInfo().getBm25TermUpperBound()));
        }
        else{
            orderedPostingLists.sort(Comparator.comparingInt(o -> o.getTermInfo().getTfidfTermUpperBound()));
        }

        for(int i = 0; i < orderedPostingLists.size(); i++){
            essential.add(i);
            postingLists[i] = orderedPostingLists.get(i);
        }

        //Pair to store the current minimum document id and the list of posting lists containing it
        long maxDocid;

        //Variables to accumulate over the iteration the score values
        double tf_tfidf;
        double tf_BM25;
        double score = 0;

        //Access each posting list in a Document-At-a-Time until no more postings are available
        while (!aPostingListEnded(postingLists)) {
            //if essential is empty no more docs can enter the top K ranking
            if(essential.isEmpty()){
                break;
            }
            //Retrieve the maximum document id and the list of posting lists containing it
            maxDocid = maxDocID(postingLists);


            //Perform the nextGEQ operation for each posting list
            for (PostingList postingList : postingLists) {
                //If we reach the end of the posting list then we break the for, the conjunctive query is ended
                // and all the next conditions are not satisfied
                postingList.nextGEQ(maxDocid);
            }
            if (aPostingListEnded(postingLists)) {
                break;
            }

            //If the current doc id is equal in all the posting lists
            if (sameDocID(postingLists)) {
                //Score the document
                int index = 0;
                for (PostingList postingList : postingLists) {

                    //Debug
                    //System.out.println(postingList);

                    if (BM25) {

                        //Compute the BM25's tf for the current posting
                        tf_BM25 = postingList.getFreq() / (K1 * ((1 - B) + B * ((double) documentIndex.get(postingList.getDocId()).getDocLen() / statistics.getAvdl()) + postingList.getFreq()));

                        //Add the partial score to the accumulated score
                        score += tf_BM25 * postingList.getTermInfo().getIdf();


                        double newMaxScore = score;
                        for(int j = index + 1; j < postingLists.length; j++){
                            newMaxScore += postingLists[j].getTermInfo().getBm25TermUpperBound();
                        }

                        if(newMaxScore < docsRanked.getThreshold()){
                            for(int j = index; j < postingLists.length; j++){
                                postingLists[j].next();
                            }
                            break;
                        }

                    } else {

                        //Compute the TFIDF'S tf for the current posting
                        tf_tfidf = 1 + Math.log(postingList.getFreq()) / Math.log(2);

                        //Add the partial score to the accumulated score
                        score += tf_tfidf * postingList.getTermInfo().getIdf();


                        double newMaxScore = score;
                        for(int j = index + 1; j < postingLists.length; j++){
                            newMaxScore += postingLists[j].getTermInfo().getTfidfTermUpperBound();
                        }

                        if(newMaxScore < docsRanked.getThreshold()){
                            for(int j = index; j < postingLists.length; j++){
                                postingLists[j].next();
                            }
                            break;
                        }
                    }

                    //Move the cursor to the next posting
                    postingList.next();
                    index++;
                }

                // If we have a document in all the posting lists, add its score to the priority queue
                // if the score is relevant for the conjunctive query
                if(score > docsRanked.getThreshold()) {
                    double old_threshold = docsRanked.getThreshold();
                    docsRanked.add(new Pair<>(maxDocid, score));

                    //update the non-essential and the essential posting lists
                    if(docsRanked.getThreshold() > 0){
                        updateEssentialPL(essential, orderedPostingLists, docsRanked.getThreshold(), BM25);
                    }
                }
            }
            //clear the support variables for the next iteration
            score = 0;
        }

        //print time used to score the documents, to generate an answer for the query
        System.out.println("\n *** Total scoring time: ***" + (System.currentTimeMillis() - begin) + "ms");

        //return the top K documents
        return getBestKDocuments(docsRanked, BEST_K_VALUE);
    }

    /**
     * Checks if at least one element of the given postingListIndex (posting lists related to the query)
     * is in the essential set.
     *
     * @param postingListsIndex indexes of the query-related posting lists
     * @param essential arraylist containing the current essential posting lists
     * @return true if at least a posting list related to the query is in the essential set, otherwise false
     */
    private static boolean foundEssential(ArrayList<Integer> postingListsIndex, ArrayList<Integer> essential) {
        for(int i : postingListsIndex){
            for(int j: essential){
                if(i == j) {
                    return true;
                }
            }
        }
        return false;
    }


    /**
     * Checks if all the posting lists are ended, so the iterator has reached the end of each posting list.
     * @param postingLists Array of posting lists.
     * @return true if all the posting lists are ended (no more postings), false otherwise.
     */
    public static boolean allPostingListsEnded(PostingList[] postingLists){

        //For each posting list check if it has a next posting
        for (PostingList postingList : postingLists) {

            //If at least one posting is available return false
            if (!postingList.getEndPosting()) {
                return false;
            }
        }

        //If all the posting lists are traversed then return false
        return true;
    }

    /**
     * Check if at least a posting list is ended
     * @param postingLists array of posting lists
     * @return true if at least a posting list is ended, otherwise false
     */
    public static boolean aPostingListEnded(PostingList[] postingLists){

        //Check if each posting list has a next posting
        for (PostingList postingList : postingLists) {

            //If at least one posting is ended return true
            if (postingList.getEndPosting()) {
                return true;
            }
        }

        //If all the posting lists are traversed then return false
        return false;
    }

    /**
     * Extract the first k tuples (docID, score) from the priority queue, in descending order of score.
     * @param rankedDocs The priority queue containing the documents and their scores.
     * @param k The number of tuples to extract.
     * @return an ordered array of tuples containing the document id and the score associated with the document.
     */
    public static ArrayList<Pair<Long, Double>> getBestKDocuments(PriorityQueue<Pair<Long,Double>> rankedDocs, int k){

        //Array list to build the result
        ArrayList<Pair<Long, Double>> results = new ArrayList<>();

        //Tuple used to contain the current (docID, score) tuple
        Pair<Long,Double> tuple;

        //Until k pairs are kept from the priority queue
        while(results.size() < k){

            //Retrieve the first pair from the priority queue based on the score value (descending order)
            tuple = rankedDocs.poll();

            //If the pair is null then we've reached the end of the priority queue, less than k tuples were present
            if(tuple == null){
                break;
            }

            //Add the pair to the result list
            results.add(tuple);
        }

        //Return the result list
        return results;
    }

    /**
     * Check if all the current doc ids of each posting list are equal.
     * @param postingLists array of posting lists to check
     * @return true if all the current doc ids are equal, false otherwise
     */
    private static boolean sameDocID(PostingList[] postingLists){

        long docid = -1;
        if(postingLists.length == 1)
            return true;

        //Traverse all the posting lists if two different docids are found, then return false
        for(PostingList postingList : postingLists){

            //If at least one is ended
            if(postingList == null){
                return false;
            }

            if(docid == -1){
                docid = postingList.getDocId();
            }else if(docid != postingList.getDocId()){
                return false;
            }
        }

        //All the docids are equal
        return true;
    }




}
