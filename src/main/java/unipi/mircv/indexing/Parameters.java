package unipi.mircv.indexing;

public interface Parameters {

    //Length in bytes of the docNo field
    public static int DOCNO_BYTE = 48;

    //Length in bytes of the docLen field
    public static int DOCLEN_BYTE = 4;

    //Length in bytes of the docId
    public static int DOCID_BYTE = 8;

    //long + string[48] + int
    public static int DOCUMENT_INDEX_ENTRY_BYTE = DOCID_BYTE + DOCNO_BYTE + DOCLEN_BYTE;

    //
    public static String DOCINDEX_PATH = "resources/utility/<da aggiungere nome file>";

    public static int TERM_LENGTH = 48;


}
