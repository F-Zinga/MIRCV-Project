package unipi.mircv;

public interface Parameters {

    //Length in bytes of the docNo field
     int DOCNO_BYTE = 48;

    //Length in bytes of the docLen field
     int DOCLEN_BYTE = 4;

    //Length in bytes of the docId
     int DOCID_BYTE = 8;

    //long + string[48] + int
     int DOCUMENT_INDEX_ENTRY_BYTE = DOCID_BYTE + DOCNO_BYTE + DOCLEN_BYTE;


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

     int OFFSET_BLOCKS_BYTES = 8;
     int N_BLOCKS_BYTES = 4;

     int MAXSCORE_BYTES = 4;

     int TERM_INFO_LENGTH = TERM_BYTES + OFFSET_DOCIDS_BYTES + OFFSET_BLOCKS_BYTES+
            N_BLOCKS_BYTES + OFFSET_FREQUENCIES_BYTES + DOCID_BYTE+
            FREQUENCY_BYTES + POSTING_LIST_BYTES + IDF_BYTES + MAXSCORE_BYTES + MAXSCORE_BYTES;



     // in Block class
     int OFFSET_LENGTH = 8;

     int BLOCK_DIMENSION_LENGTH = 4;

     int MAX_DOC_ID_LENGTH = 8;

    //Length in byte of each skip block (32)
     int BLOCK_LENGTH = 2*OFFSET_LENGTH + 2*BLOCK_DIMENSION_LENGTH + MAX_DOC_ID_LENGTH;


     // Statistics
    double K1 = 1.6;
    double B = 0.75;


    // merger
    String II_DOCID_BLOCK_PATH = "Blocks/invertedIndexDocIds/";
    String II_FREQ_BLOCK_PATH = "Blocks/invertedIndexFrequencies/";
    String LEXICON_BLOCK_PATH = "Blocks/lexiconBlock/";
    String LEXICON_PATH = "Files/lexicon.txt";
    String II_DOCID_PATH = "Files/docids.txt";
    String II_FREQ_PATH = "Files/frequencies.txt";
    String BLOCKS_PATH = "Files/skipblocks.txt";


    //Test
    //Path of the dataset
    String COLLECTION_PATH = "C://Users//kecco//Desktop//collection.tar.gz"; // "C:\Users\kecco\Desktop\collection.tar.gz"

    String FILES_PATH = "Files/";
    //Document index file path
    String DOCUMENT_INDEX_PATH = "Files/document_index.txt";

    String ARGS_ERROR = "Select one from the following arguments:\n" +
            "-s : stemming and stopwords removal enabled\n" +
            "-c : compression enabled\n" +
            "-sc : both enabled";

    //Percentage of memory used to define a threshold
    static final double PERCENTAGE = 0.7;

}
