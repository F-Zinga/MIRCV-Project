package unipi.mircv;

import org.javatuples.Pair;

import java.util.PriorityQueue;


/**
 This class extends the PriorityQueue class, designed to manage pairs (docId, score),
 * The elements are ordered in decreasing order of score (where the score determines the priority of the docId)
 * It also handles a threshold score used for pruning in the MaxScore.
 */

public class DocsRanked extends PriorityQueue<Pair<Integer, Double>> {

        private int doc; // Document identifier
        private double value;  // Final score


    /**
     * Constructs a DocsRanked object with a specified document ID and score.
     *
     * @param doc   The document identifier.
     * @param value The final score.
     */
    public DocsRanked(int doc, double value) {
        super((o1, o2) -> o2.getValue1().compareTo(o1.getValue1()));
        this.doc = doc;
        this.value = value;

    }

    public double getValue() {return value;}




    /*
        * Adds the specified element to the queue.
         * @param longDoubleTuple element to be added to the list
         * @return the boolean result of the add operation

        @Override
        public boolean add(Pair<Integer, Double> longDoubleTuple) {

            boolean result = super.add(longDoubleTuple);

            //update the threshold, if the list has at least K elements
            if(result && this.size() >= K){

                Iterator<Pair<Integer, Double>> iterator = iterator();
                int counter = 0;

                //traverse in descending order the list until the K-1th element
                while(iterator.hasNext()){
                    iterator.next();
                    counter++;
                    if(counter == K - 1){
                        break;
                    }
                }

                //updates the threshold with the value of the Kth element
                this.threshold = iterator.next().getValue1();

            }
            return result;
        }

        /**
         * Returns the current threshold
         * @return return the current threshold
         */

}
