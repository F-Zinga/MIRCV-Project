package unipi.mircv;

import java.util.*;

import static unipi.mircv.Parameters.*;

/**
 * The QueryProcessor class manages the processing of queries, coordinating with various components,
 * including the lexicon, document index, and collection statistics.
 */
public class QueryProcessor {
    public Lexicon lexicon;
    public Statistics statistics;
    public DocIndex docIndex;
    public int BlockLenght;

    public RandomByteReader docIdByteRead;
    public RandomByteReader freqByteRead;
    public RandomByteReader lastDocIdByteRead;
    public RandomByteReader skipPointersByteRead;
    public TextReader docIdsTextRead;
    public TextReader freqTextRead;
    public TextReader lastDocIdTextRead;
    public TextReader skipPointersTextRead;
    public TextReader lexiconRead;
    public TextReader documentIndexTextRead;
    public ByteReader documentIndexByteRead;
    public TextReader statisticsRead;
    public String encodingType;

    /**
     * Default constructor initializing components and loading necessary data into memory.
     */
    public QueryProcessor(String encodingType) {

        this.lexicon = new Lexicon();
        this.docIndex = new DocIndex();
        this.encodingType= encodingType;

        if (encodingType.equals("text")){
            // Open necessary files
            openTextLookupFiles();
            openTextObtainFiles();
        }
        else {
            openByteLookupFiles();
            openByteObtainFiles();
        }

        // Load data into memory
        obtainLexicon(lexicon);
        obtainStatistics();
        obtainDocumentIndex(encodingType);

        // Close necessary files
        if (encodingType.equals("text"))
            closeTextObtainFiles();
        else
            closeByteObtainFiles();

        BlockLenght = 500;
    }


    /**
     * Retrieves the posting lists for each term in the query.
     * @param queryTerms The terms in the query.
     * @param encodingType type of encoding (byte or text)
     * @return A HashMap between each term and its respective posting list.
     */
    public HashMap<String, ArrayList<Posting>> lookup(String[] queryTerms, String encodingType) {
        int offsetDocId;
        int offsetFreq;
        int postingListLength;
        int docId;
        int freq;


        HashMap<String, ArrayList<Posting>> postingLists = new HashMap<>();
        Set<String> queryTermsSet = new HashSet<>(List.of(queryTerms));

        // Iterate through query terms
        for (String term : queryTermsSet) {
            try {
                // Retrieve offset information from the lexicon to read docIds and freq files.
                offsetDocId = lexicon.getLexicon().get(term).getOffsetDocId();
                offsetFreq = lexicon.getLexicon().get(term).getOffsetFreq();
                postingListLength = lexicon.getLexicon().get(term).getPostingListLength();

                // Navigate to specified offsets in docIds and freq files
                goToOffset(docIdByteRead, offsetDocId);
                goToOffset(freqByteRead, offsetFreq);

                if (encodingType.equals("text")) {
                    // Iterate through the posting list and add postings to the list, reading docId and frequency from the relative files
                    for (int i = 0; i < postingListLength; i++) {
                        docId = docIdsTextRead.read();
                        freq = freqTextRead.read();
                        addPosting(postingLists, term, docId, freq);

                    }
                }
                else{
                    for (int i = 0; i < postingListLength; i++) {
                        docId = docIdByteRead.read();
                        freq = freqByteRead.read();
                        addPosting(postingLists, term, docId, freq);
                    }

                }
            }
                catch(NullPointerException e){
                    // Handle null pointer exception (term not found in lexicon)
                    postingLists.put(term, new ArrayList<>());
                    lexicon.addInformation(term, 0, 0, 0, 0, 0, 0);
                }
            }
            return postingLists;
        }


