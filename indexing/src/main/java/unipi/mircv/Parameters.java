package unipi.mircv;

public interface Parameters {


    // Score Function (bm25)
    double K1 = 1.6;
    double B = 0.75;

    // collection path
    String COLLECTION_PATH = "D:/collection.tar";


    // path merger
    String LEXICON_PATH = "D:/Output/Lexicon/lexicon.txt";      //only text
    String STATISTICS_PATH = "D:/Output/Statistics/Statistics.txt";     //only text
    String SKIPPOINTERS_PATH = "D:/Output/Skipping/skipPointers";
    String LASTDOCID_PATH = "D:/Output/Skipping/lastDocIds";
    String DOCID_PATH = "D:/Output/DocIds/docIds";
    String FREQ_PATH = "D:/Output/Frequencies/freq";
    String DOCINDEX_PATH = "D:/Output/DocumentIndex/documentIndex";


    // path write blocks
    String LEXICON_BLOCK_PATH = "D:/Output/Lexicon/lexicon";
    String DOCIDS_BLOCK_PATH = "D:/Output/DocIds/docIds";
    String FREQ_BLOCK_PATH = "D:/Output/Frequencies/freq";
    String DOCUMENT_INDEX_BLOCK_PATH = "D:/Output/DocumentIndex/documentIndex";


    // path evaluation
    String EVALUATION_RESULTS = "D:/Output/queryResults.txt";

}
