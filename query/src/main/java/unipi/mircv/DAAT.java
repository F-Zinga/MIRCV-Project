package unipi.mircv;


import java.util.ArrayList;
import java.util.HashMap;

public class DAAT {
    String queryType; // conjunctive or disjunctive
    QueryProcessor queryProcessor; // Processor for handling queries and lexicon information

    public DAAT(String queryType, QueryProcessor queryProcessor){
        this.queryType = queryType;
        this.queryProcessor = queryProcessor;
    }

    public PQueue scoreDocuments(String[] queryTerms, HashMap<String, ArrayList<Posting>> postingLists, ScoreFunction scoreFunction, int k, String encodingType, String scoreType){
        PQueue scores = new PQueue(k); //Initialize a new PriorityQueue with a capacity of k
        ArrayList<PLI> Iterators = new ArrayList<>(); //List of iterators

        //Create an iterator foreach posting list related to each query term (like a pointer)
        for(String term : queryTerms){
            Iterators.add(new PLI(term, postingLists.get(term), scoreFunction, queryProcessor, "daat"));
        }

        //Check if the query is conjunctive and execute it conjunctive in case
        if(queryType.equals("conjunctive")){
            processConjunctive(scores,Iterators, encodingType,scoreType);
            return scores;
        }

        while(!notFinished(Iterators)){
            int minDocid = minDocId(Iterators, encodingType); //Get minimum docID over all posting lists

            double score = 0.0;
            for(int i = 0; i < queryTerms.length; i++){ //Foreach posting list check if the current posting correspond to the minimum docID
                PLI term_iterator = Iterators.get(i);
                if (!term_iterator.isFinished(encodingType)) {
                    if (term_iterator.docid() == minDocid) { //If the current posting has the docID equal to the minimum docID
                        score += term_iterator.score(queryTerms[i], scoreType); //Compute the score using the posting score function
                        term_iterator.next(); //Go to the next element of the posting list
                    }
                }
            }

            scores.add(new DocsRanked(minDocid,score)); //Add the final score to the priorityQueue
        }
        return scores; //Return the top K scores
    }

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
        while(!minPostingListIterator.isFinished(encodingType)){ //While there are posting to be processed
            boolean toAdd = true;
            int docId = minPostingListIterator.docid();
            double score = minPostingListIterator.score(minPostingListIterator.getTerm(), scoreType);
            minPostingListIterator.next();
            for(int i=0;i<Iterators.size();i++){ //foreach other posting list call the nextGEQ on the docID of the smallest postingList
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


    //Get the minimum docID over all the posting lists
    public int minDocId(ArrayList<PLI> Iterators, String encodingType){
        int minDocId = Integer.MAX_VALUE; //N° docs in the collection (maximum docID)
        for(PLI postingListIterator : Iterators){
            if(!postingListIterator.isFinished(encodingType)){
                if(postingListIterator.docid() < minDocId){
                    minDocId = postingListIterator.docid();
                }
            }
        }
        return minDocId;
    }

    //Check if the query processing is finished, i.e. all the posting lists has been fully processed
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
