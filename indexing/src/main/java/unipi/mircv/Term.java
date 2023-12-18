package unipi.mircv;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

public class Term {

    //private int docFreq;
    //private int termFreq;
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

    public Term(String term, long offsetDocId, long offsetFrequency, int postingListLength) {
        this.term = term;
        this.offsetDocId = offsetDocId;
        this.offsetFrequency = offsetFrequency;
        this.postingListLength = postingListLength;
    }

    public Term() {

    }

    public String getTerm() { return term; }

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

    /**
     * Write the term info to a file. This method is used during the merge of the partial blocks, here we have
     * all the information directly inside the termInfo object.
     * @param lexiconFile Is the random access file on which the term info is written.
     * @param termInfo Information of the term to be written.
     */
    public void writeToFile(RandomAccessFile lexiconFile, Term termInfo){
        //Fill with whitespaces to keep the length standard
        String tmp = fillspace(termInfo.getTerm(), Parameters.TERM_BYTES);

        byte[] term = ByteBuffer.allocate(Parameters.TERM_BYTES).put(tmp.getBytes()).array();
        byte[] offsetDocId = ByteBuffer.allocate(Parameters.OFFSET_DOCIDS_BYTES).putLong(termInfo.getOffsetDocId()).array();
        byte[] offsetFrequency = ByteBuffer.allocate(Parameters.OFFSET_FREQUENCIES_BYTES).putLong(termInfo.getOffsetFrequency()).array();
        byte[] bytesDocId = ByteBuffer.allocate(Parameters.TERM_DOCID_BYTES).putInt(termInfo.getDocIdsBytesLength()).array();
        byte[] bytesFrequency = ByteBuffer.allocate(Parameters.FREQUENCY_BYTES).putInt(termInfo.getFrequenciesBytesLength()).array();
        byte[] postingListLength = ByteBuffer.allocate(Parameters.POSTING_LIST_BYTES).putInt(termInfo.getPostingListLength()).array();
        byte[] idf = ByteBuffer.allocate(Parameters.IDF_BYTES).putDouble(termInfo.getIdf()).array();
        byte[] offsetSkipBlocks = ByteBuffer.allocate(Parameters.OFFSET_BLOCKS_BYTES).putLong(termInfo.getOffsetSkipBlock()).array();
        byte[] numberOfSkipBlocks = ByteBuffer.allocate(Parameters.N_BLOCKS_BYTES).putInt(termInfo.getNumberOfSkipBlocks()).array();
        byte[] tfidfTermUpperBound = ByteBuffer.allocate(Parameters.MAXSCORE_BYTES).putInt(termInfo.getTfidfTermUpperBound()).array();
        byte[] bm25TermUpperBound = ByteBuffer.allocate(Parameters.MAXSCORE_BYTES).putInt(termInfo.getBm25TermUpperBound()).array();

        try {
            lexiconFile.write(term);
            lexiconFile.write(offsetDocId);
            lexiconFile.write(offsetFrequency);
            lexiconFile.write(idf);
            lexiconFile.write(bytesDocId);
            lexiconFile.write(bytesFrequency);
            lexiconFile.write(postingListLength);
            lexiconFile.write(offsetSkipBlocks);
            lexiconFile.write(numberOfSkipBlocks);
            lexiconFile.write(tfidfTermUpperBound);
            lexiconFile.write(bm25TermUpperBound);

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

    public void setTermFreq(int size) {
    }
}

