package unipi.mircv;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Represents a component responsible for building the lexicon and inverted index for each block.
 */
public class IndexBuilder {

    //The lexicon has a String as a key and an array of integers as value, the value is composed by:
    // value[0] -> TermId
    // value[1] -> offset in the posting list

    // The lexicon maps a term (String) to a corresponding Term object.
    HashMap<String, Term> lexicon;
    // The inverted index maps a term to a list of Postings (document IDs and frequencies).
    HashMap<String, ArrayList<Posting>> invertedIndex;

    /**
     * Constructor for the IndexBuilder the class.
     * Initializes the lexicon and inverted index HashMaps for efficient term lookup (O(1)).
     * Sets the initial term ID to 1, where the term ID also represents the position of the term
     * in the inverted index for each block.
     */

    public IndexBuilder() {
        lexicon = new HashMap<>();
        invertedIndex = new HashMap<>();
    }

    /**
     * Insert the document's tokens inside the lexicon and the inverted index (SPIMI)
     * @param docParsed Contains the doc id, the doc length and the list of tokens
     */
    public void insertDocument(DocParsed docParsed) {

        //long begin = System.currentTimeMillis();
        //Generate a stream of String
        Stream.of(docParsed.getTerms())
                .forEach((term) -> {
                    //If the term is already present in the lexicon
                    if(lexicon.containsKey(term)){

                        //Retrieve the posting list of the term accessing the first element of the array
                        // (the value of the termID in the lexicon)
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

                        //If the posting of the document is not present in the posting list, add it
                        if(!check){

                            //Posting added to the posting list of the term
                            termPostingList.add(new Posting(docParsed.getDocId(), 1));
                        }
                    }
                    //If the term was not present in the lexicon
                    else{
                        //Insert a new element in the lexicon, in each block the currTermID corresponds to the id
                        // associated to the term, but also to the position in the inverted index
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
    }

    /**
     * Clear the lexicon instance, after the lexicon has been written in the disk.
     */
    private void clearLexicon(){
        lexicon.clear();
    }

    /**
     * Clear the inverted index istance, after the inverted index has been written in the disk.
     */
    private void clearInvertedIndex(){
        invertedIndex.clear();
    }

    /**
     * Clear the class instances to use it for a new block processing.
     */
    public void clear(){
        clearLexicon();
        clearInvertedIndex();

        // Calls the garbage collector to free memory occupied by cleared data structures; if it is not done
        // the memory will be over the threshold until the gc will be called automatically.

        Runtime.getRuntime().gc();
    }

    /**
     * Sort the lexicon with O(nlog(n)) where n is the number of elements in the lexicon.
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
     * Sort the inverted index with O(nlog(n)) where n is the number of elements in the inverted index.
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
     * Writes the current inverted index to disk. The inverted index is written to two separate files:
     * One containing the document IDs of each posting list,
     * and the other containing the frequencies of the terms in the documents.
     * Updates the information in the lexicon accordingly.
     *
     * @param outputPathDocIds      Path of the file that will contain the document IDs.
     * @param outputPathFrequencies Path of the file that will contain the frequencies.
     */
    public void writeInvertedIndexToFile(String outputPathDocIds, String outputPathFrequencies){

        //Create resources with try-catch
        try (RandomAccessFile docIdBlock = new RandomAccessFile(outputPathDocIds, "rw");
             RandomAccessFile frequencyBlock = new RandomAccessFile(outputPathFrequencies, "rw"))
        {

            AtomicInteger atomicOffsetDocId = new AtomicInteger(0);
            AtomicInteger atomicOffsetFrequency = new AtomicInteger(0);


            //for each element of the inverted index
            invertedIndex.forEach((term, postingList) -> {

                //Set the current offsets in the lexicon
                int offsetDocId = atomicOffsetDocId.get();
                int offsetFrequency = atomicOffsetFrequency.get();

                postingList.forEach(posting -> {
                    //Create the buffers for each element
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
            System.err.println(" *** Exception during creation of blocks ***");
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