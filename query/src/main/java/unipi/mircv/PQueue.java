package unipi.mircv;

import java.util.PriorityQueue;
import java.util.Stack;

/**
 * Represents a priority queue for storing ranked documents. Uses a PriorityQueue to maintain the top-k ranked documents.
 */
public class PQueue {
    PriorityQueue<DocsRanked> queue;
    int dimension;
    public PQueue(int k){
        this.dimension = k;
        this.queue = new PriorityQueue<>(k);
    }

    public PriorityQueue<DocsRanked> getQueue(){
        return this.queue;
    }

    /**
     * Adds a DocsRanked object to the priority queue, maintaining the top-k elements.
     *
     * @param score The DocsRanked object to add.
     */
    public void add(DocsRanked score){
        if(queue.size() == dimension){ //Check if the last element in the priorityQueue is lower than the element that i want to insert
            assert queue.peek() != null;
            if(queue.peek().getValue() < score.getValue()){
                queue.poll();
                queue.add(score);
            }
        }
        else{
            queue.add(score);
        }
    }

    /**
     * Checks if the priority queue is full (contains k elements).
     *
     * @return true if the queue is full, false otherwise.
     */
    public boolean isFull(){
        return queue.size() == dimension;
    }

    /**
     * Peeks at the top DocsRanked object in the priority queue without removing it.
     *
     * @return The top DocsRanked object.
     */
    public DocsRanked peek(){
        return queue.peek();
    }

    public int size(){
        return queue.size();
    }

    public void printResults() {
        Stack<DocsRanked> stack = new Stack<>();
        PriorityQueue<DocsRanked> copy = new PriorityQueue<>(queue);

        // Iterate through the priority queue and add each element to the stack
        while (!copy.isEmpty()) {
            stack.push(copy.poll());
        }

        System.out.print("\nPOSITION DOCID SCORE");
        System.out.print("\n--------------------------\n");
        // Print the elements in the stack (in reverse order)
        int i=1;
        while (!stack.isEmpty()) {
            System.out.print(i);
            i += 1;
            System.out.print( ") " + stack.pop() + "\n");
        }
    }
}
