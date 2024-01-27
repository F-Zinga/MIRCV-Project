package unipi.mircv;

import javax.print.Doc;
import java.util.*;

/**
 * The QueryProcessor class handles the processing of queries, interacting with various components,
 * such as the lexicon, document index, and collection statistics.
 */
public class QueryProcessor {
    public ManagerIO managerIO;
    public Lexicon lexicon;
    public Statistics statistics;
    public DocIndex docIndex;
    public int postingListLength;


    /**
     * Default constructor initializing components and loading necessary data into memory.
     */
    public QueryProcessor(){
        this.managerIO = new ManagerIO();
        this.lexicon = new Lexicon();
        this.docIndex = new DocIndex();

        // Open necessary files
        managerIO.openLookupFiles();
        managerIO.openObtainFiles();

        // Load data into memory
        obtainLexicon(lexicon, managerIO);
        obtainCollectionStatistics();
        obtainDocumentIndex();

        // Close obtain files
        managerIO.closeObtainFiles();

        postingListLength = 500; //TODO: da controllare
    }

    /**
     * Given a query, retrieves the posting lists for each term in the query.
     * @param queryTerms The terms in the query.
     * @return A HashMap between each term and its respective posting list.
     */
    public HashMap<String, ArrayList<Posting>> lookup(String[] queryTerms){
        int offsetDocId;
        int offsetFreq;
        int postingListLength;
        int docId;
        int freq;

        HashMap<String, ArrayList<Posting>> postingLists = new HashMap<>();
        Set<String> queryTermsSet = new HashSet<>(List.of(queryTerms));

        // Iterate through query terms
        for(String term : queryTermsSet){
            try {
                //Gets the offset information from the lexicon to read in the docIds and freq files.
                offsetDocId = lexicon.getLexicon().get(term).getOffsetDocId();
                offsetFreq = lexicon.getLexicon().get(term).getOffsetFreq();
                postingListLength = lexicon.getLexicon().get(term).getPostingListLength();

                // Go to the specified offsets in docIds and freq files
                managerIO.goToOffset((RandomByteReader) managerIO.getDocIdsReader(), offsetDocId);
                managerIO.goToOffset((RandomByteReader) managerIO.getFreqReader(), offsetFreq);

                // Iterate through the posting list and add postings to the list, reading docId and frequency from the relative files
                for (int i = 0; i < postingListLength; i++) {
                    docId = managerIO.readFromFile(managerIO.getDocIdsReader());
                    freq = managerIO.readFromFile(managerIO.getFreqReader());
                    addPosting(postingLists, term, docId, freq);
                }
            }catch (NullPointerException e){
                // Handle null pointer exception (term not found in lexicon)
                postingLists.put(term, new ArrayList<>());
                lexicon.addInformation(term, 0, 0, 0, 0, 0, 0);
            }
        }
        return postingLists;
    }

    /**
     * Lookup function that, given a query, retrieves the first block of each term's posting list.
     * @param queryTerms The terms in the query.
     * @return A HashMap between each term and the first block of its posting list.
     */
    public HashMap<String, ArrayList<Posting>> initialLookUp(String[] queryTerms){
        int offsetDocId;
        int offsetFreq;
        int postingListLength;
        int docId;
        int freq;
        int postingToRead;

        HashMap<String, ArrayList<Posting>> postingLists = new HashMap<>();
        Set<String> queryTermsSet = new HashSet<>(List.of(queryTerms));

        // Iterate through query terms
        for(String term : queryTermsSet){
            try {
                //Gets the offset information from the lexicon to read in the docIds and freq files.
                offsetDocId = lexicon.getLexicon().get(term).getOffsetDocId();
                offsetFreq = lexicon.getLexicon().get(term).getOffsetFreq();
                postingListLength = lexicon.getLexicon().get(term).getPostingListLength();

                // Go to the specified offsets in docIds and freq files
                managerIO.goToOffset((RandomByteReader) managerIO.getDocIdsReader(), offsetDocId);
                managerIO.goToOffset((RandomByteReader) managerIO.getFreqReader(), offsetFreq);

                // Determine the number of postings to read
                postingToRead = Math.min(postingListLength, postingListBlockLength);

                // Iterate through the first block of the posting list and add postings to the list, reading docId and frequency from the relative files
                for (int i = 0; i < postingToRead; i++) {
                    docId = managerIO.readFromFile(managerIO.getDocIdsReader());
                    freq = managerIO.readFromFile(managerIO.getFreqReader());
                    addPosting(postingLists, term, docId, freq);
                }
            }catch (NullPointerException e){
                // Handle null pointer exception (term not found in lexicon)
                postingLists.put(term, new ArrayList<>());
                lexicon.addInformation(term, 0, 0, 0, 0, 0, 0);
            }
        }
        return postingLists;
    }

