package unipi.mircv;

import java.util.Arrays;

/**
 * Represents a parsed document with essential information.
 */
public class DocParsed {

    long docId; // Document ID assigned during parsing
    int documentLength; // Length of the document in terms

    String docNo; // Document number
    String[] terms; // Array containing the terms extracted from the document


    /**
     * Constructor for creating a DocParsed instance with document number and terms.
     * Automatically sets the document length based on the number of terms.
     *
     * @param docNo Document number
     * @param terms Array of terms extracted from the document
     */
    public DocParsed(String docNo, String[] terms){
        this.docNo = docNo;
        this.terms = terms;
        documentLength = terms.length;
    }

    /**
     * Constructor for creating a DocParsed instance with document ID, terms, and document number.
     *
     * @param docId           Document ID assigned during parsing
     * @param terms           Array of terms extracted from the document
     * @param docNo           Document number
     */
    public DocParsed(int docId, String[] terms, String docNo) {
        this.docId = docId;
        this.documentLength = terms.length;
        this.terms = terms;
        this.docNo = docNo;
    }

    public void setDocId(int docId) {
        this.docId = docId;
    }

    /**
     * Returns a string representation of the DocParsed object.
     *
     * @return String representation of the object
     */
    @Override
    public String toString() {
        return "DocParsed{" +
                "docId=" + docId +
                ", documentLength=" + documentLength +
                ", docNo='" + docNo + '\'' +
                ", terms=" + Arrays.toString(terms) +
                '}';
    }

    public long getDocId() {
        return docId;
    }

    public String[] getTerms() {
        return terms;
    }

    public int getDocumentLength() {
        return documentLength;
    }

    public String getDocNo() {
        return docNo;
    }
}
