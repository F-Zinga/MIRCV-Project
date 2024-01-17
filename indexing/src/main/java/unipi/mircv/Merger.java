package unipi.mircv;
import org.javatuples.Pair;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 * The Merger class facilitates the merging of inverted index and lexicon blocks into a single file.
 */
public class Merger {

    /**
     * This method performs the merging of lexicon blocks and inverted index blocks into a single file.
     *
     * @param compress If true, the inverted index and lexicon blocks will be compressed using VBE; otherwise, they will be written without compression.
     */
    public static void merge(boolean compress) {

        System.out.println(" *** Merging lexicon blocks and inverted index blocks ... ***");

        // Record the start time for performance measurement
        long start = System.nanoTime();

        //Retrieve blocks statistics
        Statistics statistics = readStatistics();

        int num_blocks = statistics.getNBlocks();

        //Initialize arrays of random access files, for docIds, frequencies and lexicon blocks
        RandomAccessFile[] randomAccessFileDocIds = new RandomAccessFile[num_blocks];
        RandomAccessFile[] randomAccessFilesFrequencies = new RandomAccessFile[num_blocks];
        RandomAccessFile[] randomAccessFilesLexicon = new RandomAccessFile[num_blocks];

        //Files for saving the final result
        RandomAccessFile lexiconFile;
        RandomAccessFile docIdsFile;
        RandomAccessFile frequenciesFile;
        RandomAccessFile blocksFile;
        RandomAccessFile documentIndex;


        // Accumulators to hold the current offset for the next list of postings
        long docIdsOffset = 0;
        long frequenciesOffset = 0;
        long blocksOffset = 0;

        //Array to store the current offset reached in each lexicon block
        int[] offsets = new int[num_blocks];

        //Array of boolean, where the i-th entry is true, if the i-th block has reached the end of the lexicon block file
        boolean[] endOfBlock = new boolean[num_blocks];

        //Initialize each offset to 0 and each boolean to false, for each lexicon block
        //because at the beginning no block has reached the end
        for (int i = 0; i < num_blocks; i++) {
            offsets[i] = 0;
            endOfBlock[i] = false;
        }

        //String to keep track of the min term among all the current terms in each lexicon block, it is used to determine the
        // term of which the posting lists must be merged
        String minTerm = null;

        //TermInfo to keep the term's information to be written in the lexicon file
        Term lexiconEntry;

        //Used to store the term's info for each lexicon block file
        Term[] curTerm = new Term[num_blocks];

        // Linked list to contain blocks with the current minimum term
        LinkedList<Integer> blocksWithMinTerm = new LinkedList<>();

        //Array containing the docIds and frequencies of the posting list of the current min term in the current block
        ArrayList<Long> docIds = new ArrayList<>();
        ArrayList<Integer> frequencies = new ArrayList<>();

        //Array containing info about the blocks
        ArrayList<Block> blocks = new ArrayList<>();

        //Arrays containing compressed docIds and frequencies of the posting list of the current min term
        byte[] docIdsCompressed;
        byte[] frequenciesCompressed;


        try {
            //Create a stream for each random access files of each block, the stream isread only
            for (int i = 0; i < num_blocks; i++) {
                //noinspection resource
                randomAccessFileDocIds[i] = new RandomAccessFile(Parameters.DOCID_BLOCK_PATH +(i+1)+".txt", "r");
                //noinspection resource
                randomAccessFilesFrequencies[i] = new RandomAccessFile(Parameters.FREQ_BLOCK_PATH +(i+1)+".txt", "r");
                //noinspection resource
                randomAccessFilesLexicon[i] = new RandomAccessFile(Parameters.LEXICON_BLOCK_PATH+(i+1)+".txt", "r");
            }

            //Create a stream for the lexicon file, the docids file, frequencies file, blocks file, and document index,
            //opened in write-only and read-only modes, respectively
            lexiconFile = new RandomAccessFile(Parameters.LEXICON_PATH, "rw");
            docIdsFile = new RandomAccessFile(Parameters.DOCID_PATH, "rw");
            frequenciesFile = new RandomAccessFile(Parameters.FREQ_PATH, "rw");
            blocksFile = new RandomAccessFile(Parameters.BLOCKS_PATH, "rw");
            documentIndex = new RandomAccessFile(DocIndex.DOCUMENT_INDEX_PATH, "r");


        } catch (FileNotFoundException e) {
            // Handle the exception if the file is not found
            System.err.println(" *** File not found : " + e.getMessage());
            throw new RuntimeException(e);
        }

        //Read the first term of each lexicon block and update offsets
        for (int i = 0; i < curTerm.length; i++) {
            curTerm[i] = readNextTermInfo(randomAccessFilesLexicon[i],offsets[i]);

            // Check if the end of the block is reached
            if(curTerm[i] == null) {
                endOfBlock[i] = true;
            }

            //Update the offset to the offset of the next file to be read
            offsets[i] += 68;//term + offsetDocId + offsetFrequency + postingListLength
        }

        long j = 1;
        //Iterate over all the lexicon blocks, until the end of the lexicon block file is reached for each block
        while(!endOfAllFiles(endOfBlock, num_blocks)) {

            j++;

            // Print processing info every 25000 blocks
            if(j%25000 == 0){
                System.out.println(" *** Processing time: " + (System.nanoTime() - start)/1000000000+ "s. Processed " + j + " terms ***");
            }

            //For each block read the next term
            for(int i = 0; i < num_blocks; i++) {

                // Skip reading from the block if the end of the block is reached
                if(endOfBlock[i]) {
                    continue;
                }

                // Update minTerm if the current term is lexicographically smaller than the min term
                if(minTerm == null || curTerm[i].getTerm().compareTo(minTerm) < 0) {

                    //If we've found another min term, then update the min term.
                    minTerm = curTerm[i].getTerm();

                    //Clear the array of blocks with the min term.
                    blocksWithMinTerm.clear();

                    //Add the current block to the list of blocks with the min term.
                    blocksWithMinTerm.add(i);

                    //If the current term is equal to the min term, then add the current block to the list of blocks with the min term.
                } else if (curTerm[i].getTerm().compareTo(minTerm) == 0) {

                    //Add the current block to the list of blocks with the min term.
                    blocksWithMinTerm.add(i);
                }
            }//Now we have the current min term.

            //Check if we've reached the end of the merge.
            if(endOfAllFiles(endOfBlock, num_blocks)) {
                System.out.println("*** end of files ***");
                break;
            }


            //Merge the posting lists of the current min term in the blocks containing the term
            for (Integer integer : blocksWithMinTerm) {

                //Append the current term docIds to the docIds accumulator
                docIds.addAll(PostingList.readPLDocId(randomAccessFileDocIds[integer], curTerm[integer].getOffsetDocId(), curTerm[integer].getPostingListLength()));


                //Append the current term frequencies to the frequencies accumulator
                frequencies.addAll(PostingList.readPlFreq(randomAccessFilesFrequencies[integer], curTerm[integer].getOffsetFrequency(), curTerm[integer].getPostingListLength()));


                //Read the lexicon entry from the current block and move the pointer to the next term
                curTerm[integer] = readNextTermInfo(randomAccessFilesLexicon[integer], offsets[integer]);

                //Check if the end of the block is reached or a problem during the reading occurred
                if(curTerm[integer] == null) {

                    endOfBlock[integer] = true;
                    continue;
                }

                //Increment the offset of the current block to the starting offset of the next term
                offsets[integer] += Parameters.TERM_BYTES + Parameters.OFFSET_DOCIDS_BYTES +
                                Parameters.OFFSET_FREQUENCIES_BYTES + Parameters.POSTING_LIST_BYTES;

            }

            // Initialize variables for maximum term frequency and maximum tf for bm25
            int maxFreq = 0;

            double tf_maxScoreBm25 = 0;

            if(compress){

                // Initialize a Pair to store the maximum score for bm25
                Pair<Double, Double> maxscore = Pair.with(0.0, 0.0);

                //Compress the list of docIds using VBE and create the list of skip blocks for the list of docids
                docIdsCompressed = Compressor.variableByteEncodeDocId(docIds, blocks);

                //Compress the list of frequencies using VBE and update the frequencies information in the skip blocks
                Pair<byte[],Pair<Double,Double>> tuple = Compressor.variableByteEncodeFreq(frequencies, blocks, docIds, maxscore, documentIndex, statistics);
                frequenciesCompressed = tuple.getValue0();
                maxscore = tuple.getValue1();

                //Write the compressed docIds and frequencies of the current term to the respective files
                try {
                    docIdsFile.write(docIdsCompressed);
                    frequenciesFile.write(frequenciesCompressed);
                } catch (IOException e) {
                    // Handle the exception if the file is not found
                    System.err.println(" *** File not found: " + e.getMessage());
                    throw new RuntimeException(e);
                }

                //Compute idf value
                double idf = Math.log(statistics.getNDocs()/ (double)docIds.size())/Math.log(2);
                // Compute term upper bounds for tfidf and bm25
                int tfidfTermUpperBound = (int) Math.ceil((1 + Math.log(maxscore.getValue0()) / Math.log(2))*idf);
                int bm25TermUpperBound = (int) Math.ceil(maxscore.getValue1()*idf);

                // Create a Term object with the computed values
                lexiconEntry = new Term(
                        minTerm,                     //Term
                        docIdsOffset,                //offset in the docids file in which the docids list starts
                        frequenciesOffset,           //offset in the frequencies file in which the frequencies list starts
                        idf,                         //idf
                        docIdsCompressed.length,     //length in bytes of the compressed docids list
                        frequenciesCompressed.length,//length in bytes of the compressed frequencies list
                        docIds.size(),               //Length of the posting list of the current term
                        blocksOffset,               //Offset of the SkipBlocks in the SkipBlocks file
                        blocks.size(),              //number of SkipBlocks
                        tfidfTermUpperBound,         //term upper bound (tfidf)
                        bm25TermUpperBound           //term upper bound (bm25)
                );


                // Write the lexicon entry to the lexicon file
                lexiconEntry.writeToFile(lexiconFile, lexiconEntry);

                // Update offsets for docIds and frequencies
                docIdsOffset += docIdsCompressed.length;
                frequenciesOffset += frequenciesCompressed.length;


            }else {//No compression

                //Write the docIds and frequencies of the current term in the respective files
                try {

                    //Size skip block
                    int blocksLength = (int) Math.floor(Math.sqrt(docIds.size()));

                    //Number of postings
                    int blocksElements = 0;

                    // Variable to store the bm25 score for the current doc id
                    double tf_currentBm25;

                    // Iterate through docIds and frequencies, write to files, and create skip blocks
                    for(int i=0; i < docIds.size(); i++) {

                        //Retrieve the maximum frequency to compute the TFIDF term upper bound
                        if(frequencies.get(i) > maxFreq){
                            maxFreq = frequencies.get(i);
                        }

                        //Compute the bm25 scoring for the current document
                        tf_currentBm25 = frequencies.get(i)/ (Parameters.K1 * ((1-Parameters.B) + Parameters.B *
                                ( (double) DocInfo.getDocLenFromFile(documentIndex, docIds.get(i)) /
                                        statistics.getAvdl()) + frequencies.get(i)));

                        // Update the maximum tf for bm25
                        if(tf_currentBm25 > tf_maxScoreBm25){
                            tf_maxScoreBm25 = tf_currentBm25;
                        }


                        //Write the docIds (long) to the end of the docIds file
                        docIdsFile.writeLong(docIds.get(i));

                        //Write the frequencies (integer) to the end of the frequencies file
                        frequenciesFile.writeInt(frequencies.get(i));

                        //We create a new skip block iIf we are at a skip position
                        if(((i+1)%blocksLength == 0) || ((i + 1) == docIds.size())){

                            //if the size of the skip block is less than blocksLength, we use the reminder
                            // to get the actual dimension of the skip block (if we are at the end we can have less
                            // than skipBlockLength postings)
                            // Since there is no compression the lengths of docids and frequencies skip blocks are the same
                            int currentSkipBlockSize = ((i + 1) % blocksLength == 0) ? blocksLength : ((i+1) % blocksLength);

                            //Create skip block
                            blocks.add(new Block(
                                    (long) blocksElements *Long.BYTES,
                                    currentSkipBlockSize,
                                    (long) blocksElements *Integer.BYTES,
                                    currentSkipBlockSize,
                                    docIds.get(i)
                            ));

                            //Increment the number of elements in order to obtain the first offset equal to 0
                            blocksElements += currentSkipBlockSize;
                        }

                    }


                } catch (IOException e) {
                    // Handle the exception if an error occurs while writing compressed data to file
                    System.err.println(" *** File not found: " + e.getMessage());
                    throw new RuntimeException(e);
                }

                //Compute idf
                double idf = Math.log(statistics.getNDocs()/ (double)docIds.size())/Math.log(2);
                // Compute term upper bounds for tfidf and bm25
                int tfidfTermUpperBound = (int) Math.ceil((1 + Math.log(maxFreq) / Math.log(2))*idf);
                int bm25TermUpperBound = (int) Math.ceil(tf_maxScoreBm25*idf);

                // Create a Term object with the current computed values
                lexiconEntry = new Term(
                        minTerm,                     //Term
                        docIdsOffset,                //offset in the docids file in which the docids list starts
                        frequenciesOffset,           //offset in the frequencies file in which the frequencies list starts
                        idf,                         //idf of the term for future scoring
                        docIds.size(),               //length in number of long in the docids list
                        frequencies.size(),          //length number of integers in the frequencies list
                        docIds.size(),               //Length of the posting list of the current term
                        blocksOffset,               //Offset of the SkipBlocks in the SkipBlocks file
                        blocks.size(),               //number of SkipBlocks
                        tfidfTermUpperBound,         //term upper bound (tfidf)
                        bm25TermUpperBound           //term upper bound (bm25)
                );


                // Write the lexicon entry to the lexicon file
                lexiconEntry.writeToFile(lexiconFile, lexiconEntry);

                // Update offsets for docIds and frequencies
                docIdsOffset += 8L*docIds.size();
                frequenciesOffset += 4L*frequencies.size();


            }

            // Write each skip block to the SkipBlocks file and update the blocks offset
            for(Block s : blocks){
                s.writeToFile(blocksFile);
                blocksOffset += Parameters.BLOCK_LENGTH;
            }

            //Clear the accumulators for the next iteration
            docIds.clear();
            frequencies.clear();
            blocks.clear();
            minTerm = null;  //Reset minTerm to null to avoid using it as the first min term found at the beginning of the merge
            blocksWithMinTerm.clear(); //Clear the list of blocks with the min term
        }

        System.out.println(" *** Closing the streams of the files. Analyzed " + j + " terms ***");

        try {
            // Close the file streams
            for (int i = 0; i < num_blocks; i++) {
                randomAccessFileDocIds[i].close();
                randomAccessFilesFrequencies[i].close();
                randomAccessFilesLexicon[i].close();
            }

            lexiconFile.close();
            docIdsFile.close();
            frequenciesFile.close();

        } catch (RuntimeException | IOException e) {
            //Handle the exception if any file stream cannot be closed
            System.err.println(" *** File not found: ***" + e.getMessage());
            throw new RuntimeException(e);
        }

        // Delete blocks if successful
        if(deleteBlocks(num_blocks)){
            System.out.println(" *** Blocks deleted successfully ***");
        }

        System.out.println(" *** Total processing time: " + (System.nanoTime() - start)/1000000000+ "s ***");
        System.out.println(" *** MERGING COMPLETE ***");
    }


