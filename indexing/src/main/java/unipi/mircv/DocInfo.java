package unipi.mircv;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Represents information about a document, including its document number, length, URL, and PageRank score.
 */
public class DocInfo {

    private int docNo;
    private int docLen;


    /**
     * Constructor for creating a DocInfo instance with document number and length.
     *
     * @param docNo  Document number
     * @param docLen Document length
     */
    public DocInfo(int docNo, int docLen) {
        this.docNo = docNo;
        this.docLen = docLen;
    }

    public int getDocNo() {
        return docNo;
    }

    public int getDocLen() {
        return docLen;
    }



}
