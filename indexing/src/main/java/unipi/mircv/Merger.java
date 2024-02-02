package unipi.mircv;

import java.util.HashMap;

import static unipi.mircv.Parameters.*;

/**
 * The Merger class is responsible for merging block files generated during the SPIMI algorithm's indexing phase.
 * It handles both text and byte encodings, performing the merging of lexicon, document IDs, frequencies, document index,
 * last document IDs, and skip pointers.
 */
public class Merger {

    //readers used during the merging phase
    TextReader[] lexiconScanners;
    TextReader[] docIdsTextScanners;
    TextReader[] freqTextScanners;
    TextReader[] documentIndexTextScanners;
    ByteReader[] docIdByteScanners;
    ByteReader[] freqByteScanners;
    ByteReader[] documentIndexByteScanners;

    //writers used to write the blocks during indexing and then for merging.
    public TextWriter lexiconWriter;
    public TextWriter docIdsTextWriter;
    public TextWriter freqTextWriter;
    public TextWriter docIndexTextWriter;
    public TextWriter lastDocIdsTextWriter;
    public TextWriter skipPointersTextWriter;

    public ByteWriter docIdsByteWriter;
    public ByteWriter freqByteWriter;
    public ByteWriter docIndexByteWriter;
    public ByteWriter lastDocIdsByteWriter;
    public ByteWriter skipPointersByteWriter;

    public int BlockLenght = 500;




    /**
     * Merges byte-encoded blocks using the SPIMI algorithm, combining information from lexicon, document IDs,
     * frequencies, document index, last document IDs, and skip pointers.
     *
     * @param blockCounter  Number of blocks to merge.
     * @param encodingType  The encoding type "byte".
     * @param statistics    Statistics object for indexing information.
     */
    public void mergeByteBlocks(int blockCounter, String encodingType, Statistics statistics) {

        int localPostingListLength;
        int postingListLength;
        int offsetDocIds = 0;
        int offsetFreq = 0;
        int offsetLastDocIds = 0;
        int offsetSkipPointers = 0;
        int docId = 0;
        float termUpperBound;
        float maxTermFrequency;
        float localTermFrequency;
        float tf;
        float idf;
        String minTerm;
        int postingBlockCounter;

        // Array to store terms read from the current lexicon pointers
        String[][] terms = new String[blockCounter][];
        // Arrays to manage whether scanners need to read from files or have finished scanning
        boolean[] scannerToRead = new boolean[blockCounter];
        boolean[] scannerFinished = new boolean[blockCounter];


        // Initialization of scanner status arrays
        for (int i = 0; i < blockCounter; i++) {
            scannerToRead[i] = true;
            scannerFinished[i] = false;
        }

        openScanners(blockCounter, encodingType); //open scanners of the block files

        openMergeFiles(encodingType); //open the final merge files

        HashMap<Integer,DocInfo> docIndex = new HashMap<>();

        // Merging of the document index is performed first, reading three integers for each row.
        for (int i = 0; i < blockCounter; i++) {
            int id = documentIndexByteScanners[i].read(); //read the first integer

            while (id != -1) { //continue until the file is not ended
                docIndexByteWriter.write(id); //write the read integer on the final file
                // Read two more integers from the current block file and write them to the final file
                 int docno = documentIndexByteScanners[i].read();
                 docIndexByteWriter.write(docno);
                 int docLen = documentIndexByteScanners[i].read();
                 docIndexByteWriter.write(docLen);
                 DocInfo docinfo = new DocInfo(docno,docLen);
                 docIndex.put(id,docinfo);
                 id = documentIndexByteScanners[i].read();
            }
        }
        // Merging loop for lexicon, document IDs, and frequencies files
        while (true) {
            //Read from the lexicon files that needs to be read.
            advancePointers(lexiconScanners, scannerToRead, terms, scannerFinished,blockCounter);
            //Checks if the merging phase is finished.
            if (!continueMerging(scannerFinished,blockCounter)) {
                break;
            }
            // Get the minimum term among the current pointers
            minTerm = minTerm(terms, scannerFinished,blockCounter);
            postingListLength = 0;
            postingBlockCounter = 0;
            maxTermFrequency = 0;

            // Write term information to the lexicon in text format
            lexiconWriter.write(minTerm + " "
                    + offsetDocIds + " " + offsetFreq + " " + offsetLastDocIds + " " + offsetSkipPointers + " ");

            //for every block if the current pointed term is the minimum term perform merging.
            for (int i = 0; i < blockCounter; i++) {
                if (terms[i][0].equals(minTerm)) {
                    scannerToRead[i] = true;  // Indicate that information is used, so the next time it needs to read new information

                    //Obtain the posting list length of the current block
                    localPostingListLength = Integer.parseInt(terms[i][5]);
                    //Update the global posting list length
                    postingListLength += localPostingListLength;
                    localTermFrequency = Float.parseFloat(terms[i][6]);

                    if (localTermFrequency > maxTermFrequency) maxTermFrequency = localTermFrequency;
                    for (int j = 0; j < localPostingListLength; j++) {
                        // If at the start of the posting list block, save skip pointers for the block
                        if (postingBlockCounter == 0) {
                            // Save two integers in the skipPointers file: the docId offset and the frequency offset
                            offsetSkipPointers += skipPointersByteWriter.write(offsetDocIds);
                            offsetSkipPointers += skipPointersByteWriter.write(offsetFreq);
                        }

                        docId = docIdByteScanners[i].read();
                        // Save information from block files to the final files
                        offsetDocIds += docIdsByteWriter.write(docId);
                        offsetFreq += freqByteWriter.write(freqByteScanners[i].read());

                        postingBlockCounter += 1;
                        //if we are at the end of the posting list block we save the current docId in the lastDocId file.
                        if (postingBlockCounter == BlockLenght) {
                            offsetLastDocIds += lastDocIdsByteWriter.write(docId);
                            postingBlockCounter = 0;
                        }
                    }
                } else {
                    //We not read from the scanner again if the current term of the lexicon pointer is not the min term
                    scannerToRead[i] = false;
                }
            }
            //at the end of the merging for a specific term we save the docId of the last posting of the posting list of that term.
            if (postingBlockCounter != BlockLenght) {
                offsetLastDocIds += lastDocIdsByteWriter.write(docId);
            }

            //At the end of lexicon merging we add the global posting list length and the term upper bound information.
            tf = (float) (1 + Math.log(maxTermFrequency));
            idf = (float) Math.log((double) statistics.getNDocs() / postingListLength);

            //termUpperBound = tf * idf; TFIDS TERM UPPER BOUND

            int doclen = docIndex.get(docId).getDocLen();
            float denominator =(float) (K1 * ((1 - B) + B * ((double) doclen / statistics.getAvdl())) + tf);
            termUpperBound = (tf * idf) / denominator;

            lexiconWriter.write(postingListLength + " "
                    + termUpperBound + "\n");
        }

        // close byte scanners
        for (int i = 0; i < lexiconScanners.length; i++) {
                lexiconScanners[i].close();
                docIdByteScanners[i].close();
                freqByteScanners[i].close();
                documentIndexByteScanners[i].close();
            }

        //close merged files writers
            docIdsByteWriter.close();
            freqByteWriter.close();
            lexiconWriter.close();
            docIndexByteWriter.close();
            lastDocIdsByteWriter.close();
            skipPointersByteWriter.close();
    }