        /**
         * Retrieves the initial block of posting lists for each term in a given query.
         *
         * @param queryTerms The terms present in the query.
         * @param encodingType type of the encoding (byte or text)
         * @return A HashMap associating each term with its first posting block.
         */
        public HashMap<String, ArrayList<Posting>> initialLookUp (String[]queryTerms, String encodingType){

            int offsetDocId;
            int offsetFreq;
            int postingListLength;
            int docId;
            int freq;
            int postingToRead;

            HashMap<String, ArrayList<Posting>> postingLists = new HashMap<>();
            Set<String> queryTermsSet = new HashSet<>(List.of(queryTerms));

            // Iterate through query terms
            for (String term : queryTermsSet) {
                try {
                    //Obtain the offset information from the lexicon to read in the docIds and freq files.
                    offsetDocId = lexicon.getLexicon().get(term).getOffsetDocId();
                    offsetFreq = lexicon.getLexicon().get(term).getOffsetFreq();
                    postingListLength = lexicon.getLexicon().get(term).getPostingListLength();

                    // Navigate to the specified offsets in docIds and freq files
                    goToOffset(docIdByteRead, offsetDocId);
                    goToOffset(freqByteRead, offsetFreq);

                    // Compute the number of postings to read
                    postingToRead = Math.min(postingListLength, BlockLenght);

                if (encodingType.equals("text")) {
                    // Iterate through the first block of the posting list and add postings to the list, reading docId and frequency from the relative files
                    for (int i = 0; i < postingToRead; i++) {
                        docId = docIdsTextRead.read();
                        freq = freqTextRead.read();
                        addPosting(postingLists, term, docId, freq);
                    }
                }
                else {
                    for (int i = 0; i < postingToRead; i++) {
                        docId = docIdByteRead.read();
                        freq = freqByteRead.read();
                        addPosting(postingLists, term, docId, freq);

                    }
                }
                } catch (NullPointerException e) {
                    // Handle null pointer exception (term not found in lexicon)
                    postingLists.put(term, new ArrayList<>());
                    lexicon.addInformation(term, 0, 0, 0, 0, 0, 0);
                }
            }
            return postingLists;
        }

        /**
         * Retrieves the posting list block containing a specified docId for a given term.
         *
         * @param term The term for which to obtain the posting list block.
         * @param docId The docId used to identify the posting list block.
         * @param encodingType The encoding type for reading files (text or byte).
         * @return A HashMap associating the term with the posting list block containing the specified docId.
         */
        public HashMap<String, ArrayList<Posting>> lookupDocId (String term,int docId, String encodingType){
            HashMap<String, ArrayList<Posting>> postingLists = new HashMap<>();
            int[] skipPointers = new int[4];
            int newDocId;
            int newFreq;
            int postingToRead;
            int postingListLength;
            int numberOfBlocks;

            searchBlock(skipPointers, term, docId, encodingType); // Find the block to read using skip pointers

            // If no posting with a docId greater or equal to the one exists, return an empty HashMap
            if (skipPointers[3] == 0) return postingLists;

            // Navigate to the specified offsets in docIds and freq files
            goToOffset(docIdByteRead, skipPointers[0]);
            goToOffset(freqByteRead, skipPointers[1]);

            // Compute the number of posting to read
            postingListLength = lexicon.getLexicon().get(term).getPostingListLength();

            //skiPointers[2] == 0 if the docId is not contained in the last block of the posting list, 1 otherwise.
            if (skipPointers[2] == 0) {
                postingToRead = Math.min(postingListLength, BlockLenght);
            } else {
                // Compute the right length to read if the posting containing the docId is the last block
                numberOfBlocks = (lexicon.getLexicon().get(term).getPostingListLength() / BlockLenght) + 1;
                postingToRead = postingListLength - (numberOfBlocks - 1) * BlockLenght;
            }

            if (encodingType.equals("text")) {
                // Iterate through the posting list block and add postings to the list, reading docId and frequency from the relative files
                for (int i = 0; i < postingToRead; i++) {
                    newDocId = docIdsTextRead.read();
                    newFreq = freqTextRead.read();
                    addPosting(postingLists, term, newDocId, newFreq);
                }
            }
            else{
                for (int i = 0; i < postingToRead; i++) {
                    newDocId = docIdByteRead.read();
                    newFreq = freqByteRead.read();
                    addPosting(postingLists, term, newDocId, newFreq);
                }
            }
            return postingLists;
        }

