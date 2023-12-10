package unipi.mircv;


import org.javatuples.Pair;
import java.util.Comparator;
import java.util.ArrayList;

public class MaxScore {

    // TODO: CREATE parameter interface for query processor
    //Values used for the BM25 scoring
    static final double K1 = 1.5;
    static final double B = 0.75;
    //Length of the final ranking
    static final int BEST_K_VALUE = 20;

    //Retrieve the statistics of the inverted index
    static final Statistics statistics = new Statistics();

    public static ArrayList<Pair<Long,Double>> scoreCollectionDisjunctive(PostingList[] postingLists, DocIndex documentIndex, boolean BM25, boolean debug) {

        RankedDocs rankedDocs = new RankedDocs(BEST_K_VALUE);
        ArrayList<Integer> essential = new ArrayList<>();

        ArrayList<PostingList> orderedPostingLists = new ArrayList<>();

        //Retrieve the time at the beginning of the computation
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
            /*
            if(debug){
                System.out.println("[DEBUG] Lexicon entry:\n" + postingLists[i].getTermInfo());
            }
            */
        }


        //Tuple to store the current minimum document id and the list of posting lists containing it
        Pair<Long,ArrayList<Integer>> minDocidTuple;

        //Support variables to accumulate over the iteration the score values
        double tf_tfidf;
        double tf_BM25;
        double score = 0;

        //Access each posting list in a Document-At-a-Time fashion until no more postings are available
        while (!allPostingListsEnded(postingLists)) {
            //if essential is empty no more docs can enter the top K ranking
            if(essential.isEmpty()){
                break;
            }

            //Retrieve the minimum document id and the list of posting lists containing it
            minDocidTuple = minDocid(postingLists);

            if(debug) {
                System.out.println("------------------");
                System.out.println("[DEBUG] Min docID: " + minDocidTuple.getValue0());
                System.out.println("[DEBUG] Blocks with minDocID: " + minDocidTuple.getValue1());
            }

            //check if some docs can enter the top K ranking
            if(!foundEssential(minDocidTuple.getValue1(), essential)){
                for(Integer index : minDocidTuple.getValue1()){
                    postingLists[index].next();
                }

                continue;
            }
            //For each index in the list of posting lists with min doc id
            for(int index = 0; index < minDocidTuple.getValue1().size(); index++){
                //If the scoring is BM25
                if(BM25){
                    //Compute the BM25's tf for the current posting
                    tf_BM25 = postingLists[minDocidTuple.getValue1().get(index)].getFreq()/ (K1 * (
                            (1-B) +
                                    B * ((double)documentIndex.get(postingLists[minDocidTuple.getValue1().get(index)].getDocId()).getDocLength() / statistics.getAvdl())
                                    + postingLists[minDocidTuple.getValue1().get(index)].getFreq()
                    ));

                    //Add the partial score to the accumulated score
                    score += tf_BM25*postingLists[minDocidTuple.getValue1().get(index)].getTermInfo().getIdf();

                    if(debug){
                        System.out.println("[DEBUG] bm25 docID " + minDocidTuple.getValue0() + ": " + score);
                    }

                    double newMaxScore = score;
                    for(int j = index + 1; j < minDocidTuple.getValue1().size(); j++){
                        newMaxScore += postingLists[minDocidTuple.getValue1().get(j)].getTermInfo().getBm25TermUpperBound();
                    }

                    if(newMaxScore < rankedDocs.getThreshold()){
                        if(debug) {
                            System.out.println("[DEBUG] New Max Score < rankedDocs.getThreshold: " + newMaxScore + "<" + rankedDocs.getThreshold() +  " docID " + minDocidTuple.getValue0() + " ruled out");
                        }
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

                    if(debug){
                        System.out.println("[DEBUG] tfidf docID " + minDocidTuple.getValue0() + ": " + score);
                    }

                    double newMaxScore = score;
                    for(int j = index + 1; j < minDocidTuple.getValue1().size(); j++){
                        newMaxScore += postingLists[minDocidTuple.getValue1().get(j)].getTermInfo().getTfidfTermUpperBound();
                    }

                    if(newMaxScore < rankedDocs.getThreshold()){
                        if(debug) {
                            System.out.println("[DEBUG] New Max Score < rankedDocs.getThreshold: " + newMaxScore +
                                    "<" + rankedDocs.getThreshold() +  " docID " + minDocidTuple.getValue0() + " ruled out");
                        }
                        for(int j = index; j < minDocidTuple.getValue1().size(); j++){
                            postingLists[minDocidTuple.getValue1().get(j)].next();
                        }
                        break;
                    }

                }

                //Move the cursor to the next posting, if there is one, otherwise the flag of the posting list is set to
                // true, in this way we mark the end of the posting list
                postingLists[minDocidTuple.getValue1().get(index)].next();
            }

            //Add the score of the current document to the priority queue
            if(score > rankedDocs.getThreshold()){
                double old_threshold = rankedDocs.getThreshold();
                rankedDocs.add(new Pair<>(minDocidTuple.getValue0(), score));

                //update the non-essential and the essential posting lists
                if(rankedDocs.getThreshold() > 0) {
                    updateEssentialPostingLists(essential, orderedPostingLists, rankedDocs.getThreshold(), BM25);
                    if(debug){
                        System.out.println("[DEBUG] Threshold changed: " + old_threshold + " -> " + rankedDocs.getThreshold());
                    }
                }
            }

            //Clear the support variables for the next iteration
            score = 0;
        }
        //prova
        //Print the time used to score the documents, so to generate an answer for the query
        System.out.println("\n[SCORE DOCUMENT] Total scoring time: " + (System.currentTimeMillis() - begin) + "ms");

        return getBestKDocuments(rankedDocs, BEST_K_VALUE);
    }


}
