package unipi.mircv;
import org.javatuples.Pair;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedList;

public class Merger {

    /**
     * This method merges the inverted index and the lexicon blocks into one single file.
     * @param compress If true, the inverted index and the lexicon blocks will be compressed using VBE, otherwise
     *                 they will be written without compression.
     */
    public static void merge(boolean compress, boolean debug) {

        System.out.println("[MERGER] Merging lexicon blocks and inverted index blocks...");

        //Retrieve the time at the beginning of the computation
        long start = System.nanoTime();

        //Retrieve the blocks statistics
        Statistics statistics = readStatistics();

        int num_blocks = statistics.getNBlocks();

        //Arrays of random access files, for docIds, frequencies and lexicon blocks
        RandomAccessFile[] randomAccessFileDocIds = new RandomAccessFile[num_blocks];
        RandomAccessFile[] randomAccessFilesFrequencies = new RandomAccessFile[num_blocks];
        RandomAccessFile[] randomAccessFilesLexicon = new RandomAccessFile[num_blocks];

        //Files for the final result
        RandomAccessFile lexiconFile;
        RandomAccessFile docIdsFile;
        RandomAccessFile frequenciesFile;
        RandomAccessFile blocksFile;
        RandomAccessFile documentIndex;

        //Accumulators to hold the current offset, starting from which the next list of postings will be written
        long docIdsOffset = 0;
        long frequenciesOffset = 0;
        long blocksOffset = 0;

        //Array of the current offset reached in each lexicon block
        int[] offsets = new int[num_blocks];

        //Array of boolean, each i-th entry is true, if the i-th block has reached the end of the lexicon block file
        boolean[] endOfBlock = new boolean[num_blocks];

        //Set each offset equal to 0, the starting offset of each lexicon block
        //Set each boolean equal to false, at the beginning no block has reached the end
        for (int i = 0; i < num_blocks; i++) {
            offsets[i] = 0;
            endOfBlock[i] = false;
        }

        //String to keep the min term among all the current terms in each lexicon block, it is used to determine the
        // term of which the posting lists must be merged
        String minTerm = null;

        //TermInfo to keep the term's information to be written in the lexicon file
        Term lexiconEntry;

        //Used to store the information of the current term entry for each lexicon block file
        Term[] curTerm = new Term[num_blocks];

        //Contains the list of all the blocks containing the current min term
        LinkedList<Integer> blocksWithMinTerm = new LinkedList<>();

        //Array to store the docIds and frequencies of the posting list of the current min term in the current block
        ArrayList<Long> docIds = new ArrayList<>();
        ArrayList<Integer> frequencies = new ArrayList<>();

        //Array to store the information about the blocks
        ArrayList<Block> blocks = new ArrayList<>();

        //Arrays to store the compressed docIds and frequencies of the posting list of the current min term
        byte[] docIdsCompressed;
        byte[] frequenciesCompressed;


        try {
            //Create a stream for each random access files of each block, the stream is opened as read only
            for (int i = 0; i < num_blocks; i++) {
                //noinspection resource
                randomAccessFileDocIds[i] = new RandomAccessFile(Parameters.II_DOCID_BLOCK_PATH+(i+1)+".txt", "r");
                //noinspection resource
                randomAccessFilesFrequencies[i] = new RandomAccessFile(Parameters.II_FREQ_BLOCK_PATH+(i+1)+".txt", "r");
                //noinspection resource
                randomAccessFilesLexicon[i] = new RandomAccessFile(Parameters.LEXICON_BLOCK_PATH+(i+1)+".txt", "r");
                if(debug){
                    System.out.println("[DEBUG] Block " + i + " opened");
                }
            }

            //Create a stream for the lexicon file, the docids file and the frequencies file, the stream is opened as write only
            lexiconFile = new RandomAccessFile(Parameters.LEXICON_PATH, "rw");
            docIdsFile = new RandomAccessFile(Parameters.II_DOCID_PATH, "rw");
            frequenciesFile = new RandomAccessFile(Parameters.II_FREQ_PATH, "rw");
            blocksFile = new RandomAccessFile(Parameters.BLOCKS_PATH, "rw");
            documentIndex = new RandomAccessFile(DocIndex.DOCUMENT_INDEX_PATH, "r");


        } catch (FileNotFoundException e) {
            System.err.println("[MERGER] File not found: " + e.getMessage());
            throw new RuntimeException(e);
        }

        //Read the first term of each lexicon block
        for (int i = 0; i < curTerm.length; i++) {
            curTerm[i] = readNextTermInfo(randomAccessFilesLexicon[i],offsets[i]);

            if(curTerm[i] == null) {
                endOfBlock[i] = true;
            }

            //TODO check 68
            //Update the offset to the offset of the next file to be read
            offsets[i] += 68;
        }

        long j = 1;
        //Iterate over all the lexicon blocks, until the end of the lexicon block file is reached for each block
        while(!endOfAllFiles(endOfBlock, num_blocks)) {

            j++;

            // every 25000 blocks prints info
            if(j%25000 == 0){
                System.out.println("[MERGER] Processing time: " + (System.nanoTime() - start)/1000000000+ "s. Processed " + j + " terms");
            }

            //For each block read the next term
            for(int i = 0; i < num_blocks; i++) {

                //Avoid to read from the block if the end of the block is reached
                if(endOfBlock[i]) {
                    continue;
                }

                //If the current term is the lexicographically smaller than the min term, then update the min term.
                if(minTerm == null || curTerm[i].getTerm().compareTo(minTerm) < 0) {

                    //If we've found another min term, then update the min term.
                    minTerm = curTerm[i].getTerm();

                    //Clear the array of blocks with the min term.
                    blocksWithMinTerm.clear();

                    //Add the current block to the list of blocks with the min term.
                    blocksWithMinTerm.add(i);

                    //Else if the current term is equal to the min term, then add the current block to the list of blocks with the min term.
                } else if (curTerm[i].getTerm().compareTo(minTerm) == 0) {

                    //Add the current block to the list of blocks with the min term.
                    blocksWithMinTerm.add(i);
                }
            }//At this point we have the current min term.

            //Check if we've reached the end of the merge.
            if(endOfAllFiles(endOfBlock, num_blocks)) {
                System.out.println("END OF ALL FILES");
                break;
            }

            //System.out.println("----------- TERM: " + minTerm + " -----------");
            //System.out.println(blocksWithMinTerm);

            //Merge the posting lists of the current min term in the blocks containing the term
            for (Integer integer : blocksWithMinTerm) {

                //Append the current term docIds to the docIds accumulator
                docIds.addAll(PostingList.readPLDocId(randomAccessFileDocIds[integer], curTerm[integer].getOffsetDocId(), curTerm[integer].getPostingListLength()));

                //System.out.println("Current docIds: " + docIds);

                //Append the current term frequencies to the frequencies accumulator
                frequencies.addAll(PostingList.readPlFreq(randomAccessFilesFrequencies[integer], curTerm[integer].getOffsetFrequency(), curTerm[integer].getPostingListLength()));

                //System.out.println("Current term frequencies: " + frequencies);

                //Read the lexicon entry from the current block and move the pointer of the file to the next term
                curTerm[integer] = readNextTermInfo(randomAccessFilesLexicon[integer], offsets[integer]);

                //Check if the end of the block is reached or a problem during the reading occurred
                if(curTerm[integer] == null) {
                    if(debug) {
                        System.out.println("[DEBUG] Block " + integer + " has reached the end of the file");
                    }

                    endOfBlock[integer] = true;
                    continue;
                }

                //Increment the offset of the current block to the starting offset of the next term
                offsets[integer] += Parameters.TERM_BYTES + Parameters.OFFSET_DOCIDS_BYTES +
                                Parameters.OFFSET_FREQUENCIES_BYTES + Parameters.POSTING_LIST_BYTES;

            }

            //Maximum term frequency
            int maxFreq = 0;

            //Maximum tf for bm25
            double tf_maxScoreBm25 = 0;

            if(compress){

                Pair<Double, Double> maxscore = Pair.with(0.0, 0.0);

                //Compress the list of docIds using VBE and create the list of skip blocks for the list of docids
                docIdsCompressed = Compressor.variableByteEncodeDocId(docIds, blocks);

                //Compress the list of frequencies using VBE and update the frequencies information in the skip blocks
                frequenciesCompressed = Compressor.variableByteEncodeFreq(frequencies, blocks, docIds, maxscore, documentIndex, statistics);

                //Write the docIds and frequencies of the current term in the respective files
                try {
                    docIdsFile.write(docIdsCompressed);
                    frequenciesFile.write(frequenciesCompressed);
                } catch (IOException e) {
                    System.err.println("[MERGER] File not found: " + e.getMessage());
                    throw new RuntimeException(e);
                }

                //Compute idf
                double idf = Math.log(statistics.getNDocs()/ (double)docIds.size())/Math.log(2);

                //Compute the tfidf term upper bound
                int tfidfTermUpperBound = (int) Math.ceil((1 + Math.log(maxscore.getValue0()) / Math.log(2))*idf);

                //Compute the bm25 term upper bound
                int bm25TermUpperBound = (int) Math.ceil(maxscore.getValue1()*idf);


                lexiconEntry = new Term(
                        minTerm,                     //Term
                        docIdsOffset,                //offset in the docids file in which the docids list starts
                        frequenciesOffset,           //offset in the frequencies file in which the frequencies list starts
                        idf,                         //idf
                        docIdsCompressed.length,     //length in bytes of the compressed docids list
                        frequenciesCompressed.length,//length in bytes of the compressed frequencies list
                        docIds.size(),               //Length of the posting list of the current term
                        blocksOffset,            //Offset of the SkipBlocks in the SkipBlocks file
                        blocks.size(),           //number of SkipBlocks
                        tfidfTermUpperBound,         //term upper bound for the tfidf
                        bm25TermUpperBound           //term upper bound for the bm25
                );

                //For DEBUG
                if(debug && j%25000 == 0) {
                    System.out.println("[DEBUG] Current lexicon entry: " + lexiconEntry);
                    System.out.println("[DEBUG] Number of blocks created: " + blocks.size());
                }

                lexiconEntry.writeToFile(lexiconFile, lexiconEntry);

                docIdsOffset += docIdsCompressed.length;
                frequenciesOffset += frequenciesCompressed.length;


            }else {//No compression

                //Write the docIds and frequencies of the current term in the respective files
                try {

                    //Dimension of each skip block
                    int blocksLength = (int) Math.floor(Math.sqrt(docIds.size()));

                    //Number of postings
                    int blocksElements = 0;

                    //To store the bm25 score for the current doc id
                    double tf_currentBm25;

                    //Write the docids and frequencies in their respective files and create the skip blocks
                    for(int i=0; i < docIds.size(); i++) {

                        //Retrieve the maximum to compute the TFIDF term upper bound
                        if(frequencies.get(i) > maxFreq){
                            maxFreq = frequencies.get(i);
                        }

                        //Compute the bm25 scoring for the current document
                        tf_currentBm25 = frequencies.get(i)/ (Parameters.K1 * ((1-Parameters.B) + Parameters.B *
                                ( (double) DocInfo.getDocLenFromFile(documentIndex, docIds.get(i)) /
                                        statistics.getAvdl()) + frequencies.get(i)));

                        if(tf_currentBm25 > tf_maxScoreBm25){
                            tf_maxScoreBm25 = tf_currentBm25;
                        }


                        //Write the docIds as a long to the end of the docIds file
                        docIdsFile.writeLong(docIds.get(i));

                        //Write the frequencies as an integer to the end of the frequencies file
                        frequenciesFile.writeInt(frequencies.get(i));

                        //If we're at a skip position, we create a new skip block
                        if(((i+1)%blocksLength == 0) || ((i + 1) == docIds.size())){

                            //if the size of the skip block is less than blocksLength then used the reminder,
                            // to get the actual dimension of the skip block, since if we're at the end we can have less
                            // than skipBlockLength postings
                            // Since we don't have compression the lengths of docids and frequencies skip blocks are the same
                            int currentSkipBlockSize = ((i + 1) % blocksLength == 0) ? blocksLength : ((i+1) % blocksLength);

                            //Creation of the skip block
                            blocks.add(new Block(
                                    (long) blocksElements *Long.BYTES,
                                    currentSkipBlockSize,
                                    (long) blocksElements *Integer.BYTES,
                                    currentSkipBlockSize,
                                    docIds.get(i)
                            ));

                            //Increment the number of elements seen until now, otherwise we're not able to obtain the first offset
                            // equal to 0
                            blocksElements += currentSkipBlockSize;
                        }

                    }


                } catch (IOException e) {
                    System.err.println("[MERGER] File not found: " + e.getMessage());
                    throw new RuntimeException(e);
                }

                //Compute idf
                double idf = Math.log(statistics.getNDocs()/ (double)docIds.size())/Math.log(2);

                //Compute the tfidf term upper bound
                int tfidfTermUpperBound = (int) Math.ceil((1 + Math.log(maxFreq) / Math.log(2))*idf);

                //Compute the bm25 term upper bound
                int bm25TermUpperBound = (int) Math.ceil(tf_maxScoreBm25*idf);


                //Instantiate a new TermInfo object with the current term information, here we use the information in
                //the docids and frequencies objects
                lexiconEntry = new Term(
                        minTerm,                     //Term
                        docIdsOffset,                //offset in the docids file in which the docids list starts
                        frequenciesOffset,           //offset in the frequencies file in which the frequencies list starts
                        idf,                         //idf of the term for future scoring
                        docIds.size(),               //length in number of long in the docids list
                        frequencies.size(),          //length number of integers in the frequencies list
                        docIds.size(),               //Length of the posting list of the current term
                        blocksOffset,            //Offset of the SkipBlocks in the SkipBlocks file
                        blocks.size(),           //number of SkipBlocks
                        tfidfTermUpperBound,         //term upper bound for the tfidf
                        bm25TermUpperBound           //term upper bound for the bm25
                );

                //For DEBUG
                if(debug && j%25000 == 0) {
                    System.out.println("[DEBUG] Current lexicon entry: " + lexiconEntry);
                    System.out.println("[DEBUG] Number of blocks created: " + blocks.size());
                }

                lexiconEntry.writeToFile(lexiconFile, lexiconEntry);

                docIdsOffset += 8L*docIds.size();
                frequenciesOffset += 4L*frequencies.size();


            }

            for(Block s : blocks){
                s.writeToFile(blocksFile);
                blocksOffset += Parameters.BLOCK_LENGTH;
            }

            //Clear the accumulators for the next iteration
            docIds.clear();
            frequencies.clear();
            blocks.clear();
            minTerm = null; //Otherwise it will be always the first min term found at the beginning of the merge
            blocksWithMinTerm.clear(); //Clear the list of blocks with the min term
        }

        System.out.println("[MERGER] Closing the streams of the files. Analyzed " + j + " terms");

        try {
            //Close the streams of the files
            for (int i = 0; i < num_blocks; i++) {
                randomAccessFileDocIds[i].close();
                randomAccessFilesFrequencies[i].close();
                randomAccessFilesLexicon[i].close();
            }

            lexiconFile.close();
            docIdsFile.close();
            frequenciesFile.close();

        } catch (RuntimeException | IOException e) {
            System.err.println("[MERGER] File not found: " + e.getMessage());
            throw new RuntimeException(e);
        }

        if(deleteBlocks(num_blocks)){
            System.out.println("[MERGER] Blocks deleted successfully");
        }

        System.out.println("[MERGER] Total processing time: " + (System.nanoTime() - start)/1000000000+ "s");
        System.out.println("[MERGER] MERGING PROCESS COMPLETE");
    }