    /**
    //     * Given a term and a docId, retrieves the posting list block containing that docId.
    //     * @param term The term for which to retrieve the posting list block.
    //     * @param docId The docId for which to find the posting list block.
    //     * @return A HashMap between the term and the posting list block containing the specified docId.
    //     */
    public HashMap<String, ArrayList<Posting>> lookupDocId(String term, int docId){
        HashMap<String, ArrayList<Posting>> postingLists = new HashMap<>();
        int[] skipPointers = new int[4];
        int newDocId;
        int newFreq;
        int postingToRead;
        int postingListLength;
        int numberOfBlocks;

        searchBlock(skipPointers, term, docId); // Find the block to read using skip pointers

        // If no posting with a docId greater or equal to the provided one exists, return an empty HashMap
        if(skipPointers[3] == 0) return postingLists;

        // Go to the specified offsets in docIds and freq files
        managerIO.goToOffset((RandomByteReader) managerIO.getDocIdsReader(), skipPointers[0]);
        managerIO.goToOffset((RandomByteReader) managerIO.getFreqReader(), skipPointers[1]);

        // Determine the number of posting to read
        postingListLength = lexicon.getLexicon().get(term).getPostingListLength();
        //skiPointers[2] == 0 if the docId is not contained in the last block of the posting list, 1 otherwise.
        if(skipPointers[2] == 0){
            postingToRead = Math.min(postingListLength, postingListLength);
        }
        else {
            // Compute the right length to read if the posting containing the docId is the last block
            numberOfBlocks = (lexicon.getLexicon().get(term).getPostingListLength() / postingListLength) + 1;
            postingToRead = postingListLength - (numberOfBlocks - 1) * postingListLength;
        }

        // Iterate through the posting list block and add postings to the list, reading docId and frequency from the relative files
        for(int i = 0; i<postingToRead; i++){
            newDocId = managerIO.readFromFile(managerIO.getDocIdsReader());
            newFreq = managerIO.readFromFile(managerIO.getFreqReader());
            addPosting(postingLists, term, newDocId, newFreq);
        }
        return postingLists;
    }

    /**
     * Searches for the skip pointers to the relative block of a term's posting list containing a specified docId.
     * @param skipPointers An array to store skip pointers.
     * @param term The term for which to find skip pointers.
     * @param docId The docId used to locate the block in the posting list.
     */
    public void searchBlock(int[] skipPointers, String term, int docId){
        ArrayList<Integer> pointersDocIds = new ArrayList<>();
        ArrayList<Integer> pointerFreq = new ArrayList<>();
        ArrayList<Integer> docIds = new ArrayList<>();
        int offsetLastDocIds;
        int offsetSkipPointers;

        //Gets the number of blocks of the term's posting list
        int blockNumber = (lexicon.getLexicon().get(term).getPostingListLength() / postingListLength) + 1;
        offsetLastDocIds = lexicon.getLexicon().get(term).getOffsetLastDocIds();
        offsetSkipPointers = lexicon.getLexicon().get(term).getOffsetSkipBlock();

        // Go to the specified offsets in last docIds and skip pointers files
        managerIO.goToOffset((RandomByteReader) managerIO.getLastDocIdsReader(), offsetLastDocIds);
        managerIO.goToOffset((RandomByteReader) managerIO.getSkipPointersReader(), offsetSkipPointers);

        // Iterate through the blocks, reading and adding posting list block information to the arrays
        for(int i = 0; i<blockNumber; i++){
            docIds.add(managerIO.readFromFile(managerIO.getLastDocIdsReader()));
            pointersDocIds.add(managerIO.readFromFile(managerIO.getSkipPointersReader()));
            pointerFreq.add(managerIO.readFromFile(managerIO.getSkipPointersReader()));
        }

        // Search for the block containing the posting with the specified docId grater or equal to the one passed as argument.
        for(int i = 0; i<docIds.size(); i++){
            //if a posting with a docId greater than the docId passed is not present in the posting list, skipPointers[3] is put equal 0.
            if(i == 0){
                if(docId < docIds.get(i)){
                    skipPointers[0] = pointersDocIds.get(i);
                    skipPointers[1] = pointerFreq.get(i);
                    skipPointers[2] = 0;
                    skipPointers[3] = 1;
                    return;
                }
            }
            else{
                if(docId <= docIds.get(i) && docId > docIds.get(i-1)){
                    skipPointers[0] = pointersDocIds.get(i);
                    skipPointers[1] = pointerFreq.get(i);
                    skipPointers[2] = 0;

                    // Check if this is the last block of the posting list
                    if(i == (docIds.size() - 1)){
                        skipPointers[2] = 1;
                    }
                    skipPointers[3] = 1;
                    return;
                }
                else{
                    //no posting with a docId greater or equal to the one passed is found.
                    skipPointers[3] = 0;
                }
            }
        }
    }

