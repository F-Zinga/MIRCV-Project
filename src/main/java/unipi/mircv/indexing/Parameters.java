package unipi.mircv.indexing;

public interface Parameters {

    //Length in bytes of the docNo field
     int DOCNO_BYTE = 48;

    //Length in bytes of the docLen field
     int DOCLEN_BYTE = 4;

    //Length in bytes of the docId
     int DOCID_BYTE = 8;

    //long + string[48] + int
     int DOCUMENT_INDEX_ENTRY_BYTE = DOCID_BYTE + DOCNO_BYTE + DOCLEN_BYTE;

    //
     String DOCINDEX_PATH = "resources/utility/<da aggiungere nome file>";

    //Length in bytes of the term field
     int TERM_BYTES = 48;

    //Length in bytes of the offsetDocId field
     int OFFSET_DOCIDS_BYTES = 8;

    //Length in bytes of the frequency length field
     int OFFSET_FREQUENCIES_BYTES = 8;

    // in Term class
     int DOCID_BYTES = 4;

     int FREQUENCY_BYTES = 4;

    //Length in bytes of the postingListLength field
     int POSTING_LIST_BYTES = 4;
     int IDF_BYTES = 8;

     int OFFSET_SKIPBLOCKS_BYTES = 8;
     int NUMBER_OF_SKIPBLOCKS_BYTES = 4;

     int MAXSCORE_BYTES = 4;

     int TERM_INFO_LENGTH = TERM_BYTES + OFFSET_DOCIDS_BYTES + OFFSET_SKIPBLOCKS_BYTES+
            NUMBER_OF_SKIPBLOCKS_BYTES + OFFSET_FREQUENCIES_BYTES + DOCID_BYTE+
            FREQUENCY_BYTES + POSTING_LIST_BYTES + IDF_BYTES + MAXSCORE_BYTES + MAXSCORE_BYTES;


    // in Block class
     int OFFSET_LENGTH = 8;

     int SKIP_BLOCK_DIMENSION_LENGTH = 4;

     int MAX_DOC_ID_LENGTH = 8;

    //Length in byte of each skip block (32)
     int SKIP_BLOCK_LENGTH = 2*OFFSET_LENGTH + 2*SKIP_BLOCK_DIMENSION_LENGTH + MAX_DOC_ID_LENGTH;



}
