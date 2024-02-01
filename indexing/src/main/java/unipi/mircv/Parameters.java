package unipi.mircv;

public interface Parameters {

    /**
    //Length in bytes of the docNo field
     int DOCNO_BYTE = 48;

    //Length in bytes of the docLen field
     int DOCLEN_BYTE = 4;

    //Length in bytes of the docId
     int DOCID_BYTE = 8;

    //long + string[48] + int
     int DOCUMENT_INDEX_ENTRY_BYTE = DOCID_BYTE + DOCNO_BYTE + DOCLEN_BYTE;
*/

/**
    //Length in bytes of the term field
     int TERM_BYTES = 48;

    //Length in bytes of the offsetDocId field
     int OFFSET_DOCIDS_BYTES = 8;

    //Length in bytes of the frequency length field
     int OFFSET_FREQUENCIES_BYTES = 8;


     // in Term class
     int TERM_DOCID_BYTES = 4;

     int FREQUENCY_BYTES = 4;

    //Length in bytes of the postingListLength field
     int POSTING_LIST_BYTES = 4;
     int IDF_BYTES = 8;

     int OFFSET_BLOCKS_BYTES = 8;
     int N_BLOCKS_BYTES = 4;

     int MAXSCORE_BYTES = 4;

     int TERM_INFO_LENGTH = TERM_BYTES + OFFSET_DOCIDS_BYTES + OFFSET_BLOCKS_BYTES+
            N_BLOCKS_BYTES + OFFSET_FREQUENCIES_BYTES + TERM_DOCID_BYTES+
            FREQUENCY_BYTES + POSTING_LIST_BYTES + IDF_BYTES + MAXSCORE_BYTES + MAXSCORE_BYTES;

*/
/**
     // in Block class
     int OFFSET_LENGTH = 8;

     int BLOCK_DIMENSION_LENGTH = 4;

     int MAX_DOC_ID_LENGTH = 8;

    //Length in byte of each skip block (32)
     int BLOCK_LENGTH = 2*OFFSET_LENGTH + 2*BLOCK_DIMENSION_LENGTH + MAX_DOC_ID_LENGTH;
*/

     // Statistics
    double K1 = 1.6;
    double B = 0.75;


    // path merger text
    String LEXICON_PATH = "Output/Lexicon/lexicon.txt";
    String STATISTICS_TEXTPATH = "Output/CollectionStatistics/collectionStatistics.txt";
    String SKIPPOINTERS_TEXTPATH = "Output/Skipping/skipPointers.txt";
    String LASTDOCID_TEXTPATH = "Output/Skipping/lastDocIds.txt";
    String DOCID_TEXTPATH = "Output/DocIds/docIds.txt";
    String FREQ_TEXTPATH = "Output/Frequencies/freq.txt";
    String DOCINDEX_TEXTPATH = "Output/DocumentIndex/documentIndex.txt";



    // path merger byte
    String SKIPPOINTERS_BYTEPATH = "Output/Skipping/skipPointers.dat";
    String LASTDOCID_BYTEPATH = "Output/Skipping/lastDocIds.dat";
    String DOCID_BYTEPATH = "Output/DocIds/docIds.dat";
    String FREQ_BYTEPATH = "Output/Frequencies/freq.dat";
    String DOCINDEX_BYTEPATH = "Output/DocumentIndex/documentIndex.dat";

    // path evaluation
    String EVALUATION_RESULTS = "Output/queryResults.txt";
    //Test
    //Path of the dataset
    //String COLLECTION_PATH = "D://collection.tar.gz";

    //String FILES_PATH = "Files/";
    //Document index file path
    //String DOCUMENT_INDEX_PATH = "Files/document_index.txt";

   /** String ARGS_ERROR = "Select one from the following arguments:\n" +
            "-s : stemming and stopwords removal enabled\n" +
            "-c : compression enabled\n" +
            "-sc : both enabled";
*/
    //Percentage of memory used to define a threshold
    //double PERCENTAGE = 0.9;

}
