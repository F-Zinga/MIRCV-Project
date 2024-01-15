package unipi.mircv;
import org.javatuples.Pair;
import java.util.Iterator;
import java.util.PriorityQueue;


/**
 This class extends the PriorityQueue class and is designed to manage pairs (docId, score),
 * where the score determines the priority of the docId. The elements are ordered in decreasing order of score.
 * It also handles a threshold score used for pruning purposes (MaxScore).
 */

public class DocsRanked extends PriorityQueue<Pair<Long, Double>> {

        //Threshold to begin one of the top K documents
        private double threshold;

        // Number of top K elements
        private final int K;

        /**
         * Construct a priority queue to track the top K documents
         * @param K number of desired elements at the top of the queue
         */
        public DocsRanked(int K){

            //Order in descending order of score the top K documents
            super((o1, o2) -> o2.getValue1().compareTo(o1.getValue1()));

            //At the beginning the threshold is 0
            this.threshold = 0;

            this.K = K;
        }

    /**
        * Adds the specified element to the queue.
         * @param longDoubleTuple element to be added to the list
         * @return the boolean result of the add operation
         */
        @Override
        public boolean add(Pair<Long, Double> longDoubleTuple) {

            boolean result = super.add(longDoubleTuple);

            //update the threshold, if the list has at least K elements
            if(result && this.size() >= K){

                Iterator<Pair<Long, Double>> iterator = iterator();
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
        public double getThreshold() {
            return threshold;
        }
}
