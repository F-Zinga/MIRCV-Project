package unipi.mircv.indexing;

import java.util.Arrays;

public class DocParsed {

    long docId;
    int documentLength;

    String docNo;
    String[] terms;


    public DocParsed(String docNo, String[] terms){
        this.docNo = docNo;
        this.terms = terms;
        documentLength = terms.length;
    }

    public DocParsed(int docId, String[] terms, String docNo) {
        this.docId = docId;
        this.documentLength = terms.length;
        this.terms = terms;
        this.docNo = docNo;
    }

    public void setDocId(int docId) {
        this.docId = docId;
    }

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