    /**
     * Reads the next lexicon entry from the given lexicon block file, starting from offset it will read the first 60
     * bytes, then if resetOffset is true, it will reset the offset to the value present ate the beginning, otherwise it
     * will keep the cursor as it is after the read of the entry.
     * @param randomAccessFileLexicon RandomAccessFile of the lexicon block file
     * @param offset offset starting from where to read the lexicon entry
     */
    public static Term readNextTermInfo(RandomAccessFile randomAccessFileLexicon, int offset) {

        //Array of bytes in which put the term
        byte[] termBytes = new byte[Parameters.TERM_BYTES];

        //String containing the term
        String term;

        //TermInfo containing the term information to be returned
        Term termInfo;

        try {
            //Set the file pointer to the start of the lexicon entry
            randomAccessFileLexicon.seek(offset);

            //Read the first 48 containing the term
            randomAccessFileLexicon.readFully(termBytes, 0, Parameters.TERM_BYTES);

            //Convert the bytes to a string and trim it
            term = new String(termBytes, Charset.defaultCharset()).trim();

            //Instantiate the TermInfo object reading the next 3 integers from the file
            termInfo = new Term(term, randomAccessFileLexicon.readLong(), randomAccessFileLexicon.readLong(), randomAccessFileLexicon.readInt());

            return termInfo;

        } catch (IOException e) {
            //System.err.println("[ReadNextTermInfo] EOF reached while reading the next lexicon entry");
            return null;
        }
    }


