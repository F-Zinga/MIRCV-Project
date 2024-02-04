package unipi.mircv;

/**
 * Represents a posting in the inverted index, containing information about a document ID and its term frequency.
 */

public class Posting {

    private int docID; // Document ID associated with the posting
    private int termFrequency; // Term frequency of the term in the document


    /**
     * Constructs a Posting with the specified document ID and term frequency.
     * @param docID The document ID associated with the posting.
     * @param termFrequency The term frequency of the term in the document.
     */
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
