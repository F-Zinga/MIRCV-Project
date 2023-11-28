package unipi.mircv.indexing;

public class Term {

    private int docFreq;
    private int termFreq;
    private int offsets;

    public Term(int docFreq, int termFreq, int offsets) {
        this.docFreq = docFreq;
        this.termFreq = termFreq;
        this.offsets = offsets;
    }

    public String toString() {
        return "docFreq: " + docFreq + " termFreq: " + termFreq + " offsets: " + offsets;
    }

    public int getDocFreq() {
        return docFreq;
    }

    public int getTermFreq() {
        return termFreq;
    }

    public int getOffsets() {
        return offsets;
    }

    public void setDocFreq(int docFreq) {
        this.docFreq = docFreq;
    }

    public void setTermFreq(int termFreq) {
        this.termFreq = termFreq;
    }

    public void setOffsets(int offsets) {
        this.offsets = offsets;
    }



}

