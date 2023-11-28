/*
package unipi.mircv.indexing;
import java.io.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.FileUtils;


public class App
{
    /*
    //metodi aggiuntivi
    private static boolean isMemoryAvailable(long threshold){

        //Subtract the free memory at the moment to the total memory allocated obtaining the memory used, then check
        //if the memory used is above the threshold
        return Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory() < threshold;
    }

    public static void main(String[] args) throws IOException {

            String path = "C:/Users/danny/Desktop/collection/collection.tar.gz";
            // String path = "C:/Users/kecco/Desktop/collection.tar.gz";


            File file = new File(path);

            //Try to open the collection provided
            try (FileInputStream fileInputStream = new FileInputStream(file) {
                 //RandomAccessFile documentIndexFile = new RandomAccessFile(DOCUMENT_INDEX_PATH, "rw")) {

                //Create an input stream for the tar archive
                TarArchiveInputStream tarInput = new TarArchiveInputStream(new GzipCompressorInputStream(fileInputStream));

                //Get the first file from the stream, that is only one
                TarArchiveEntry currentEntry = tarInput.getNextTarEntry();

                //If the file exist
                if(currentEntry != null) {

                    //Read the uncompressed tar file specifying UTF-8 as encoding
                    InputStreamReader inputStreamReader = new InputStreamReader(tarInput, StandardCharsets.UTF_8);

                    //Create a BufferedReader in order to access one line of the file at a time
                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                    //Variable to keep the current line read from the buffer
                    String line;

                    //Instantiate the inverted index builder for the current block
                    //InvertedIndexBuilder invertedIndexBuilder = new InvertedIndexBuilder();

                    //Counter to keep the number of documents read in total
                    int numberOfDocuments = 0;

                    //variable to keep track of the average length of the document
                    float avdl = 0;

                    //Counter to keep the number of documents read for the current block
                    int blockDocuments = 0;

                    //String to keep the current document processed
                    //ParsedDocument parsedDocument;

                    //Retrieve the time at the beginning of the computation
                    long begin = System.nanoTime();

                    //Retrieve the initial free memory
                    long initialMemory = Runtime.getRuntime().freeMemory();

                    //Retrieve the total memory allocated for the execution of the current runtime
                    long totalMemory = Runtime.getRuntime().totalMemory();

                    //Retrieve the memory used at the beginning of the computation
                    long beforeUsedMem=Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();

                    //Define the threshold of memory over which the index must be flushed to disk
                    long THRESHOLD = (long) (totalMemory * 0.8);

                    System.out.println("[INDEXER] Initial total memory allocated "+ totalMemory/(1024*1024)+"MB");
                    System.out.println("[INDEXER] Initial free memory "+ initialMemory/(1024*1024)+"MB");
                    System.out.println("[INDEXER] Initial memory used "+ beforeUsedMem/(1024*1024)+"MB");
                    System.out.println("[INDEXER] Memory threshold: " + THRESHOLD/(1024*1024)+"MB -> " + 0.8 * 100 + "%");
                    System.out.println("[INDEXER] Starting to fetch the documents...");

                    //Iterate over the lines
                    /*
                    while ((line = bufferedReader.readLine()) != null ) {

                        //Process the document using the stemming and stopwords removal
                        parsedDocument = Parser.processDocument(line, stopwordsRemovalAndStemming);

                        //If the parsing of the document was completed correctly, it'll be appended to the collection buffer
                        if (parsedDocument!= null && parsedDocument.getTerms().length != 0) {

                            //updating the average number of documents
                            avdl = avdl*(numberOfDocuments)/(numberOfDocuments + 1) + ((float) parsedDocument.getTerms().length)/(numberOfDocuments + 1);

                            //Increase the number of documents analyzed in total
                            numberOfDocuments++;

                            //Increase the number of documents analyzed in the current block
                            blockDocuments++;

                            //Set the docid of the current document
                            parsedDocument.setDocId(numberOfDocuments);

                            //System.out.println("[INDEXER] Doc: "+parsedDocument.docId + " read with " + parsedDocument.documentLength + "terms");
                            invertedIndexBuilder.insertDocument(parsedDocument);

                            //Insert the document index row in the document index file. It's the building of the document
                            // index. The document index will be read from file in the future, the important is to build it
                            // and store it inside a file.
                            DocumentIndexEntry docEntry = new DocumentIndexEntry(parsedDocument.getDocNo(), parsedDocument.getDocumentLength());
                            docEntry.writeToDisk(documentIndexFile, numberOfDocuments);

                     */

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

/*
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
                            }

                            } catch (IOException e) {
                            throw new RuntimeException(e);
                            }
                            }

                            }
                            }


                            }
*/