    /**
     * Determines skip pointers for locating the block in a term's posting list that contains a specified docId.
     *
     * @param skipPointers An array to store skip pointers.
     * @param term The term for which to find skip pointers.
     * @param docId The docId used to identify the target block in the posting list.
     * @param encodingType The encoding type for reading files (text or byte).
     */
        public void searchBlock ( int[] skipPointers, String term,int docId, String encodingType){
            ArrayList<Integer> pointersDocIds = new ArrayList<>();
            ArrayList<Integer> pointerFreq = new ArrayList<>();
            ArrayList<Integer> docIds = new ArrayList<>();
            int offsetLastDocIds;
            int offsetSkipPointers;

            //Obtain the number of blocks of the term's posting list
            int blockNumber = (lexicon.getLexicon().get(term).getPostingListLength() / BlockLenght) + 1;
            offsetLastDocIds = lexicon.getLexicon().get(term).getOffsetLastDocIds();
            offsetSkipPointers = lexicon.getLexicon().get(term).getOffsetSkipPointers();

            // Navigate to the specified offsets in last docIds and skip pointers files
            goToOffset(lastDocIdByteRead, offsetLastDocIds);
            goToOffset(skipPointersByteRead, offsetSkipPointers);

            if (encodingType.equals("text")) {
            // Iterate through the blocks, reading and adding posting list block information to the arrays
                for (int i = 0; i < blockNumber; i++) {
                    docIds.add(lastDocIdTextRead.read());
                    pointersDocIds.add(skipPointersTextRead.read());
                    pointerFreq.add(skipPointersTextRead.read());
                }
            }
            else {
                for (int i = 0; i < blockNumber; i++) {
                    docIds.add(lastDocIdByteRead.read());
                    pointersDocIds.add(skipPointersByteRead.read());
                    pointerFreq.add(skipPointersByteRead.read());
                }
            }

            // Search for the block containing the posting with the specified docId grater or equal to the argument.
            for (int i = 0; i < docIds.size(); i++) {
                //if a posting with a docId greater than the docId passed is not present in the posting list, skipPointers[3] is 0.
                if (i == 0) {
                    if (docId < docIds.get(i)) {
                        skipPointers[0] = pointersDocIds.get(i);
                        skipPointers[1] = pointerFreq.get(i);
                        skipPointers[2] = 0;
                        skipPointers[3] = 1;
                        return;
                    }
                } else {
                    if (docId <= docIds.get(i) && docId > docIds.get(i - 1)) {
                        skipPointers[0] = pointersDocIds.get(i);
                        skipPointers[1] = pointerFreq.get(i);
                        skipPointers[2] = 0;

                        // Check if we reach the last block of the posting list
                        if (i == (docIds.size() - 1)) {
                            skipPointers[2] = 1;
                        }
                        skipPointers[3] = 1;
                        return;
                    } else {
                        //no posting with a docId greater or equal to the argument is found.
                        skipPointers[3] = 0;
                    }
                }
            }
        }

        /**
         * Appends a posting to the existing posting list for a specific term.
         *
         * @param postingLists The HashMap storing posting lists for different terms.
         * @param term The term for which the posting is appended.
         * @param docId The document ID associated with the posting.
         * @param freq The frequency of the term in the document.
         */
        public void addPosting (HashMap < String, ArrayList < Posting >> postingLists, String term,int docId, int freq){
            // Check if the term already exist in the postingLists, otherwise, add a new entry.
            if (!postingLists.containsKey(term)) {
                postingLists.put(term, new ArrayList<>());
            }
            // Add a new Posting object to the posting list of the specified term.
            postingLists.get(term).add(new Posting(docId, freq));
        }

        /**
         * Reads and populates the lexicon data from the disk into the main memory.
         *
         * @param lexicon The Lexicon object to store the loaded data.
         */
        public void obtainLexicon (Lexicon lexicon){
            String line;
            String[] terms;

            // Process each line of the lexicon file and update the lexicon with parsed information.
            while (lexiconRead.hasNextLine()) {
                line = lexiconRead.readLine();
                terms = line.split(" ");

                // Populate lexicon information using values parsed from the line.
                lexicon.addInformation(terms[0], Integer.parseInt(terms[1]), Integer.parseInt(terms[2]),
                        Integer.parseInt(terms[3]), Integer.parseInt(terms[4]), Integer.parseInt(terms[5]), Float.parseFloat(terms[6]));
            }
        }

