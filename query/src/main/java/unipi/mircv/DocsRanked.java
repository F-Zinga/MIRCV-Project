package unipi.mircv;


/**
 This class is designed to manage pairs (docId, score),
 * The elements are ordered in decreasing order of score (where the score determines the priority of the docId)
 */

public class DocsRanked  implements Comparable<DocsRanked>{
    private int key;
    private double value;

    public DocsRanked(int key, double value){
        this.key = key;
        this.value = value;
    }

    public void setKey(int key) {this.key = key;}

    public int getKey() {return key;}

   /* public void setValue(double value) {this.value = value;} */

    public double getValue() {return value;}

    //Used to compare the value of the priorityQueue
    @Override
    public int compareTo(DocsRanked other) {
        return Double.compare(this.value, other.getValue());
    }

    @Override
    public String toString() {
        return key + " " + value;
    }
}