    /**
     * Merges byte-encoded blocks using the SPIMI algorithm, combining information from lexicon, document IDs,
     * frequencies, document index, last document IDs, and skip pointers.
     *
     * @param blockCounter  Number of blocks to merge.
     * @param encodingType  The encoding type "text".
     * @param statistics    Statistics object for indexing information.
     */
    public void mergeTextBlocks(int blockCounter, String encodingType, Statistics statistics) {

        int localPostingListLength;
        int postingListLength;
        int offsetDocIds = 0;
        int offsetFreq = 0;
        int offsetLastDocIds = 0;
        int offsetSkipPointers = 0;
        int docId = 0;
        float termUpperBound;
        float maxTermFrequency;
        float localTermFrequency;
        float tf;
        float idf;
        String minTerm;

        // Array to store terms read from the current lexicon pointers
        String[][] terms = new String[blockCounter][];
        // Arrays to manage whether scanners need to read from files or have finished scanning
        boolean[] scannerToRead = new boolean[blockCounter];
        boolean[] scannerFinished = new boolean[blockCounter];

        // Initialization of scanner status arrays
        int postingBlockCounter;
        for (int i = 0; i < blockCounter; i++) {
            scannerToRead[i] = true;
            scannerFinished[i] = false;
        }

        openScanners(blockCounter, encodingType); //open the scanners of the block files

        openMergeFiles(encodingType); //open the final marge files

        // Merging of the document index is performed first, reading three integers for each row.
        for (int i = 0; i < blockCounter; i++) {
            int number = documentIndexTextScanners[i].read(); //read the first integer
            while (number != -1) { //continue until the file is not ended
                docIndexTextWriter.write(number); //write on the final file the read integer
                for (int j = 0; j < 2; j++) // Read two more integers from the current block file and write them to the final file
                {
                    docIndexTextWriter.write(documentIndexTextScanners[i].read());
                }
                number = documentIndexTextScanners[i].read();
            }
        }
        // Merging loop for lexicon, document IDs, and frequencies files
        while (true) {
            //Read from the lexicon files that needs to be read.
            advancePointers(lexiconScanners, scannerToRead, terms, scannerFinished,blockCounter);
            //Checks if the merging phase is finished.
            if (!continueMerging(scannerFinished,blockCounter)) {
                break;
            }
            // Get the minimum term among the current pointers
            minTerm = minTerm(terms, scannerFinished,blockCounter);
            postingListLength = 0;
            postingBlockCounter = 0;
            maxTermFrequency = 0;
            // Write term information to the lexicon in text format
            lexiconWriter.write(minTerm + " "
                    + offsetDocIds + " " + offsetFreq + " " + offsetLastDocIds + " " + offsetSkipPointers + " ");

            //for every block if the current pointed term is the minimum term perform merging.
            for (int i = 0; i < blockCounter; i++) {
                if (terms[i][0].equals(minTerm)) {
                    scannerToRead[i] = true; // Indicate that information is used, so the next time it needs to read new information

                    //Obtain the posting list length of the current block
                    localPostingListLength = Integer.parseInt(terms[i][5]);
                    //Update the global posting list length
                    postingListLength += localPostingListLength;
                    localTermFrequency = Float.parseFloat(terms[i][6]);

                    if (localTermFrequency > maxTermFrequency) maxTermFrequency = localTermFrequency;
                    for (int j = 0; j < localPostingListLength; j++) {
                        // If at the start of the posting list block, save skip pointers for the block
                        if (postingBlockCounter == 0) {
                            //Saves in the skiPointers file 2 integers: the docId offset and the frequency offset.
                            offsetSkipPointers += skipPointersTextWriter.write(offsetDocIds);
                            offsetSkipPointers += skipPointersTextWriter.write(offsetFreq);
                        }

                        docId = docIdsTextScanners[i].read();
                        //Saves in the final files the information arriving from the block files.
                        offsetDocIds += docIdsTextWriter.write(docId);
                        offsetFreq += freqTextWriter.write(freqTextScanners[i].read());

                        postingBlockCounter += 1;
                        //if we are at the end of the posting list block we save the current docId in the lastDocId file.
                        if (postingBlockCounter == BlockLenght) {
                            offsetLastDocIds += lastDocIdsTextWriter.write(docId);
                            postingBlockCounter = 0;
                        }
                    }
                } else {
                    //We not read from the scanner again if the current term of the lexicon pointer is not the min term
                    scannerToRead[i] = false;
                }
            }
            //at the end of the merging for a specific term we save the docId of the last posting of the posting list of that term.
            if (postingBlockCounter != BlockLenght) {
                offsetLastDocIds += lastDocIdsTextWriter.write(docId);
            }
            //At the end we add the global posting list length and the term upper bound information.
            tf = (float) (1 + Math.log(maxTermFrequency));
            idf = (float) Math.log((double) statistics.getNDocs() / postingListLength);
            termUpperBound = tf * idf;
            lexiconWriter.write(postingListLength + " "
                    + termUpperBound + "\n");
        }

        // close byte scanners
        for (int i = 0; i < lexiconScanners.length; i++) {
            lexiconScanners[i].close();
            docIdsTextScanners[i].close();
            freqTextScanners[i].close();
            documentIndexTextScanners[i].close();
        }

        //close merged files writers
        docIdsTextWriter.close();
        freqTextWriter.close();
        lexiconWriter.close();
        docIndexTextWriter.close();
        lastDocIdsTextWriter.close();
        skipPointersTextWriter.close();
    }


