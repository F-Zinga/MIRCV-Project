package unipi.mircv;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

import static unipi.mircv.Parameters.*;

/**
 * This class handles the primary operations during the indexing phase.
*/

public class MainIndexing {

    public int docId = 0;
    public int blockCounter = 0;
    public IndexBuilder indexBuilder;
    public Lexicon lexicon;
    public DocIndex docIndex;
    public Statistics statistics;
    public String encodingType;
    public int postingListLength;

    public Parser parser;

    /**
     *  Constructor initializes various components and sets default posting list length
      */
    public MainIndexing(){
        this.indexBuilder = new IndexBuilder();
        this.lexicon = new Lexicon();
        this.docIndex = new DocIndex();
        this.statistics = new Statistics(0, 0, 0, 0);

    }

    public void setEncodingType(String encodingType) {
        this.encodingType = encodingType;
    }

    /**
     *  Method that processes a compressed document collection, performing preprocessing and analysis on each document.
     */
    public void processCollection(String file, String type,boolean stopWordsStemming){
        // Set the encoding type for the document collection
        setEncodingType(type);

        try {
            // Open the compressed file
            FileInputStream input = new FileInputStream(file);

            // Create a zip input stream to read entries from the compressed file
            TarArchiveInputStream tarinput = new TarArchiveInputStream(input);

            // Read the first entry in the compressed  file
            TarArchiveEntry entry = tarinput.getNextEntry();

            // Create a reader to read uncompressed data with UTF-8 encoding
            InputStreamReader reader = new InputStreamReader(tarinput, "UTF-8");

            // Create a buffered reader for efficient reading
            BufferedReader bufferedReader=new BufferedReader(reader);

            // Process each line in the compressed file, tokenizing documents in the format [doc_id]\t[token1 token2 ... tokenN]\n
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                // Split the line into columns (docNo \t document)
                String[] columns = line.split("\t",2);

                int docNo;
                try{
                    // Parse the document number as an integer
                    docNo = Integer.parseInt(columns[0]); }catch (NumberFormatException e){continue;} // Skip processing if the document number is not a valid integer
                if(columns[1].isEmpty()) continue; // Skip processing if the document content is empty

                // Preprocess the document and obtain the processed document
                String document = parser.processDocument(columns[1],stopWordsStemming); //Get document

                // Perform document analysis and create the index
                createIndex(document, docNo);
            }


            // Close the input stream
            bufferedReader.close();
            input.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // After processing the documents, save the block currently in main memory
        if(encodingType.equals("text")) {
            writeTextBlock(lexicon, lexicon.sortLexicon(), docIndex.sortDocIndex());
        }
        else{
            writeBytesBlock(lexicon, lexicon.sortLexicon(), docIndex.sortDocIndex());
        }

        // Reset data structures and counters for the next block
        indexBuilder.setIndexBuilder(new HashMap<>());
        lexicon.setLexicon(new HashMap<>());
        docIndex.setDocIndex(new HashMap<>());
        blockCounter += 1;

        System.gc();  // Trigger garbage collection to free up memory

        Merger merger = new Merger();  // Create a Merger instance for merging blocks

        // Perform block merging based on encoding type
        if(encodingType.equals("text"))
            merger.mergeTextBlocks(blockCounter,encodingType,statistics);
        else
            merger.mergeByteBlocks(blockCounter,encodingType,statistics);

        saveStatistics(); // Save the final statistics
    }

    /**
     * Method that takes a document and its corresponding document number, processes the document, and generates the index.
     * @param document document to process
     * @param docNo document number
     */
    public void createIndex(String document, int docNo){

        // Calculate memory usage statistics
        float totalMemory = Runtime.getRuntime().totalMemory();
        float memoryUsage = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        float percentageMemory = (memoryUsage / totalMemory) * 100;

        // Check if available memory is below a threshold; if so, save the current block to disk
        if (percentageMemory >= 75 ){

            // Write the current block to disk based on the encoding type
            if(encodingType.equals("text")) {
                writeTextBlock(lexicon, lexicon.sortLexicon(), docIndex.sortDocIndex());
            }
            else{
                writeBytesBlock(lexicon, lexicon.sortLexicon(), docIndex.sortDocIndex());
            }

            // Reset data structures and counters for the next block
            lexicon.setLexicon(new HashMap<>());
            indexBuilder.setIndexBuilder(new HashMap<>());
            docIndex.setDocIndex(new HashMap<>());
            blockCounter += 1;

            System.gc(); // Trigger garbage collection to free up memory
        }

        String[] terms = document.split(" "); // Split the document into terms
        // Count occurrences of each term in the document
        HashMap<String, Integer> counter = new HashMap<>();
        for (String term : terms){
            counter.put(term, counter.containsKey(term) ? counter.get(term) + 1 : 1);
        }

        //Update the index information for each term.
        for (String term : counter.keySet()) {
            lexicon.addInformation(term, 0, 0, 0,
                    0, 0, counter.get(term));
            indexBuilder.addPosting(term, docId, counter.get(term));
            statistics.setPostings(statistics.getPostings() + 1);
        }

        // Update document index information and statistics
        docIndex.addDocument(docId, docNo, terms.length);
        docId += 1;
        statistics.setnDocs(statistics.getNDocs() + 1);
        statistics.setAvdl(statistics.getAvdl() + terms.length);
    }