        /**
         * Reads and loads the document index from the disk into the main memory.
         *
         * @param encodingType The encoding type for reading files (text or byte).
         */
        public void obtainDocumentIndex ( String encodingType) {
            int docId;
            int docNo;
            int size;

            if (encodingType.equals("text")) {
                // Process document statistics and update the document index with new document information.
                for (int i = 0; i < statistics.getNDocs(); i++) {
                    docId = documentIndexTextRead.read();
                    docNo = documentIndexTextRead.read();
                    size = documentIndexTextRead.read();
                    docIndex.addDocument(docId, docNo, size);
                }
            }
            else {
                for (int i = 0; i < statistics.getNDocs(); i++) {
                    docId = documentIndexByteRead.read();
                    docNo = documentIndexByteRead.read();
                    size = documentIndexByteRead.read();
                    docIndex.addDocument(docId, docNo, size);
                }
            }
        }

        /**
         * Reads and transfers the collection statistics from the disk into the main memory.
         */
        public void obtainStatistics () {
            String[] terms;

            // Read a line from the collection statistics file and initialize the Statistics object.
            terms = statisticsRead.readLine().split(" ");
            statistics = new Statistics(Integer.parseInt(terms[0]), Double.parseDouble(terms[1]),
                    lexicon.getLexicon().size(), Integer.parseInt(terms[2]));
        }

        public Statistics getStatistics () {
            return statistics;
        }

        public DocIndex getDocIndex () {
            return docIndex;
        }

        public Lexicon getLexicon () {
            return lexicon;
        }


        /**
         * Load the subsequent block of a posting list associated with a specific term and document ID.
         *
         * @param term The term for which the next block is to be loaded.
         * @param docId The document ID for which the next block is to be loaded.
         * @param encodingType type of encoding (byte or text)
         * @return A HashMap containing posting lists for the specified term and document ID.
         */
        public HashMap<String, ArrayList<Posting>> loadNextBlock (String term,int docId, String encodingType){
            HashMap<String, ArrayList<Posting>> postingLists = new HashMap<>();
            int[] skipPointers = new int[3];
            int postingListLength;
            int postingToRead;
            int numberOfBlocks;
            int newFreq;
            int newDocId;

            // Identify the skip pointers for the subsequent posting list block.
            searchNextBlock(skipPointers, term, docId, encodingType);

            if (skipPointers[0] == 0)
                return postingLists;  //Return an empty postingLists if skip pointers are not located.

            // Navigate to the specified offsets for docIds and frequencies.
            goToOffset(docIdByteRead, skipPointers[0]);
            goToOffset(freqByteRead, skipPointers[1]);

            postingListLength = lexicon.getLexicon().get(term).getPostingListLength();

            // Compute the number of postings to read in the next block.
            //skiPointers[2] == 0 if the docId is not contained in the last block of the posting list, 1 otherwise.
            if (skipPointers[2] == 0) {
                postingToRead = Math.min(postingListLength, BlockLenght);
            } else {
                //if the posting containing the docId is the last block of the posting list, computes the right
                //length to read.
                numberOfBlocks = (lexicon.getLexicon().get(term).getPostingListLength() / BlockLenght) + 1;
                postingToRead = postingListLength - (numberOfBlocks - 1) * BlockLenght;
            }

            if (encodingType.equals("text")) {
                // Read postings from the files and add them to the postingLists, reading docId and frequency from the relative files
                for (int i = 0; i < postingToRead; i++) {
                    newDocId = docIdsTextRead.read();
                    newFreq = freqTextRead.read();
                    addPosting(postingLists, term, newDocId, newFreq);
                }
            }
            else {
                for (int i = 0; i < postingToRead; i++) {
                    newDocId = docIdByteRead.read();
                    newFreq = freqByteRead.read();
                    //TODO
                    addPosting(postingLists, term, newDocId, newFreq);

                }
            }
            return postingLists;
        }