    /**
     * Return a statistics object containing the information about the blocks
     */
    private static Statistics readStatistics(){
        return new Statistics();
    }

    /**
     * Check if all the files have reached the end of the file, and if so return true, otherwise return false
     * @param endOfBlocks array of boolean indicating if the files have reached the end of the file
     * @param numberOfBlocks number of blocks, it is the length of the array
     * @return true if all the files have reached the end of the file, and if so return true, otherwise return false
     */
    private static boolean endOfAllFiles(boolean[] endOfBlocks, int numberOfBlocks) {

        //For each block check if it has reached the end of the file
        for(int i = 0; i < numberOfBlocks; i++) {
            if(!endOfBlocks[i])
                //At least one file has not reached the end of the file
                return false;
        }
        //All the files have reached the end of the file
        return true;
    }

    /**
     * Delete the partial block of lexicon and inverted index
     * @param numberOfBlocks number of partial blocks
     * @return true if all the files are successfully deleted, false otherwise
     */
    private static boolean deleteBlocks(int numberOfBlocks) {
        File file;
        for (int i = 0; i < numberOfBlocks; i++) {
            file = new File(Parameters.II_DOCID_BLOCK_PATH+(i+1)+".txt");
            if(!file.delete())
                return false;
            file = new File(Parameters.II_FREQ_BLOCK_PATH+(i+1)+".txt");
            if(!file.delete())
                return false;
            file = new File(Parameters.LEXICON_BLOCK_PATH+(i+1)+".txt");
            if(!file.delete())
                return false;
        }
        return true;
    }
}
