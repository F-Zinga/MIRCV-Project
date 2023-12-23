package unipi.mircv;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Iterator;

public class PostingList extends ArrayList<Posting> {

    private long docId;

    //Current frequency
    private int freq;

    //If we've reached the end of the posting list
    private boolean endPosting;

    //Iterator to iterate over the posting list
    private Iterator<Posting> iterator;

    //Iterator to iterate over the skip blocks
    private Iterator<Block> blocksIterator;

    //TermInfo of the term, used to retrieve the idf
    private Term termInfo;

    //Variable used to store the current skip block information
    private Block currentBlock;

    //Random access file used to read the docids
    RandomAccessFile randomAccessFileDocIds;

    //Random access file used to read the frequencies
    RandomAccessFile randomAccessFileFrequencies;

    //Random access file used to read the skip blocks
    RandomAccessFile randomAccessFileBlocks;

    private Settings settings;


    public void setCurrentBlock(){

        currentBlock = blocksIterator.next();
    }
    public boolean getEndPosting() {
        return endPosting;
    }

    public long getDocId() {
        return docId;
    }
    /**
     * Clear the array list
     */
    public void closeList(){
        this.clear();
        try {
            randomAccessFileDocIds.close();
            randomAccessFileFrequencies.close();
            randomAccessFileBlocks.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public void setDocId(long docId) {
        this.docId = docId;
    }

    public void setEndPosting() {
        this.endPosting = true;
    }
    /**
     * Loads the posting list of the given term in memory, this list uses the skipping mechanism.
     * @param termInfo Lexicon entry of the term, used to retrieve the offsets and the lengths of the posting list
     */
    public void openList(Term termInfo){

        //Set the terminfo of the posting list
        this.termInfo = termInfo;
        System.out.println(termInfo.toString());

        //Load the configuration used to build the inverted index
        settings = new Settings();
        settings.loadSettings();

        //Open the stream with the posting list random access files
        try {
            randomAccessFileDocIds = new RandomAccessFile(Parameters.DOCID_PATH, "r");
            randomAccessFileFrequencies = new RandomAccessFile(Parameters.FREQ_PATH, "r");
            randomAccessFileBlocks = new RandomAccessFile(Parameters.BLOCKS_PATH,"r");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }


        //Load the blocks list of the current term's posting list
        //Blocks of the posting list
        ArrayList<Block> skipBlocks = getPostingListBlock(
                randomAccessFileBlocks,
                termInfo.getOffsetSkipBlock(),
                termInfo.getNumberOfSkipBlocks()
        );

        //initialize the skip blocks iterator
        blocksIterator = skipBlocks.iterator();

        //move the skip blocks' iterator to the first skip block
        setCurrentBlock();

        //Load the posting list of the current block
        loadPostingList();
    }

    /**
     * Loads the posting list of the current block
     */
    public void loadPostingList(){
        //Retrieve the docids and the frequencies
        ArrayList<Long> docids;
        ArrayList<Integer> frequencies;

        //If the compression is enabled, then read the posting lists files with the compression
        if(settings.isCompressed()) {

            docids = readPlDocIdCompressed(randomAccessFileDocIds,
                    termInfo.getOffsetDocId() + currentBlock.getDocIDOffset(),
                    currentBlock.getDocIDSize());

            frequencies = readPlFreqCompressed(randomAccessFileFrequencies,
                    termInfo.getOffsetFrequency() + currentBlock.getFrqOffset(),
                    currentBlock.getFrqSize());
        }else {//Read without compression

            docids = readPLDocId(randomAccessFileDocIds,
                    termInfo.getOffsetDocId() + currentBlock.getDocIDOffset(),
                    currentBlock.getDocIDSize());

            frequencies = readPlFreq(randomAccessFileFrequencies,
                    termInfo.getOffsetFrequency() + currentBlock.getFrqOffset(),
                    currentBlock.getFrqSize());
        }

        //Remove the previous postings
        this.clear();

        //Create the array list of postings
        for(int i = 0; i < docids.size() ; i++){
            this.add(new Posting(docids.get(i), frequencies.get(i)));
        }

        //Update the iterator for the current posting list
        iterator = this.iterator();


        if(settings.getDebug()){
            System.out.println("------------------");
            System.out.println("[DEBUG] Partial posting list: " + this);
        }

    }

    /**
     * Moves the iterator to the next posting in the iteration
     * @return the next posting
     */
    public Posting next(){

        //System.out.println("This.docID: " + this.docId + "/" + currentSkipBlock.maxDocid);
        if(this.docId == currentBlock.getMaxDocID()){
            //System.out.println("Last docId of the block");
            if(blocksIterator.hasNext()){
                setCurrentBlock();
            }else {
                //System.out.println("Posting list ended");
                setEndPosting();
                return null;
            }

            loadPostingList();

        }

        //Get the next posting in the iteration
        Posting result = iterator.next();

        //Update the current information
        this.docId = result.getDocID();
        this.freq = result.getTermFrequency();

        //Return the next
        return result;
    }

    /**
     * Reads the posting list's ids from the given inverted index file, starting from offset it will read the number
     * of docIds indicated by the given length parameter. It assumes that the file is compressed using VBE.
     * @param randomAccessFileDocIds RandomAccessFile of the docIds block file
     * @param offset offset starting from where to read the posting list
     * @param length length of the bytes of the encoded posting list
     */
    public static ArrayList<Long> readPlDocIdCompressed(RandomAccessFile randomAccessFileDocIds, long offset, int length) {

        byte[] docidsByte = new byte[length];

        try {

            //Set the file pointer to the start of the posting list
            randomAccessFileDocIds.seek(offset);

            randomAccessFileDocIds.readFully(docidsByte, 0, length);

            return Compressor.variableByteDecodeLong(docidsByte);

        } catch (IOException e) {
            System.err.println("[ReadPostingListDocIds] Exception during seek");
            throw new RuntimeException(e);
        }
    }


    /**
     * Reads the posting list's frequencies from the given inverted index file, starting from offset it will read the number
     * of docIds indicated by the given length parameter. It assumes that the file is compressed using VBE.
     * @param randomAccessFileFreq RandomAccessFile of the frequencies file
     * @param offset offset starting from where to read the posting list
     * @param length length of the bytes of the encoded posting list
     */
    public static ArrayList<Integer> readPlFreqCompressed(RandomAccessFile randomAccessFileFreq, long offset, int length) {

        byte[] docidsByte = new byte[length];

        try {

            //Set the file pointer to the start of the posting list
            randomAccessFileFreq.seek(offset);

            randomAccessFileFreq.readFully(docidsByte, 0, length);

            return Compressor.variableByteDecode(docidsByte);

        } catch (IOException e) {
            System.err.println("[ReadPostingListDocIds] Exception during seek");
            throw new RuntimeException(e);
        }
    }

    /**
     * Reads the posting list's ids from the given inverted index file, starting from offset it will read the number
     * of docIds indicated by the given length parameter.
     * @param randomAccessFileDocIds RandomAccessFile of the docIds block file
     * @param offset offset starting from where to read the posting list
     * @param length length of the posting list to be read
     */
    public static ArrayList<Long> readPLDocId(RandomAccessFile randomAccessFileDocIds, long offset, int length) {

        //ArrayList to store the posting list's ids
        ArrayList<Long> list = new ArrayList<>();

        try {

            //Set the file pointer to the start of the posting list
            randomAccessFileDocIds.seek(offset);

        } catch (IOException e) {
            System.err.println("[ReadPostingListDocIds] Exception during seek");
            throw new RuntimeException(e);
        }

        //Read the docIds from the file
        for(int i = 0; i < length; i ++) {
            try {

                //Read the docId and add it to the list
                list.add(randomAccessFileDocIds.readLong());

            } catch (IOException e) {
                System.err.println("[ReadPostingListDocIds] Exception during read");
                throw new RuntimeException(e);
            }
        }

        //Return the list
        return list;
    }

    /**
     * Reads the posting list's frequencies from the given inverted index file, starting from offset it will read the
     * number of frequencies indicated by the given length parameter.
     * @param randomAccessFileFrequencies RandomAccessFile of the frequencies block file
     * @param offset offset starting from where to read the posting list
     * @param length length of the posting list to be read
     */
    public static ArrayList<Integer> readPlFreq(RandomAccessFile randomAccessFileFrequencies, long offset, int length) {

        //ArrayList to store the posting list's frequencies
        ArrayList<Integer> list = new ArrayList<>();

        try {

            //Set the file pointer to the start of the posting list
            randomAccessFileFrequencies.seek(offset);

        } catch (IOException e) {
            System.err.println("[ReadPostingListFrequencies] Exception during seek");
            throw new RuntimeException(e);
        }

        //Read the frequencies from the file
        for(int i = 0; i < length; i ++) {
            try {

                //Read the frequency and add it to the list
                list.add(randomAccessFileFrequencies.readInt());

            } catch (IOException e) {
                System.err.println("[ReadPostingListFrequencies] Exception during read");
                throw new RuntimeException(e);
            }
        }

        //Return the list
        return list;
    }




    /**
     * Search the next doc id of the current posting list, such that is greater or equal to the searched doc id.
     * It exploits the skip blocks to traverse faster the posting list
     * @param searchedDocId doc id to search
     * posting are present in the posting list
     */
    public void nextGEQ(long searchedDocId){

        if(this.docId == searchedDocId){
            return;
        }

        /*
        if(settings.getDebug()){
            System.out.println("[DEBUG] Max docId in current skipBlock < searched docId: " + currentBlock.getMaxDocID() +" < "+ searchedDocId);
        }

         */

        //Move to the next skip block until we find that the searched doc id can be contained in the
        // portion of the posting list described by the skip block
        while(currentBlock.getMaxDocID() < searchedDocId){

            //If it is possible to move to the next skip block, then move the iterator
            if(blocksIterator.hasNext()){

                /*
                //Debug
                if(configuration.getDebug()){
                    System.out.println("[DEBUG] Changing the skip block");
                }
                */

                //Move the iterator to the next skip block
                setCurrentBlock();
                loadPostingList();
            }else{

                //All the skip blocks are traversed, the posting list doesn't contain a doc id GEQ than
                // the one searched

                /*
                //Debug
                if(configuration.getDebug()){
                    System.out.println("[DEBUG] End of posting list");
                }

                 */

                //Set the end of posting list flag
                setEndPosting();

                return;
            }

            /*
            if(configuration.getDebug()){
                System.out.println("[DEBUG] Max docId in the new skipBlock < searched docId: " + currentBlock.getMaxDocID() +" < "+ searchedDocId);
            }

             */
        }

        //load the posting lists related to the current skip block, once we've found a posting list portion
        // that can contain the searched doc id


        //Helper variable to hold the posting during the traversing of the posting list
        Posting posting;

        //While we have more postings
        while(iterator.hasNext()){
            //Move to the next posting
            posting = next();
            if(posting.getDocID() > searchedDocId){
                return;
            }
        }

        //No postings are GEQ in the current posting list, we've finished the traversing the whole posting list
        if(!blocksIterator.hasNext())
            setEndPosting();
    }

    /**
     * Reads the posting list's skip blocks from the given file, starting from offset it will read the
     * number of skip blocks indicated by the given length parameter.
     * @param randomAccessFileSkipBlocks RandomAccessFile of the skip blocks' file
     * @param offset offset starting from where to read the skip blocks'
     * @param length number of skip blocks to read
     */
    public static ArrayList<Block> getPostingListBlock(RandomAccessFile randomAccessFileSkipBlocks, long offset, int length) {

        //ArrayList to store the posting list's frequencies
        ArrayList<Block> list = new ArrayList<>();

        try {

            //Set the file pointer to the start of the posting list
            randomAccessFileSkipBlocks.seek(offset);

        } catch (IOException e) {
            System.err.println("[ReadPostingListSkipBlocks] Exception during seek");
            throw new RuntimeException(e);
        }

        //Read the skip blocks from the file
        for(int i = 0; i < length; i ++) {
            try {

                long first = randomAccessFileSkipBlocks.readLong();
                int second = randomAccessFileSkipBlocks.readInt();
                long third = randomAccessFileSkipBlocks.readLong();
                int fourth = randomAccessFileSkipBlocks.readInt();
                long fifth = randomAccessFileSkipBlocks.readLong();

                //Read the next skip block from the file and add it to the result list
                list.add(new Block(
                        first, //Docids offset
                        second,  //Docids length
                        third, //Frequencies offset
                        fourth,  //Frequencies length
                        fifth) //Max docid in the skip block
                );

            } catch (IOException e) {
                System.err.println("[ReadPostingListSkipBlocks] Exception during read");
                throw new RuntimeException(e);
            }
        }

        //Return the list
        return list;
    }

    /**
     * Returns true if the iteration has more elements.
     * (In other words, returns true if next would return an element rather than throwing an exception.)
     * @return true if the iteration has more elements.
     */
    public boolean hasNext() {
        return iterator.hasNext();
    }

    public Term getTermInfo() {
        return termInfo;
    }

    public int getFreq() {
        return freq;
    }

    public void setFreq(int freq) {
        this.freq = freq;
    }
}