        /**
         * Search the skip pointers for the subsequent posting list block.
         *
         * @param skipPointers An array to store skip pointers.
         * @param term The term for which to search the skip pointers.
         * @param docId The document ID for which to search the skip pointers.
         * @param encodingType type of encoding (byte or text)
         */
        public void searchNextBlock ( int[] skipPointers, String term,int docId, String encodingType){
            ArrayList<Integer> pointersDocIds = new ArrayList<>();
            ArrayList<Integer> pointerFreq = new ArrayList<>();
            ArrayList<Integer> docIds = new ArrayList<>();
            int offsetLastDocIds;
            int offsetSkipPointers;

            // Determine the number of blocks for the term's posting list.
            int blockNumber = (lexicon.getLexicon().get(term).getPostingListLength() / BlockLenght) + 1;

            offsetLastDocIds = lexicon.getLexicon().get(term).getOffsetLastDocIds();
            offsetSkipPointers = lexicon.getLexicon().get(term).getOffsetSkipPointers();

            // Navigate to the specified offsets for last docIds and skip pointers.
            goToOffset(lastDocIdByteRead, offsetLastDocIds);
            goToOffset(skipPointersByteRead, offsetSkipPointers);

            if (encodingType.equals("text")) {
                // Read information about each block.
                for (int i = 0; i < blockNumber; i++) {
                    docIds.add(lastDocIdTextRead.read());
                    pointersDocIds.add(skipPointersTextRead.read());
                    pointerFreq.add(skipPointersTextRead.read());
                }
            }
            else {
                for (int i = 0; i < blockNumber; i++) {
                    docIds.add(lastDocIdByteRead.read());
                    pointersDocIds.add(skipPointersByteRead.read());
                    pointerFreq.add(skipPointersByteRead.read());
                }
            }

            // Iterate through docIds to find the block containing the specified docId.
            for (int i = 0; i < docIds.size(); i++) {
                if (docId == docIds.get(i)) {

                    // return without updating skipPointers, if the docId belongs to the last block, .
                    if (i == (docIds.size() - 1)) return;

                    //  set the flag to indicate the last block if the next block is the last block,.
                    if (i == (docIds.size() - 2)) skipPointers[2] = 1;
                    // Update skipPointers with the offsets for the next block.
                    skipPointers[0] = pointersDocIds.get(i + 1);
                    skipPointers[1] = pointerFreq.get(i + 1);
                }
            }
        }

        /**
         * Moves the file cursor to a specified offset for random access reads.
         *
         * @param file The RandomByteReader representing the file to navigate.
         * @param offset The offset to move the cursor to.
         */
        public void goToOffset (RandomByteReader file,int offset){
            file.goToOffset(offset);
        }


    // Open text files for the lookup phase.
    public void openTextLookupFiles() {
        docIdsTextRead = new TextReader(DOCID_PATH + ".txt");
        freqTextRead = new TextReader(FREQ_PATH + ".txt");
        lastDocIdTextRead = new TextReader(LASTDOCID_PATH + ".txt");
        skipPointersTextRead = new TextReader(SKIPPOINTERS_PATH + ".txt");
    }

    // Open byte files for the lookup phase.
    public void openByteLookupFiles() {
        Compressor compressor = new Compressor();
        docIdByteRead = new RandomByteReader(DOCID_PATH + ".dat", compressor);
        freqByteRead = new RandomByteReader(FREQ_PATH + ".dat", compressor);
        lastDocIdByteRead = new RandomByteReader(LASTDOCID_PATH + ".dat", compressor);
        skipPointersByteRead = new RandomByteReader(SKIPPOINTERS_PATH + ".dat", compressor);
    }

    // Close text files after lookup phase.
    public void closeTextLookupFiles() {
        docIdsTextRead.close();
        freqTextRead.close();
        lastDocIdTextRead.close();
        skipPointersTextRead.close();

    }

    // Close byte files after lookup phase.
    public void closeByteLookupFiles() {
        docIdByteRead.close();
        freqByteRead.close();
        lastDocIdByteRead.close();
        skipPointersByteRead.close();

    }


    public void openTextObtainFiles() {
        lexiconRead = new TextReader(LEXICON_PATH);
        statisticsRead = new TextReader(STATISTICS_PATH);
        documentIndexTextRead = new TextReader(DOCINDEX_PATH + ".txt");
    }

    public void openByteObtainFiles() {
        Compressor compressor = new Compressor();
        lexiconRead = new TextReader(LEXICON_PATH);
        statisticsRead = new TextReader(STATISTICS_PATH);
        documentIndexByteRead = new ByteReader(DOCINDEX_PATH + ".dat", compressor);
    }

    public void closeTextObtainFiles() {
        lexiconRead.close();
        statisticsRead.close();
        documentIndexTextRead.close();
    }

    public void closeByteObtainFiles() {
        lexiconRead.close();
        statisticsRead.close();
        documentIndexByteRead.close();
    }
}
