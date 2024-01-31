package unipi.mircv;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

//class that performs the main operations during the indexing phase.
public class MainIndexing {

    public int docId = 0;
    public int blockCounter = 0;
    public IndexBuilder indexBuilder;
    public Lexicon lexicon;
    public DocIndex docIndex;
    public Statistics statistics;
    public String encodingType;

    public Parser parser;

    public MainIndexing(){
        this.indexBuilder = new IndexBuilder();
        this.lexicon = new Lexicon();
        this.docIndex = new DocIndex();
        this.statistics = new Statistics(0, 0, 0, 0);

    }

    public int getDocId() {
        return docId;
    }

    public void setDocId(int docId) {
        this.docId = docId;
    }

    public int getBlockCounter() {
        return blockCounter;
    }

    public void setBlockCounter(int blockCounter) {
        this.blockCounter = blockCounter;
    }

    public IndexBuilder getIndexBuilder() {
        return indexBuilder;
    }

    public void setInvertedIndex(IndexBuilder indexBuilder) {
        this.indexBuilder = indexBuilder;
    }

    public Lexicon getLexicon() {
        return lexicon;
    }

    public void setLexicon(Lexicon lexicon) {
        this.lexicon = lexicon;
    }

    public DocIndex getDocIndex() {
        return docIndex;
    }

    public void setDocIndex(DocIndex docIndex) {
        this.docIndex = docIndex;
    }
    public Statistics getStatistics() {
        return statistics;
    }

    public void setStatistics(Statistics Statistics) {
        this.statistics = statistics;
    }

    public String getEncodingType() {
        return encodingType;
    }

    public void setEncodingType(String encodingType) {
        this.encodingType = encodingType;
    }

