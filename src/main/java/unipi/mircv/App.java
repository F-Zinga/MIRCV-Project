package unipi.mircv;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import unipi.mircv.indexing.DocInfo;
import unipi.mircv.indexing.DocParsed;
import unipi.mircv.indexing.Parser;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class App {

    public static void main(String[] args) throws IOException {
        Parser p = new Parser();
        //String[] output = p.processDocument("ciao come stai!!! \t tonellotto is a good prof.!!! ?");
        //System.out.println(output[1].toString());



        // String path = "C:/Users/danny/Desktop/collection.tar.gz";
        String path = "resources/utility/textsample.tsv";

        File file = new File(path);
        //Try to open the collection provided
        try (FileInputStream fileInputStream = new FileInputStream(file);
             //Create an input stream for the tar archive
             TarArchiveInputStream tarInput = new TarArchiveInputStream(new GzipCompressorInputStream(fileInputStream))) {

            //RandomAccessFile documentIndexFile = new RandomAccessFile(DOCUMENT_INDEX_PATH, "rw")) {


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
                    //DocParsed DocParsed;

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
                        line = bufferedReader.readLine();
                        System.out.println("[EXAMPLE] First line of first document before parsing: " + line);
                        String[] output = Parser.processDocument(line);
                        System.out.println("[EXAMPLE] First word of first document after parsing: " + output[0].toString());
                        */

                        while ((line = bufferedReader.readLine()) != null ) {

                            //Process the document using the stemming and stopwords removal
                            DocParsed docParsed = Parser.processDocument(line);


                            //If the parsing of the document was completed correctly, it'll be appended to the collection buffer
                            if (docParsed != null && docParsed.getTerms().length != 0) {

                                //updating the average number of documents
                                avdl = avdl * (numberOfDocuments) / (numberOfDocuments + 1) + ((float) docParsed.getTerms().length) / (numberOfDocuments + 1);

                                //Increase the number of documents analyzed in total
                                numberOfDocuments++;

                                //Increase the number of documents analyzed in the current block
                                blockDocuments++;

                                //Set the docid of the current document
                                docParsed.setDocId(numberOfDocuments);

                                //System.out.println("[INDEXER] Doc: "+DocParsed.docId + " read with " + DocParsed.documentLength + "terms");
                                //invertedIndexBuilder.insertDocument(docParsed);

                                //Insert the document index row in the document index file. It's the building of the document
                                // index. The document index will be read from file in the future, the important is to build it
                                // and store it inside a file.
                                DocInfo docInfo = new DocInfo(docParsed.getDocNo(), docParsed.getDocumentLength());
                                //docInfo.writeToDisk(documentIndexFile, numberOfDocuments);
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
                /*if(debug) {
                    System.out.println("[DEBUG] Document index entry: " + docEntry);
                    System.out.println("[DEBUG] Memory used: " + getMemoryUsed()*100 + "%");
                }*/
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