    /**
     * Adds a posting to the posting list of a term.
     * @param postingLists The HashMap containing posting lists for various terms.
     * @param term The term for which to add the posting.
     * @param docId The document ID of the posting.
     * @param freq The frequency of the term in the document.
     */
    public void addPosting(HashMap<String, ArrayList<Posting>> postingLists, String term, int docId, int freq){
        // Check if the term is already present in the postingLists, if not, add a new entry.
        if (!postingLists.containsKey(term)){
            postingLists.put(term, new ArrayList<>());
        }
        // Add a new Posting object to the posting list of the specified term.
        postingLists.get(term).add(new Posting(docId, freq));
    }

    /**
     * Loads the lexicon from disk into main memory.
     * @param lexicon The Lexicon object to store the loaded data.
     * @param managerIO The ManagerIO object managing I/O operations.
     */
    public static void obtainLexicon(Lexicon lexicon, ManagerIO managerIO) {
        String line;
        String[] terms;

        // Iterate through the lines of the lexicon file and add information to the lexicon.
        while (managerIO.hasNextLine((TextReader) managerIO.getLexiconReader())) {
            line = managerIO.readLineFromFile((TextReader) managerIO.getLexiconReader());
            terms = line.split(" ");

            // Add lexicon information using parsed values from the line.
            lexicon.addInformation(terms[0], Integer.parseInt(terms[1]), Integer.parseInt(terms[2]),
                    Integer.parseInt(terms[3]), Integer.parseInt(terms[4]), Integer.parseInt(terms[5]), Float.parseFloat(terms[6]));
        }
    }

    /**
     * Loads the document index from disk into main memory.
     */
    public void obtainDocumentIndex(){
        int docId;
        int docNo;
        int size;

        // Iterate through the document statistics and add new document information to the document index.
        for(int i = 0; i<statistics.getNDocs(); i++){
            docId = managerIO.readFromFile(managerIO.getDocumentIndexReader());
            docNo = managerIO.readFromFile(managerIO.getDocumentIndexReader());
            size = managerIO.readFromFile(managerIO.getDocumentIndexReader());
            docIndex.addDocument(docId, docNo, size);
        }
    }

    /**
     * Loads the collection statistics from disk into main memory.
     */
    public void obtainCollectionStatistics(){
        String[] terms;

        // Read a line from the collection statistics file and initialize the Statistics object.
        terms = managerIO.readLineFromFile((TextReader) managerIO.getCollectionStatisticsReader()).split(" ");
        statistics = new Statistics(Integer.parseInt(terms[0]), Double.parseDouble(terms[1]),
                lexicon.getLexicon().size(), Integer.parseInt(terms[2]));
    }

    public Statistics getStatistics() {
        return statistics;
    }

    public DocIndex getDocIndex() {
        return docIndex;
    }

    public ManagerIO getManagerIO() {
        return managerIO;
    }

    public Lexicon getLexicon() {
        return lexicon;
    }

    public void setLexicon(Lexicon lexicon) {
        this.lexicon = lexicon;
    }

