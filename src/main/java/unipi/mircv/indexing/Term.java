package unipi.mircv.indexing;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

public class Term {

    private int docFreq;
    private int termFreq;
    private String term;
    private long offsetDocId;
    private long offsetFrequency;
    private long offsetSkipBlock;
    private int numberOfSkipBlocks;
    private double idf;
    private int docIdsBytesLength;

    private int frequenciesBytesLength;
    private int postingListLength;

    private int tfidfTermUpperBound;

    private int bm25TermUpperBound;

    public Term(String term, long offsetDocId, long offsetFrequency, double idf, int docIdsBytesLength, int frequenciesBytesLength, int postingListLength, long offsetSkipBlock, int numberOfSkipBlocks, int tfidfTermUpperBound, int bm25TermUpperBound) {
        this.term = term;
        this.offsetDocId = offsetDocId;
        this.offsetFrequency = offsetFrequency;
        this.idf = idf;
        this.docIdsBytesLength = docIdsBytesLength;
        this.frequenciesBytesLength = frequenciesBytesLength;
        this.postingListLength = postingListLength;
        this.numberOfSkipBlocks = numberOfSkipBlocks;
        this.offsetSkipBlock = offsetSkipBlock;
        this.tfidfTermUpperBound = tfidfTermUpperBound;
        this.bm25TermUpperBound = bm25TermUpperBound;
    }

    public Term() {
        this.docFreq = 0;
        this.termFreq = 0;
    }

    public String toString() {
        return "docFreq: " + docFreq + " termFreq: " + termFreq;
    }

    public int getDocFreq() {
        return docFreq;
    }

    public int getTermFreq() {
        return termFreq;
    }

    public String getTerm() { return term; }

    public void setDocFreq(int docFreq) {
        this.docFreq = docFreq;
    }

    public void setTermFreq(int termFreq) {
        this.termFreq = termFreq;
    }

    public static String fillspace(String text, int length) {
        return String.format("%" + length + "." + length + "s", text);
    }

    /**
     * Write the term info to a file. This method is used during the building of the partial blocks.
     * @param lexiconFile Is the random access file on which the term info is written.
     * @param key Term to be written.
     * @param term Information of the term to be written.
     */
    public void writeToFile(RandomAccessFile lexiconFile, String key, Term term){

        //Fill with whitespaces to keep the length standard
        String tmp = fillspace(key, Parameters.TERM_BYTES);


        byte[] t = ByteBuffer.allocate(Parameters.TERM_BYTES).put(tmp.getBytes()).array();
        byte[] offsetDocId = ByteBuffer.allocate(Parameters.OFFSET_DOCIDS_BYTES).putLong(term.getOffsetDocId()).array();
        byte[] offsetFrequency = ByteBuffer.allocate(Parameters.OFFSET_FREQUENCIES_BYTES).putLong(term.getOffsetFrequency()).array();
        byte[] postingListLength = ByteBuffer.allocate(Parameters.POSTING_LIST_BYTES).putInt(term.getPostingListLength()).array();

        try {
            lexiconFile.write(t);
            lexiconFile.write(offsetDocId);
            lexiconFile.write(offsetFrequency);
            lexiconFile.write(postingListLength);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public int getTfidfTermUpperBound() {
        return tfidfTermUpperBound;
    }

    public void setTfidfTermUpperBound(int tfidfTermUpperBound) {
        this.tfidfTermUpperBound = tfidfTermUpperBound;
    }

    public int getBm25TermUpperBound() {
        return bm25TermUpperBound;
    }

    public void setBm25TermUpperBound(int bm25TermUpperBound) {
        this.bm25TermUpperBound = bm25TermUpperBound;
    }

    public int getPostingListLength() {
        return postingListLength;
    }

    public void setPostingListLength(int postingListLength) {
        this.postingListLength = postingListLength;
    }

    public int getFrequenciesBytesLength() {
        return frequenciesBytesLength;
    }

    public void setFrequenciesBytesLength(int frequenciesBytesLength) {
        this.frequenciesBytesLength = frequenciesBytesLength;
    }

    public int getDocIdsBytesLength() {
        return docIdsBytesLength;
    }

    public void setDocIdsBytesLength(int docIdsBytesLength) {
        this.docIdsBytesLength = docIdsBytesLength;
    }

    public long getOffsetDocId() {
        return offsetDocId;
    }

    public void setOffsetDocId(long offsetDocId) {
        this.offsetDocId = offsetDocId;
    }

    public long getOffsetFrequency() {
        return offsetFrequency;
    }

    public void setOffsetFrequency(long offsetFrequency) {
        this.offsetFrequency = offsetFrequency;
    }

    public long getOffsetSkipBlock() {
        return offsetSkipBlock;
    }

    public void setOffsetSkipBlock(long offsetSkipBlock) {
        this.offsetSkipBlock = offsetSkipBlock;
    }

    public int getNumberOfSkipBlocks() {
        return numberOfSkipBlocks;
    }

    public void setNumberOfSkipBlocks(int numberOfSkipBlocks) {
        this.numberOfSkipBlocks = numberOfSkipBlocks;
    }

    public double getIdf() {
        return idf;
    }

    public void setIdf(double idf) {
        this.idf = idf;
    }
}

