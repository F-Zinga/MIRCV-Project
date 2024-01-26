package unipi.mircv;

import java.util.PriorityQueue;
import java.util.Stack;
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

    public void add(DocsRanked score){
        if(queue.size() == dimension){ //Check if the last element in the priorityQueue is lower then the element that i want to insert
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

    public boolean isFull(){
        return queue.size() == dimension;
    }

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

        System.out.print("[ ");
        // Print the elements in the stack (in reverse order)
        while (!stack.isEmpty()) {
            System.out.print("(" + stack.pop() + "), ");
        }
        System.out.println(" ]");
    }
}