    //function that taken the compressed document collection, preprocess and elaborate every document.
    public void processCollection(String file, String type,boolean stopWordsStemming){
        setEncodingType(type);

        try {
            // Open the compressed file
            FileInputStream input = new FileInputStream(file);

            // Create a zip input stream from the compressed file
            TarArchiveInputStream tarinput = new TarArchiveInputStream(input);

            // Read the first entry in the zip file
            TarArchiveEntry entry = tarinput.getNextEntry();

            // Create a reader for reading the uncompressed data
            InputStreamReader reader = new InputStreamReader(tarinput, "UTF-8");

            BufferedReader bufferedReader=new BufferedReader(reader);
            // Read data from the zip file and process it
            String line;

            while ((line = bufferedReader.readLine()) != null) {
                String[] columns = line.split("\t",2); //Read the line and split it (cause the line is composed by (docNo \t document))

                int docNo;
                try{ docNo = Integer.parseInt(columns[0]); }catch (NumberFormatException e){continue;}
                if(columns[1].isEmpty()) continue;

                //preprocess the document
                String document = Parser.processDocument(columns[1],stopWordsStemming); //Get document
                //elaborate the document
                createIndex(document, docNo);
            }


            // Close the input stream
            bufferedReader.close();
            input.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        //after finishing the documents saves block that is in the main memory.
        if(encodingType.equals("text")) {
            writeTextBlock(lexicon, lexicon.sortLexicon(), docIndex.sortDocIndex());
        }
        else{
            writeBytesBlock(lexicon, lexicon.sortLexicon(), docIndex.sortDocIndex());
        }

        indexBuilder.setIndexBuilder(new HashMap<>());
        lexicon.setLexicon(new HashMap<>());
        docIndex.setDocIndex(new HashMap<>());
        blockCounter += 1;

        System.gc(); // garbage collector

        Merger merger = new Merger();

        if(encodingType.equals("text"))
            merger.mergeTextBlocks(blockCounter,encodingType,statistics);
        else
            merger.mergeByteBlocks(blockCounter,encodingType,statistics);

        saveStatistics();
    }


    //function that taken a document and a doc no elaborate the document to create the index.
    public void createIndex(String document, int docNo){

        float totalMemory = Runtime.getRuntime().totalMemory();
        float memoryUsage = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        float percentageMemory= (memoryUsage / totalMemory) * 100;

        //if the available memory is not enough to process the document saves the current block into the disk.
        if (percentageMemory >= 75 ){
            if(encodingType.equals("text")) {
                writeTextBlock(lexicon, lexicon.sortLexicon(), docIndex.sortDocIndex());
            }
            else{
                writeBytesBlock(lexicon, lexicon.sortLexicon(), docIndex.sortDocIndex());
            }
             //writes the current block to disk

            lexicon.setLexicon(new HashMap<>());
            indexBuilder.setIndexBuilder(new HashMap<>());
            docIndex.setDocIndex(new HashMap<>());
            blockCounter += 1;

            System.gc(); //calls the garbage collector to force to free memory.
        }
        String[] terms = document.split(" ");
        HashMap<String, Integer> counter = new HashMap<>();
        //associate to every term the term count in the document.
        for (String term : terms){
            counter.put(term, counter.containsKey(term) ? counter.get(term) + 1 : 1);
        }
        // update the index information for every term.
        for (String term : counter.keySet()) {
            lexicon.addInformation(term, 0, 0, 0,
                    0, 0, counter.get(term));
            indexBuilder.addPosting(term, docId, counter.get(term));
            statistics.setPostings(statistics.getPostings() + 1);
        }
        docIndex.addDocument(docId, docNo, terms.length);
        docId += 1;
        statistics.setnDocs(statistics.getNDocs() + 1);
        statistics.setAvdl(statistics.getAvdl() + terms.length);
    }

    //function that writes the current block that is in memory to the disk.
    public void writeBytesBlock(Lexicon lexicon, ArrayList<String> sortedTerms, ArrayList<Integer> sortedDocIds){

        TextWriter lexiconWriter = new TextWriter("Output/Lexicon/lexicon" + blockCounter + ".txt");
        Compressor compressor = new Compressor();
        ByteWriter docIDWriter = new ByteWriter("Output/DocIds/docIds" + blockCounter + ".dat", compressor);
        ByteWriter freqWriter = new ByteWriter("Output/Frequencies/freq" + blockCounter + ".dat", compressor);
        ByteWriter docIndexWriter = new ByteWriter("Output/DocumentIndex/documentIndex" + blockCounter + ".dat", compressor);

        //saves the document index.

        for(Integer docId : sortedDocIds){
            docIndexWriter.write(docId);
            docIndexWriter.write(docIndex.docIndex.get(docId).getDocNo());
            docIndexWriter.write(docIndex.docIndex.get(docId).getDocLen());
        }
        //saves the lexicon and the docIds and frequencies in the relative files.
        for (String term : sortedTerms){
            lexicon.getLexicon().get(term).setPostingListLength(indexBuilder.getIndexBuilder().get(term).size());
            lexiconWriter.write(term + " " + lexicon.getLexicon().get(term).toString() + "\n");
            for (Posting posting : indexBuilder.getIndexBuilder().get(term)){
                docIDWriter.write(posting.getDocID());
                freqWriter.write(posting.getTermFrequency());
            }
        }

        docIDWriter.close();
        freqWriter.close();
        lexiconWriter.close();
        docIndexWriter.close();

        System.out.println("Successfully wrote to the files.");
    }


    public void writeTextBlock(Lexicon lexicon, ArrayList<String> sortedTerms, ArrayList<Integer> sortedDocIds){

        TextWriter lexiconWriter = new TextWriter("Output/Lexicon/lexicon" + blockCounter + ".txt");
        TextWriter docIDWriter = new TextWriter("Output/DocIds/docIds" + blockCounter + ".txt");
        TextWriter freqWriter = new TextWriter("Output/Frequencies/freq" + blockCounter + ".txt");
        TextWriter docIndexWriter = new TextWriter("Output/DocumentIndex/documentIndex" + blockCounter + ".txt");

        //saves the document index.

        for(Integer docId : sortedDocIds){
            docIndexWriter.write(docId);
            docIndexWriter.write(docIndex.docIndex.get(docId).getDocNo());
            docIndexWriter.write(docIndex.docIndex.get(docId).getDocLen());
        }
        //saves the lexicon and the docIds and frequencies in the relative files.
        for (String term : sortedTerms){
            lexicon.getLexicon().get(term).setPostingListLength(indexBuilder.getIndexBuilder().get(term).size());
            lexiconWriter.write(term + " " + lexicon.getLexicon().get(term).toString() + "\n");
            for (Posting posting : indexBuilder.getIndexBuilder().get(term)){
                docIDWriter.write(posting.getDocID());
                freqWriter.write(posting.getTermFrequency());
            }
        }

        docIDWriter.close();
        freqWriter.close();
        lexiconWriter.close();
        docIndexWriter.close();

        System.out.println("Successfully wrote to the files.");
    }


    //function that saves the collection statistics.
    public void saveStatistics(){
        statistics.setAvdl(statistics.getAvdl() / statistics.getNDocs());
        try{
            FileWriter writer = new FileWriter("Output/CollectionStatistics/collectionStatistics.txt");
            writer.write(statistics.getNDocs() + " "
                    + statistics.getAvdl() + " " + statistics.getPostings());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args){

        System.out.println("Welcome");
        String file = args[0];
        String type = args[1];
        Boolean stopWordsStemming = Boolean.valueOf(args[2]); //Stopwords Removal

        if(!type.equals("text") && !type.equals("bytes")){
            System.out.println("Sorry the encoding type is wrong please try again");
        }
        else{
            MainIndexing index = new MainIndexing();
            long start = System.currentTimeMillis();
            index.processCollection(file, type, stopWordsStemming);
            long end = System.currentTimeMillis();
            System.out.println("Elapsed Time in milliseconds: " + (end-start));
        }
    }
}