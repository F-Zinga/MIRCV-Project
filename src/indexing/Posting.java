package indexing;

public class Posting {

    private int docID;
    private int termFrequency;


    public Posting(int docID, int termFrequency) {
        this.docID = docID;
        this.termFrequency = termFrequency;
    }

    public int getDocID() {
        return docID;
    }

    public int getTermFrequency() {
        return termFrequency;
    }

    public void setDocID(int docID) {
        this.docID = docID;
    }

    public void setTermFrequency(int termFrequency) {
        this.termFrequency = termFrequency;
    }

    @Override
    public String toString() {
        return "Posting{" +
                "docID=" + docID +
                ", termFrequency=" + termFrequency +
                '}';
    }


}
