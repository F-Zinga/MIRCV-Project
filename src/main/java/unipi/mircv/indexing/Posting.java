package unipi.mircv.indexing;

public class Posting {

    private long docID;
    private int termFrequency;


    public Posting(long docID, int termFrequency) {
        this.docID = docID;
        this.termFrequency = termFrequency;
    }

    public long getDocID() {
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

    public void updateTermFrequency() { termFrequency++; }

    @Override
    public String toString() {
        return "Posting{" +
                "docID=" + docID +
                ", termFrequency=" + termFrequency +
                '}';
    }


}