    /**
     * Loads the next block of a posting list for a specific term and document ID.
     * @param term The term for which to load the next block.
     * @param docId The document ID for which to load the next block.
     * @return A HashMap containing posting lists for the specified term and document ID.
     */
    public HashMap<String, ArrayList<Posting>> loadNextBlock(String term, int docId){
        HashMap<String, ArrayList<Posting>> postingLists = new HashMap<>();
        int[] skipPointers = new int[3];
        int postingListLength;
        int postingToRead;
        int numberOfBlocks;
        int newFreq;
        int newDocId;

        // Search for the skip pointers for the next posting list block.
        searchNextBlock(skipPointers, term, docId);

        if (skipPointers[0] == 0) return postingLists;  // If skip pointers are not found, return an empty postingLists.

        // Go to the specified offsets for docIds and frequencies.
        managerIO.goToOffset((RandomByteReader) managerIO.getDocIdsReader(), skipPointers[0]);
        managerIO.goToOffset((RandomByteReader) managerIO.getFreqReader(), skipPointers[1]);
        postingListLength = lexicon.getLexicon().get(term).getPostingListLength();

        // Calculate the number of postings to read in the next block.
        //skiPointers[2] == 0 if the docId is not contained in the last block of the posting list, 1 otherwise.
        if(skipPointers[2] == 0){
            postingToRead = Math.min(postingListLength, postingListLength);
        }
        else {
            //if the posting containing the docId is the last block of the posting list it computes the right
            //length to read.
            numberOfBlocks = (lexicon.getLexicon().get(term).getPostingListLength() / postingListLength) + 1;
            postingToRead = postingListLength - (numberOfBlocks - 1) * postingListLength;
        }

        // Read postings from the files and add them to the postingLists, reading docId and frequency from the relative files
        for(int i = 0; i<postingToRead; i++){
            newDocId = managerIO.readFromFile(managerIO.getDocIdsReader());
            newFreq = managerIO.readFromFile(managerIO.getFreqReader());
            addPosting(postingLists, term, newDocId, newFreq);
        }
        return postingLists;
    }

    /**
     * Searches for the skip pointers for the next posting list block.
     * @param skipPointers An array to store skip pointers.
     * @param term The term for which to search the skip pointers.
     * @param docId The document ID for which to search the skip pointers.
     */
    public void searchNextBlock(int[] skipPointers, String term, int docId){
        ArrayList<Integer> pointersDocIds = new ArrayList<>();
        ArrayList<Integer> pointerFreq = new ArrayList<>();
        ArrayList<Integer> docIds = new ArrayList<>();
        int offsetLastDocIds;
        int offsetSkipPointers;

        // Calculate the number of blocks for the term's posting list.
        int blockNumber = (lexicon.getLexicon().get(term).getPostingListLength() / postingListLength) + 1;

        offsetLastDocIds = lexicon.getLexicon().get(term).getOffsetLastDocIds();
        offsetSkipPointers = lexicon.getLexicon().get(term).getOffsetSkipPointers();

        // Go to the specified offsets for last docIds and skip pointers.
        managerIO.goToOffset((RandomByteReader) managerIO.getLastDocIdsReader(), offsetLastDocIds);
        managerIO.goToOffset((RandomByteReader) managerIO.getSkipPointersReader(), offsetSkipPointers);

        // Read information about each block.
        for(int i = 0; i<blockNumber; i++){
            docIds.add(managerIO.readFromFile(managerIO.getLastDocIdsReader()));
            pointersDocIds.add(managerIO.readFromFile(managerIO.getSkipPointersReader()));
            pointerFreq.add(managerIO.readFromFile(managerIO.getSkipPointersReader()));
        }

        // Iterate through docIds to find the block containing the specified docId.
        for(int i = 0; i<docIds.size(); i++){
            if (docId == docIds.get(i)){

                // If the docId belongs to the last block, return without updating skipPointers.
                if (i == (docIds.size() - 1)) return;

                // If the next block is the last block, set the flag to indicate the last block.
                if (i == (docIds.size() - 2)) skipPointers[2] = 1;

                // Update skipPointers with the offsets for the next block.
                skipPointers[0] = pointersDocIds.get(i+1);
                skipPointers[1] = pointerFreq.get(i+1);
            }
        }
    }
}
