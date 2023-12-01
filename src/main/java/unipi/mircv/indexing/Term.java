package unipi.mircv.indexing;

public class Term {

    private int docFreq;
    private int termFreq;
    private int offset;
    private String term;

    public Term(int docFreq, int termFreq, int offset) {
        this.docFreq = docFreq;
        this.termFreq = termFreq;
        this.offset = offset;
    }

    public Term() {
        this.docFreq = 0;
        this.termFreq = 0;
        this.offset = 0;
    }

    public String toString() {
        return "docFreq: " + docFreq + " termFreq: " + termFreq + " offset: " + offset;
    }

    public int getDocFreq() {
        return docFreq;
    }

    public int getTermFreq() {
        return termFreq;
    }

    public int getOffset() {
        return offset;
    }

    public String getTerm() { return term; }

    public void setDocFreq(int docFreq) {
        this.docFreq = docFreq;
    }

    public void setTermFreq(int termFreq) {
        this.termFreq = termFreq;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    //TODO: metodo da fare a doppio
    // public void writeToFile(random)



}