    /**
     * Checks whether the merging phase needs to continue by examining the status of scanners.
     *
     * @param scannerFinished An array indicating if each scanner has finished its block.
     * @param blockCounter    Number of blocks involved in the merge.
     * @return True if merging should continue, false otherwise.
     */
    public boolean continueMerging(boolean[] scannerFinished,int blockCounter) {
        boolean continueMerging;
        continueMerging = false;
        for (int i = 0; i < blockCounter; i++) {
            if (!scannerFinished[i]) {
                continueMerging = true;
                break;
            }
        }
        return continueMerging;
    }

    /**
     * Advances the pointers of lexicon scanners during the merging phase.
     *
     * @param lexiconScanners   Array of lexicon scanners.
     * @param scannerToRead     Array indicating which scanners need to read new information.
     * @param terms             Array to store terms read from scanners.
     * @param scannerFinished   Array indicating if each scanner has finished scanning its block.
     * @param blockCounter      Number of blocks involved in the merge.
     */
    public void advancePointers(TextReader[] lexiconScanners, boolean[] scannerToRead, String[][] terms,
                                boolean[] scannerFinished,int blockCounter) {
        for (int i = 0; i < blockCounter; i++) {
            if (scannerToRead[i]) {
                if (lexiconScanners[i].hasNextLine()) {
                    terms[i] = lexiconScanners[i].readLine().split(" ");
                } else {
                    scannerFinished[i] = true;
                }
            }
        }
    }

