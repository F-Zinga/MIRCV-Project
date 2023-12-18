package unipi.mircv;

import unipi.mircv.DocParsed;
import unipi.mircv.Posting;
import unipi.mircv.Term;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Stream;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


/**
 * Represent a component that gives the methods to build the lexicon and the inverted index for each block.
 */
public class IndexBuilder {

    //The lexicon has a String as a key and an array of integers as value, the value is composed by:
    // value[0] -> TermId
    // value[1] -> offset in the posting list
    HashMap<String, Term> lexicon;
    HashMap<String, ArrayList<Posting>> invertedIndex;

    /**
     * Constructor of the class.
     * Instantiate the HashMap for the lexicon and the inverted index, used for the fast lookup that requires O(1);
     * Set the first termID to 1, the term id for each block carries also the information about the position of a term
     * in the inverted index.
     */
    public IndexBuilder() {
        lexicon = new HashMap<>();
        invertedIndex = new HashMap<>();
    }

    /**
     * Insert the document's tokens inside the lexicon and the inverted index, it's an implementation of SPIMI
     * @param docParsed Contains the id of the document, its length and the list of tokens
     */
    public void insertDocument(DocParsed docParsed) {

        //long begin = System.currentTimeMillis();
        //Generate a stream of String
        Stream.of(docParsed.getTerms())
                .forEach((term) -> {
                    //If the term is already present in the lexicon
                    if(lexicon.containsKey(term)){

                        //Retrieve the posting list of the term accessing the first element of the array of int that is
                        // the value of the termID in the lexicon
                        ArrayList<Posting> termPostingList = invertedIndex.get(term);

                        //Flag to set if the doc id's posting is present in the posting list of the term
                        boolean check = false;

                        //Iterate through the posting
                        for(Posting p : termPostingList){

                            //If the doc id is present, increment the frequency and terminate the loop
                            if(p.getDocID() == docParsed.getDocId()){

                                //Increment the frequency of the doc id
                                p.updateTermFrequency();

                                check = true;
                                break;
                            }
                        }

                        //If the posting of the document is not present in the posting list, it must be added
                        if(!check){

                            //Posting added to the posting list of the term
                            termPostingList.add(new Posting(docParsed.getDocId(), 1));
                        }
                    }
                    //If the term was not present in the lexicon
                    else{
                        //Insert a new element in the lexicon, in each block the currTermID corresponds to the id
                        // associated to the term, but also to the position in the inverted index!
                        // To access the posting list of that term we can just retrieve the currTermId and access the
                        // array of posting lists
                        lexicon.put(term, new Term());

                        //Insert a new posting list in the inverted index
                        ArrayList<Posting> postingsList = new ArrayList<>();
                        Posting posting = new Posting(docParsed.getDocId(), 1);
                        postingsList.add(posting);

                        //Insert the new posting list
                        invertedIndex.put(term, postingsList);

                    }
                });
        //System.out.println("[DEBUG: INSERT DOCUMENT] " + (System.currentTimeMillis() - begin) + "ms");
    }

    /**
     * Clear the instance of the lexicon, it must be used after the lexicon has been written in the disk.
     */
    private void clearLexicon(){
        lexicon.clear();
    }

    /**
     * Clear the instance of the inverted index, it must be used after the inverted index has been written in the disk.
     */
    private void clearInvertedIndex(){
        invertedIndex.clear();
    }

    /**
     * Clear the class instances in order to be used for a new block processing.
     */
    public void clear(){
        clearLexicon();
        clearInvertedIndex();

        //Call the garbage collector to thrash the data structures cleared above, if it is not done the memory will be
        // over the threshold until the gc will be called automatically, causing the writes of a block at every document
        // processed after the trespassing of the threshold.
        Runtime.getRuntime().gc();
    }

    /**
     * Sort the lexicon with complexity O(nlog(n)) where n is the # of elements in the lexicon.
     */
    public void sortLexicon(){

        //To not double the memory instantiating a new data structure we've decided to use the following sorting
        lexicon = lexicon.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1, LinkedHashMap::new)); //LinkedHashMap to keep O(1) time complexity

    }
    /**
     * Sort the inverted index with complexity O(nlog(n)) where n is the # of elements in the inverted index.
     */
    public void sortInvertedIndex(){

        invertedIndex = invertedIndex.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1, LinkedHashMap::new));

    }

    /**
     * Writes the current lexicon into a file
     * @param outputPath path of the file that will contain the block's lexicon
     */
    public void writeLexiconToFile(String outputPath){

        try (RandomAccessFile randomAccessFile = new RandomAccessFile(outputPath, "rw")){

            //Write each lexicon entry in the lexicon in the output file
            lexicon.forEach( (id, term) -> term.writeToFile(randomAccessFile, id, term));

        }catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Writes the current inverted index in the disk, the inverted index is written in two different files:
     * The file containing the document ids of each posting list
     * The file containing the frequencies of the terms in the documents
     * Then update the information in the lexicon
     * @param outputPathDocIds path of the file that will contain the document ids
     * @param outputPathFrequencies path of the file that will contain the frequencies
     */
    public void writeInvertedIndexToFile(String outputPathDocIds, String outputPathFrequencies){

        //Create resources with try-catch with resources
        try (RandomAccessFile docIdBlock = new RandomAccessFile(outputPathDocIds, "rw");
             RandomAccessFile frequencyBlock = new RandomAccessFile(outputPathFrequencies, "rw"))
        {

            AtomicInteger atomicOffsetDocId = new AtomicInteger(0);
            AtomicInteger atomicOffsetFrequency = new AtomicInteger(0);


            //for each element of the inverted index
            invertedIndex.forEach((term, postingList) -> {

                //Set the current offsets to be written in the lexicon
                int offsetDocId = atomicOffsetDocId.get();
                int offsetFrequency = atomicOffsetFrequency.get();

                postingList.forEach(posting -> {
                    //Create the buffers for each element to be written
                    byte[] postingDocId = ByteBuffer.allocate(8).putLong(posting.getDocID()).array();
                    byte[] postingFreq = ByteBuffer.allocate(4).putInt(posting.getTermFrequency()).array();

                    try {
                        //Append each element to the file, each one adds 4 bytes to the file
                        docIdBlock.write(postingDocId);
                        frequencyBlock.write(postingFreq);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    //Increment the current offset
                    atomicOffsetDocId.addAndGet(8);
                    atomicOffsetFrequency.addAndGet(4);
                });

                //Set the docId offset, the frequency offset, the posting list length of the term in the lexicon
                lexicon.get(term).setOffsetDocId(offsetDocId);
                lexicon.get(term).setOffsetFrequency(offsetFrequency);
                lexicon.get(term).setPostingListLength(postingList.size());
            });
        }catch (IOException e) {
            System.err.println("Exception during file creation of block");
            throw new RuntimeException(e);
        }
    }

    public HashMap<String, Term> getLexicon() {
        return lexicon;
    }

    public HashMap<String, ArrayList<Posting>> getInvertedIndex() {
        return invertedIndex;
    }

}