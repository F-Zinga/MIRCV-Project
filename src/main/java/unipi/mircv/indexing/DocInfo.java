package unipi.mircv.indexing;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

import unipi.mircv.indexing.Parameters;

public class DocInfo {

    private String docNo;
    private int docLen;
    private String url;
    private float pr_score;



    public DocInfo(String docNo, int docLen) {
        this.docNo = docNo;
        this.docLen = docLen;
    }

    public String getDocNo() {
        return docNo;
    }

    public int getDocLen() {
        return docLen;
    }

    public String getUrl() {
        return url;
    }

    public float getPr_score() {
        return pr_score;
    }

    public void writeFile(RandomAccessFile randomAccessFile, int docId) {

        //Fill with whitespaces to keep the length standard
        String tmp = fillspace(this.docNo, Parameters.DOCNO_BYTE);

        //Instantiating the ByteBuffer to write to the file
        byte[] docIdBytes = ByteBuffer.allocate(Parameters.DOCID_BYTE).putLong(docId).array();
        byte[] docNoBytes = ByteBuffer.allocate(Parameters.DOCNO_BYTE).put(tmp.getBytes()).array();
        byte[] docLenBytes = ByteBuffer.allocate(Parameters.DOCLEN_BYTE).putInt(this.docLen).array();

        try {
            randomAccessFile.write(docIdBytes);
            randomAccessFile.write(docNoBytes);
            randomAccessFile.write(docLenBytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
        public static String fillspace(String text, int length) {
            return String.format("%" + length + "." + length + "s", text);
        }
}