    /**
     * Reads the next lexicon entry from the provided lexicon block file, starting from the specified offset. It reads the first 60 bytes,
     * and if resetOffset is true, it resets the offset to its initial value; otherwise, it maintains the cursor position after reading the entry.
     *
     * @param randomAccessFileLexicon RandomAccessFile of the lexicon block file
     * @param offset Offset from where we read the lexicon entry
     * @return Term object containing the term information
     */
    public static Term readNextTermInfo(RandomAccessFile randomAccessFileLexicon, int offset) {

        //Array of bytes to store the term
        byte[] termBytes = new byte[Parameters.TERM_BYTES];

        //String representation of the term
        String term;

        //Term object containing the term information
        Term termInfo;

        try {
            //Set the file pointer to the start of the lexicon entry
            randomAccessFileLexicon.seek(offset);

            //Read the first 48 bytes that contain the term
            randomAccessFileLexicon.readFully(termBytes, 0, Parameters.TERM_BYTES);

            //Convert the bytes to a string and trim it
            term = new String(termBytes, Charset.defaultCharset()).trim();

            //Instantiate the TermInfo object reading the next 3 integers from the file
            termInfo = new Term(term, randomAccessFileLexicon.readLong(), randomAccessFileLexicon.readLong(), randomAccessFileLexicon.readInt());

            return termInfo;

        } catch (IOException e) {
            //Handle IOException (EOF reached while reading the next lexicon entry)
            return null;
        }
    }