    /**
     * Method that writes the current in-memory block to disk in compressed format for byte-encoded data.
     * @param lexicon object lexicon
     * @param sortedTerms list of ordered terms
     * @param sortedDocIds list of sorted docIds
     */
    public void writeBytesBlock(Lexicon lexicon, ArrayList<String> sortedTerms, ArrayList<Integer> sortedDocIds){

        // Create writers for lexicon, document IDs, frequencies, and document index
        TextWriter lexiconWriter = new TextWriter(LEXICON_BLOCK_PATH + blockCounter + ".txt");
        Compressor compressor = new Compressor();
        ByteWriter docIDWriter = new ByteWriter(DOCIDS_BLOCK_PATH + blockCounter + ".dat", compressor);
        ByteWriter freqWriter = new ByteWriter(FREQ_BLOCK_PATH + blockCounter + ".dat", compressor);
        ByteWriter docIndexWriter = new ByteWriter(DOCUMENT_INDEX_BLOCK_PATH + blockCounter + ".dat", compressor);

        //saves the document index.
        for(Integer docId : sortedDocIds){
            docIndexWriter.write(docId);
            docIndexWriter.write(docIndex.docIndex.get(docId).getDocNo());
            docIndexWriter.write(docIndex.docIndex.get(docId).getDocLen());
        }

        // Save the lexicon, document IDs, and frequencies to their respective files
        for (String term : sortedTerms){
            lexicon.getLexicon().get(term).setPostingListLength(indexBuilder.getIndexBuilder().get(term).size());
            lexiconWriter.write(term + " " + lexicon.getLexicon().get(term).toString() + "\n");
            for (Posting posting : indexBuilder.getIndexBuilder().get(term)){
                docIDWriter.write(posting.getDocID());
                freqWriter.write(posting.getTermFrequency());
            }
        }

        // Close the writers
        docIDWriter.close();
        freqWriter.close();
        lexiconWriter.close();
        docIndexWriter.close();

        System.out.println("*** Blocks successfully written to disk. ***");
    }


    /**
     * // Method that writes the current in-memory block to disk in text format.
     * @param lexicon object lexicon
     * @param sortedTerms list of sorted terms
     * @param sortedDocIds list of sorted docIds
     */
    public void writeTextBlock(Lexicon lexicon, ArrayList<String> sortedTerms, ArrayList<Integer> sortedDocIds){

        // Create writers for lexicon, document IDs, frequencies, and document index
        TextWriter lexiconWriter = new TextWriter(LEXICON_BLOCK_PATH + blockCounter + ".txt");
        TextWriter docIDWriter = new TextWriter(DOCIDS_BLOCK_PATH + blockCounter + ".txt");
        TextWriter freqWriter = new TextWriter(FREQ_BLOCK_PATH + blockCounter + ".txt");
        TextWriter docIndexWriter = new TextWriter(DOCUMENT_INDEX_BLOCK_PATH + blockCounter + ".txt");

        //saves the document index.
        for(Integer docId : sortedDocIds){
            docIndexWriter.write(docId);
            docIndexWriter.write(docIndex.docIndex.get(docId).getDocNo());
            docIndexWriter.write(docIndex.docIndex.get(docId).getDocLen());
        }

        // Save the lexicon, document IDs, and frequencies to their respective files
        for (String term : sortedTerms){
            lexicon.getLexicon().get(term).setPostingListLength(indexBuilder.getIndexBuilder().get(term).size());
            lexiconWriter.write(term + " " + lexicon.getLexicon().get(term).toString() + "\n");
            for (Posting posting : indexBuilder.getIndexBuilder().get(term)){
                docIDWriter.write(posting.getDocID());
                freqWriter.write(posting.getTermFrequency());
            }
        }

        // Close the writers
        docIDWriter.close();
        freqWriter.close();
        lexiconWriter.close();
        docIndexWriter.close();

        System.out.println("*** Blocks successfully written to disk. ***");
    }


    /**
     * Method that saves statistics about the processed document collection.
     */
    public void saveStatistics(){
        // Calculate and set the average document length
        statistics.setAvdl(statistics.getAvdl() / statistics.getNDocs());
        try{
            // Write collection statistics to a file
            FileWriter writer = new FileWriter(STATISTICS_PATH);
            writer.write(statistics.getNDocs() + " "
                    + statistics.getAvdl() + " " + statistics.getPostings());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args){

        System.out.println("*** MAIN INDEXING ***");

        // Retrieve path for the input file, set the encoding type and stemming&stopwords removal
        String file = COLLECTION_PATH;
        String type = "bytes";  //bytes or text
        Boolean stopWordsStemming = true;

        // Check if the provided encoding type is valid
        if(!type.equals("text") && !type.equals("bytes")){
            System.out.println("*** Try again: the encoding type is wrong. ***");
        }
        else{
            MainIndexing index = new MainIndexing();
            long start = System.currentTimeMillis(); // Record the start time for performance measurement
            index.processCollection(file, type, stopWordsStemming); // Process the document collection with the specified parameters
            long end = System.currentTimeMillis(); // Record the end time and calculate the elapsed time
            System.out.println("*** Time in milliseconds: " + (end-start) + " ***");
        }
    }
}