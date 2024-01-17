package unipi.mircv;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class App {

    /**
     * Main method to execute the application.
     * @param args Command-line arguments.
     * @throws IOException If an I/O error occurs.
     */
    public static void main(String[] args) throws IOException {

        // Instantiate the Parser class
        Parser p = new Parser();
        boolean stopStemming = true;

        // Specify the path to the input file
        String path = "resources/utility/textsample.tsv";

        // Create a File object for the specified path
        File file = new File(path);
        //Try to open the collection
        try (FileInputStream fileInputStream = new FileInputStream(file);
             //Create an input stream for the tar archive
             TarArchiveInputStream tarInput = new TarArchiveInputStream(new GzipCompressorInputStream(fileInputStream))) {

            // Create a RandomAccessFile for the document index
            RandomAccessFile documentIndexFile = new RandomAccessFile(Parameters.DOCID_PATH, "rw");


            //Get the first file from the stream (is only one)
            TarArchiveEntry currentEntry = tarInput.getNextTarEntry();

            //If the file exist
            if(currentEntry != null) {

                //Read the uncompressed tar file specifying UTF-8 as encoding
                InputStreamReader inputStreamReader = new InputStreamReader(tarInput, StandardCharsets.UTF_8);

                //Create a BufferedReader to access one line of the file at a time
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                //Variable to store the current line read from the buffer
                String line;


                //Counter storing the total number of documents read
                int numberOfDocuments = 0;

                //variable storing the average length of the document
                float avdl = 0;

                //Counter storing the number of documents read for the current block
                int blockDocuments = 0;


                //Record the start time for performance measurement
                long begin = System.nanoTime();

                //Record the initial free memory
                long initialMemory = Runtime.getRuntime().freeMemory();

                //Record the total memory allocated for the execution of the current runtime
                long totalMemory = Runtime.getRuntime().totalMemory();

                //Record the memory used at the beginning
                long beforeUsedMem=Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();

                //Define the threshold of memory over which the index must be flushed to disk
                long THRESHOLD = (long) (totalMemory * 0.8);

                // Output initial memory information
                System.out.println("[INDEXER] Initial total memory allocated "+ totalMemory/(1024*1024)+"MB");
                System.out.println("[INDEXER] Initial free memory "+ initialMemory/(1024*1024)+"MB");
                System.out.println("[INDEXER] Initial memory used "+ beforeUsedMem/(1024*1024)+"MB");
                System.out.println("[INDEXER] Memory threshold: " + THRESHOLD/(1024*1024)+"MB -> " + 0.8 * 100 + "%");
                System.out.println("[INDEXER] Starting to fetch the documents...");


                //Iterate over the lines
                while ((line = bufferedReader.readLine()) != null ) {

                    //Use the stemming and stopwords removal
                    DocParsed docParsed = Parser.processDocument(line,stopStemming);


                    //If the parsing of the document was completed correctly, appended it to the collection buffer
                    if (docParsed != null && docParsed.getTerms().length != 0) {

                        //updating the average number of documents
                        avdl = avdl * (numberOfDocuments) / (numberOfDocuments + 1) + ((float) docParsed.getTerms().length) / (numberOfDocuments + 1);

                        //Increase the total number of documents
                        numberOfDocuments++;

                        //Increase the number of documents in the current block
                        blockDocuments++;

                        //Set the docid of the current document
                        docParsed.setDocId(numberOfDocuments);


                        // Create a document index entry for the current document, which will be written to the document index file.
                        // The document index will be read from the file in the future; we build it
                        // and store it inside a file.
                        DocInfo docInfo = new DocInfo(docParsed.getDocNo(), docParsed.getDocumentLength());
                        docInfo.writeFile(documentIndexFile, numberOfDocuments);
                    }
                }


                //Check if the memory used is above the threshold defined
                        /*
                            if(!isMemoryAvailable(THRESHOLD)){
                                System.out.println("[INDEXER] Flushing " + blockDocuments + " documents to disk...");

                                //Sorting the lexicon and the inverted index
                                invertedIndexBuilder.sortLexicon();
                                invertedIndexBuilder.sortInvertedIndex();

                                //Write the inverted index and the lexicon in the file
                                writeToFiles(invertedIndexBuilder, blockNumber);

                                System.out.println("[INDEXER] Block "+blockNumber+" written to disk!");

                                //Handle the blocks' information
                                blockNumber++;
                                blockDocuments = 0;

                                //Clear the inverted index data structure and call the garbage collector
                                invertedIndexBuilder.clear();
                            }
                         */


                //Print checkpoint information
                if(numberOfDocuments%50000 == 0){
                    System.out.println("[INDEXER] " + numberOfDocuments+ " processed");
                    System.out.println("[INDEXER] Processing time: " + (System.nanoTime() - begin)/1000000000+ "s");
                }
            }
            /*
            if(blockDocuments > 0 ){

                System.out.println("[INDEXER] Last block reached");
                System.out.println("[INDEXER] Flushing " + blockDocuments + " documents to disk...");

                //Sort the lexicon and the inverted index
                invertedIndexBuilder.sortLexicon();
                invertedIndexBuilder.sortInvertedIndex();

                //Write the inverted index and the lexicon in the file
                writeToFiles(invertedIndexBuilder, blockNumber);

                System.out.println("[INDEXER] Block "+blockNumber+" written to disk");

                //Write the blocks statistics
                Statistics.writeStatistics(blockNumber, numberOfDocuments, avdl);

                System.out.println("[INDEXER] Statistics of the blocks written to disk");

                }else{
                //Write the blocks statistics
                Statistics.writeStatistics(blockNumber-1, numberOfDocuments, avdl);

                System.out.println("[INDEXER] Statistics of the blocks written to disk");
                }

                System.out.println("[INDEXER] Total processing time: " + (System.nanoTime() - begin)/1000000000+ "s");
                */}
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}