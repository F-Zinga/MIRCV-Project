package unipi.mircv;

import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.util.HashMap;

public class DocIndex extends HashMap<Long, DocInfo> {

    //Path to the document index file
    //TODO create folder Files or similar and change path accordingly
    public final static String DOCUMENT_INDEX_PATH = "Files/document_index.txt";

    /**
     * Construct that calls the HashMap constructor
     */
    public DocIndex(){
        super();
    }

    /**
     * Load the document index in memory.
     */
    public void loadDocumentIndex() {
        System.out.println("[DOCUMENT INDEX LOADER] Document index loading");
        try (//Object to open the stream from the document index file
             RandomAccessFile documentIndexFile = new RandomAccessFile(DOCUMENT_INDEX_PATH, "r")){
            //Start the stream from the document index file

            //Accumulator for the current offset in the file
            int offset = 0;

            //Array of bytes in which put the docno
            byte[] docnoBytes = new byte[Parameters.DOCNO_BYTE];

            long docid;

            int docLength;

            String docno;

            //System.out.println(documentIndexFile.length());
            //While we're not at the end of the file
            while (offset < (documentIndexFile.length())) {

                //Read the docid from the first 8 bytes starting from the offset
                docid = documentIndexFile.readLong();

                //Read the first DOCUMENT_INDEX_DOCNO_LENGTH bytes containing the docno
                documentIndexFile.readFully(docnoBytes, 0, Parameters.DOCNO_BYTE);

                //Convert the bytes to a string and trim it
                docno = new String(docnoBytes, Charset.defaultCharset()).trim();

                //Read the length of the document, 4 bytes starting from the offset
                docLength = documentIndexFile.readInt();

                //Insert the termInfo into the HashMap
                this.put(docid, new DocInfo(docno,docLength));

                //Increment the offset
                offset += Parameters.DOCUMENT_INDEX_ENTRY_BYTE;
            }

            System.out.println("[DOCUMENT INDEX LOADER] Document index loaded");

        } catch (Exception e) {
            System.out.println("[DOCUMENT INDEX LOADER] Error loading the document index: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
