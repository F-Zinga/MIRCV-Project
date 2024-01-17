package unipi.mircv;

import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.util.HashMap;

/**
 * Represents a document index, extending HashMap to map document IDs to document information.
 */
public class DocIndex extends HashMap<Long, DocInfo> {

    // Path to the document index file
    public final static String DOCUMENT_INDEX_PATH = "Files/document_index.txt";

    /**
     * Default construct that calls the HashMap constructor
     */
    public DocIndex(){
        super();
    }

    /**
     * Loads the document index into memory from the document index file.
     */
    public void loadDocumentIndex() {
        System.out.println(" *** loadDocumentIndex... ***");
        try (//Object to open the stream from the document index file
             RandomAccessFile documentIndexFile = new RandomAccessFile(DOCUMENT_INDEX_PATH, "r")){

            //Accumulator for the current offset in the file
            int offset = 0;

            //Array of bytes to store the docno
            byte[] docnoBytes = new byte[Parameters.DOCNO_BYTE];

            long docid;

            int docLength;

            String docno;

            // Iterate until the end of the file
            while (offset < (documentIndexFile.length())) {

                //Read the docid from the first 8 bytes starting from the offset
                docid = documentIndexFile.readLong();

                //Read the first DOCUMENT_INDEX_DOCNO_LENGTH bytes containing the docno
                documentIndexFile.readFully(docnoBytes, 0, Parameters.DOCNO_BYTE);

                //Convert the bytes to a string and trim it
                docno = new String(docnoBytes, Charset.defaultCharset()).trim();

                //Read the length of the document, 4 bytes starting from the offset
                docLength = documentIndexFile.readInt();

                //Insert the docInfo into the HashMap
                this.put(docid, new DocInfo(docno,docLength));

                //Increment the offset
                offset += Parameters.DOCUMENT_INDEX_ENTRY_BYTE;
            }

            System.out.println("*** loadedDocumentIndex ***");

        } catch (Exception e) {
            System.out.println("*** Error loading  documentIndex: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
