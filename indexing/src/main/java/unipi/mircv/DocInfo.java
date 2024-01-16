package unipi.mircv;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

/**
 * Represents information about a document, including its document number, length, URL, and PageRank score.
 */
public class DocInfo {

    private String docNo;
    private int docLen;
    private String url;
    private float pr_score;


    /**
     * Constructor for creating a DocInfo instance with document number and length.
     *
     * @param docNo  Document number
     * @param docLen Document length
     */
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

    /**
     * Writes the document information to a random access file.
     *
     * @param randomAccessFile Random access file to write the information
     * @param docId            Document ID to associate with the entry
     */
    public void writeFile(RandomAccessFile randomAccessFile, int docId) {

        //Fill with whitespaces to mantain the length standard
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

    /**
     * Fills the given text with whitespaces to achieve the specified length.
     *
     * @param text   Text to fill
     * @param length Desired length
     * @return Filled text
     */
    public static String fillspace(String text, int length) {
            return String.format("%" + length + "." + length + "s", text);
    }


    /**
     * Reads from the document index the document length associated with the given document ID.
     *
     * @param documentIndexFile Random access file containing the document index
     * @param docId             Document ID for which to retrieve the entry
     * @return Document length associated with the document ID
     */
    public static int getDocLenFromFile(RandomAccessFile documentIndexFile, long docId){

        //Accumulator for the current offset in the file
        long offset = (docId - 1)* Parameters.DOCUMENT_INDEX_ENTRY_BYTE + Parameters.DOCID_BYTE + Parameters.DOCNO_BYTE;

        try {
            //Move to the correct offset
            documentIndexFile.seek(offset);

            //Read the length of the document, 4 bytes starting from the offset
            return documentIndexFile.readInt();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }




}