    /**
     * Return a statistics object containing the information about the blocks
     *  @return Statistics object with block information
     */
    private static Statistics readStatistics(){
        return new Statistics();
    }


    /**
     * Return true if all the files have reached the end and false otherwise.
     * @param endOfBlocks Array of boolean values indicating whether each file has reached the end
     * @param numberOfBlocks Number of blocks (length of the array)
     * @return true if all files have reached the end, false otherwise
     */
    private static boolean endOfAllFiles(boolean[] endOfBlocks, int numberOfBlocks) {

        //Check if each block check has reached the end of the file
        for(int i = 0; i < numberOfBlocks; i++) {
            if(!endOfBlocks[i])
                //At least one file not reached the end of the file
                return false;
        }
        //All the files have reached the end of the file
        return true;
    }

    /**
     * Delete the partial blocks of lexicon and inverted index
     * @param numberOfBlocks number of partial blocks to delete
     * @return true if all the files are successfully deleted, false otherwise
     */
    private static boolean deleteBlocks(int numberOfBlocks) {
        File file;
        for (int i = 0; i < numberOfBlocks; i++) {
            file = new File(Parameters.DOCID_BLOCK_PATH +(i+1)+".txt");
            if(!file.delete())
                return false;
            file = new File(Parameters.FREQ_BLOCK_PATH +(i+1)+".txt");
            if(!file.delete())
                return false;
            file = new File(Parameters.LEXICON_BLOCK_PATH+(i+1)+".txt");
            if(!file.delete())
                return false;
        }
        return true;
    }
}
