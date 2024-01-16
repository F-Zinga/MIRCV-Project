package unipi.mircv;

/**
 * Represents a posting in the inverted index, containing information about a document ID and its term frequency.
 */

public class Posting {

    private long docID; // Document ID associated with the posting
    private int termFrequency; // Term frequency of the term in the document


    /**
     * Constructs a Posting with the specified document ID and term frequency.
     * @param docID The document ID associated with the posting.
     * @param termFrequency The term frequency of the term in the document.
     */
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

    public void setDocID(long docID) {
        this.docID = docID;
    }

    public void setTermFrequency(int termFrequency) {
        this.termFrequency = termFrequency;
    }

    public void updateTermFrequency() { termFrequency++; }

    /**
     * Returns a string representation of the Posting object.
     * @return A string containing the document ID and term frequency.
     */
    @Override
    public String toString() {
        return "Posting{" +
                "docID=" + docID +
                ", termFrequency=" + termFrequency +
                '}';
    }


}