    /**
     * Finds the minimum term among the terms currently pointed during the merging phase.
     *
     * @param terms           Array containing terms from various lexicon pointers.
     * @param scannerFinished Array indicating if each scanner has finished scanning its block.
     * @param blockCounter    Number of blocks involved in the merge.
     * @return The minimum term among the current lexicon pointers.
     */
    public String minTerm(String[][] terms, boolean[] scannerFinished,int blockCounter) {
        String minTerm = "}";
        for (int i = 0; i < blockCounter; i++) {
            if (terms[i][0].compareTo(minTerm) < 0 && !scannerFinished[i]) {
                minTerm = terms[i][0];
            }
        }
        return minTerm;
    }

    /**
     * Opens scanners for the merging phase, considering the encoding type.
     *
     * @param blockCounter Number of blocks involved in the merge.
     * @param encodingType Encoding type, either "text" or "byte".
     */
    public void openScanners(int blockCounter, String encodingType) {
        lexiconScanners = new TextReader[blockCounter];
        for (int i = 0; i < blockCounter; i++) {
            lexiconScanners[i] = new TextReader(LEXICON_BLOCK_PATH + i + ".txt");
        }
        if (encodingType.equals("text")) {
            // For text encoding, open additional scanners for document IDs, frequencies, and document index
            docIdsTextScanners = new TextReader[blockCounter];
            freqTextScanners = new TextReader[blockCounter];
            documentIndexTextScanners = new TextReader[blockCounter];
            for (int i = 0; i < blockCounter; i++) {
                docIdsTextScanners[i] = new TextReader(DOCIDS_BLOCK_PATH + i + ".txt");
                freqTextScanners[i] = new TextReader(FREQ_BLOCK_PATH + i + ".txt");
                documentIndexTextScanners[i] = new TextReader(DOCUMENT_INDEX_BLOCK_PATH + i + ".txt");
            }
        } else {
            // For byte encoding, open byte scanners for document IDs, frequencies, and document index
            Compressor compressor = new Compressor();
            docIdByteScanners = new ByteReader[blockCounter];
            freqByteScanners = new ByteReader[blockCounter];
            documentIndexByteScanners = new ByteReader[blockCounter];
            for (int i = 0; i < blockCounter; i++) {
                docIdByteScanners[i] = new ByteReader(DOCIDS_BLOCK_PATH + i + ".dat", compressor);
                freqByteScanners[i] = new ByteReader(FREQ_BLOCK_PATH + i + ".dat", compressor);
                documentIndexByteScanners[i] = new ByteReader(DOCUMENT_INDEX_BLOCK_PATH + i + ".dat", compressor);
            }
        }
    }


    /**
     * Opens final files for the merging phase, considering the encoding type.
     *
     * @param encodingType Encoding type, either "text" or "byte".
     */
    public void openMergeFiles(String encodingType) {
        lexiconWriter = new TextWriter(LEXICON_PATH);
        if (encodingType.equals("text")) {
            // For text encoding, open additional writers for document IDs, frequencies, document index, last doc IDs, and skip pointers
            docIdsTextWriter = new TextWriter(DOCID_PATH + ".txt");
            freqTextWriter = new TextWriter(FREQ_PATH + ".txt");
            docIndexTextWriter = new TextWriter(DOCINDEX_PATH + ".txt");
            lastDocIdsTextWriter= new TextWriter(LASTDOCID_PATH + ".txt");
            skipPointersTextWriter = new TextWriter(SKIPPOINTERS_PATH + ".txt");
        } else {
            // For byte encoding, open byte writers for document IDs, frequencies, document index, last doc IDs, and skip pointers
            Compressor compressor = new Compressor();
            docIdsByteWriter = new ByteWriter(DOCID_PATH + ".dat", compressor);
            freqByteWriter = new ByteWriter(FREQ_PATH + ".dat", compressor);
            docIndexByteWriter = new ByteWriter(DOCINDEX_PATH + ".dat", compressor);
            lastDocIdsByteWriter = new ByteWriter(LASTDOCID_PATH + ".dat", compressor);
            skipPointersByteWriter = new ByteWriter(SKIPPOINTERS_PATH + ".dat", compressor);
        }
    }

}

