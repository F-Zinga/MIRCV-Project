package unipi.mircv;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class MainIndexing {

    // Counter to track the number of blocks read
    static int blockNumber = 1;


    /**
     * Build an inverted index for the collection in the specified path. Utilizes the SPIMI algorithm to create different
     * blocks, each containing a partial inverted index and its respective lexicon.
     *
     * @param path            Path of the archive containing the collection (tar.gz archive)
     * @param stopStemming    true to apply stopwords removal and stemming, false otherwise
     */
    private static void parseCollection(String path, Boolean stopStemming, Boolean debug) {

        //Path of the collection
        File file = new File(path);

        //Try to open the collection
        try (
                FileInputStream fileInputStream = new FileInputStream(file);
             RandomAccessFile documentIndexFile = new RandomAccessFile(Parameters.DOCUMENT_INDEX_PATH, "rw");
            TarArchiveInputStream tarInput = new TarArchiveInputStream(new GzipCompressorInputStream(fileInputStream))
        ){



            //Get the first file from the stream (is only one)
            TarArchiveEntry currentEntry = tarInput.getNextTarEntry();

            //If the file exist
            if(currentEntry != null) {

                //Read the uncompressed tar file specifying UTF-8 as encoding
                InputStreamReader inputStreamReader = new InputStreamReader(tarInput, StandardCharsets.UTF_8);

                //Create a BufferedReader to access one line of the file at a time
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                //Variable to keep the current line from the buffer
                String line;

                //Instantiate the inverted index builder for the current block
                IndexBuilder indexBuilder = new IndexBuilder();

                //Counter storing the total number of documents read
                int numberOfDocuments = 0;

                //variable storing the average length of the document
                float avdl = 0;

                //Counter storing the number of documents read for the current block
                int blockDocuments = 0;

                //String storing the current document processed
                DocParsed parsedDocument;

                //Record the start time for performance measurement
                long begin = System.nanoTime();

                //Record the initial free memory
                long initialMemory = Runtime.getRuntime().freeMemory();

                //Record the total memory allocated for the execution of the current runtime
                long totalMemory = Runtime.getRuntime().totalMemory();

                //Record the memory used at the beginning of the computation
                long beforeUsedMem=Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();

                //Define the threshold of memory over which the index must be flushed to disk
                long THRESHOLD = (long) (totalMemory * Parameters.PERCENTAGE);

                // Output initial memory information
                System.out.println("[INDEXER] Initial total memory allocated "+ totalMemory/(1024*1024)+"MB");
                System.out.println("[INDEXER] Initial free memory "+ initialMemory/(1024*1024)+"MB");
                System.out.println("[INDEXER] Initial memory used "+ beforeUsedMem/(1024*1024)+"MB");
                System.out.println("[INDEXER] Memory threshold: " + THRESHOLD/(1024*1024)+"MB -> " + Parameters.PERCENTAGE * 100 + "%");
                System.out.println("[INDEXER] Starting to fetch the documents...");

                //Iterate over the lines
                while ((line = bufferedReader.readLine()) != null ) {

                    //Use the stemming and stopwords removal
                    parsedDocument = Parser.processDocument(line,stopStemming);

                    //If the parsing of the document was completed correctly, appended it to the collection buffer
                    if (parsedDocument!= null && parsedDocument.getTerms().length != 0) {

                        //update the average number of documents
                        avdl = avdl*(numberOfDocuments)/(numberOfDocuments + 1) + ((float) parsedDocument.getTerms().length)/(numberOfDocuments + 1);

                        //Increase the total number of documents
                        numberOfDocuments++;

                        //Increase the number of documents in the current block
                        blockDocuments++;

                        //Set the docid of the current document
                        parsedDocument.setDocId(numberOfDocuments);

                        indexBuilder.insertDocument(parsedDocument);

                        // Create a document index entry for the current document, which will be written to the document index file.
                        // The document index will be read from the file in the future; we build it
                        // and store it inside a file.
                        DocInfo docEntry = new DocInfo(parsedDocument.getDocNo(), parsedDocument.getDocumentLength());
                        docEntry.writeFile(documentIndexFile, numberOfDocuments);

                        //Check if the memory used is above the threshold
                        if(!isMemoryAvailable(THRESHOLD)){
                            System.out.println("[INDEXER] Flushing " + blockDocuments + " documents to disk...");

                            //Sort the lexicon and the inverted index
                            indexBuilder.sortLexicon();
                            indexBuilder.sortInvertedIndex();

                            //Write the inverted index and the lexicon in the file
                            writeToFiles(indexBuilder, blockNumber);

                            System.out.println("[INDEXER] Block "+blockNumber+" written to disk!");

                            //Blocks' information
                            blockNumber++;
                            blockDocuments = 0;

                            //Clear the inverted index data structure and call the garbage collector
                            indexBuilder.clear();
                        }

                        //Print checkpoint information
                        if(numberOfDocuments%50000 == 0){
                            System.out.println("[INDEXER] " + numberOfDocuments+ " processed");
                            System.out.println("[INDEXER] Processing time: " + (System.nanoTime() - begin)/1000000000+ "s");
                            if(debug) {
                                System.out.println("[DEBUG] Document index entry: " + docEntry);
                                System.out.println("[DEBUG] Memory used: " + getMemoryUsed()*100 + "%");
                            }
                        }
                    }
                }
                if(blockDocuments > 0 ){

                    System.out.println("[INDEXER] Last block reached");
                    System.out.println("[INDEXER] Flushing " + blockDocuments + " documents to disk...");

                    //Sort the lexicon and the inverted index
                    indexBuilder.sortLexicon();
                    indexBuilder.sortInvertedIndex();

                    //Write the inverted index and the lexicon to the file
                    writeToFiles(indexBuilder, blockNumber);

                    System.out.println("[INDEXER] Block "+blockNumber+" written to disk");

                    //Write the blocks statistics
                    Statistics.writeStats(blockNumber, numberOfDocuments, avdl, (System.nanoTime() - begin)/1000000000);

                    System.out.println("[INDEXER] Statistics of the blocks written to disk");

                }else{
                    //Write the blocks statistics
                    Statistics.writeStats(blockNumber-1, numberOfDocuments, avdl, (System.nanoTime() - begin)/1000000000);

                    System.out.println("[INDEXER] Statistics of the blocks written to disk");
                }

                System.out.println("[INDEXER] Total processing time: " + (System.nanoTime() - begin)/1000000000+ "s");
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Clears the contents of the "Files" folder.
     */
    private static void clearFiles(){
        try {
            FileUtils.cleanDirectory(new File(Parameters.FILES_PATH));
        } catch (IOException e) {
            System.out.println("Error deleting files inside Files folder");
            throw new RuntimeException(e);
        }
    }

    /**
     * Writes the inverted index and the lexicon blocks. The block's number is passed as a parameter.
     * At the end, it clears the data structures and calls the garbage collector.
     *
     * @param indexBuilder Inverted index builder object containing the inverted index and the lexicon.
     * @param blockNumber Number of the block that will be written.
     */

    private static void writeToFiles(IndexBuilder indexBuilder, int blockNumber){

        //Write the inverted index's files into the block's files
        indexBuilder.writeInvertedIndexToFile(
                Parameters.DOCID_BLOCK_PATH +blockNumber+".txt",
                Parameters.FREQ_BLOCK_PATH +blockNumber+".txt");

        //Write the block's lexicon into the given file
        indexBuilder.writeLexiconToFile(Parameters.LEXICON_BLOCK_PATH+blockNumber+".txt");

        System.out.println("Block "+blockNumber+" written");

        //Clear the inverted index and lexicon data structure and call the garbage collector
        indexBuilder.clear();
    }

    /**
     * Return true if the memory used is under the threshold (there is enough free memory to continue)
     * otherwise it will return false.
     * @param threshold Memory threshold in byte.
     */
    private static boolean isMemoryAvailable(long threshold){

        //Obtain the memory used, subtracting the free memory at the moment to the total memory allocated, then check
        //if the memory used is above the threshold
        return Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory() < threshold;
    }

    /**
     * @return the amount of memory used with respect to the memory available to the process
     */
    private static long getMemoryUsed(){
        Runtime rt = Runtime.getRuntime();
        long total_mem = rt.totalMemory();
        long free_mem = rt.freeMemory();
        return  (total_mem - free_mem)/total_mem;
    }

    public static void main(String[] args){

        boolean stemmingAndStopwordsRemoval = false;
        boolean compressed = true;
        boolean debug = false;

        // Parse command line arguments
        if(args.length >= 1){
            switch (args[0]) {
                case "-s":
                    stemmingAndStopwordsRemoval = true;
                    break;
                case "-c":
                    compressed = true;
                    break;
                case "-sc":
                    stemmingAndStopwordsRemoval = true;
                    compressed = true;
                    break;
                case "-d":
                    debug = true;
                    break;
                default:
                    System.err.println("Invalid command\n"+Parameters.ARGS_ERROR);
                    return;
            }
        }

        // Additional configuration based on command line arguments
        if(args.length == 2){
            if(args[1].equals("-d") && !args[0].equals("-d")){
                debug = true;
            }
            else{
                System.err.println("Invalid command\n"+Parameters.ARGS_ERROR);
            }
        }
        else if(args.length > 2){
            System.err.println("Wrong number of arguments\n"+Parameters.ARGS_ERROR);
            return;
        }

        // Display the configuration information
        System.out.println("[INDEXER] Configuration\n" +
                "\tStemming and stopwords removal: " + stemmingAndStopwordsRemoval+"\n" +
                "\tCompression: " + compressed + "\n" +
                "\tDebug: " + debug);

        // Clear existing files in the "Files" folder
        clearFiles();

        //Create the inverted index including the document index file and statistics file
        parseCollection(Parameters.COLLECTION_PATH, stemmingAndStopwordsRemoval, debug);

        //Merge the blocks to obtain the inverted index, compressed indicates if the compression is enabled
        Merger.merge(compressed, debug);

        // Save the execution configuration
        System.out.println("[INDEXER] Saving execution configuration...");
        Settings.saveConfiguration(stemmingAndStopwordsRemoval, compressed, debug);

        System.out.println("[INDEXER] Configuration saved");

    }


}
