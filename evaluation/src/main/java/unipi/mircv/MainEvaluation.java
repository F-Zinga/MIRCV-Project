package unipi.mircv;

import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.javatuples.Pair;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Main class for the query evaluation module. Provides methods to load queries from a file, generate query results,
 *  * and evaluate them using the TREC_EVAL tool.
 */
public class MainEvaluation{

    //Path to the input file containing the queries
    static final String QUERY_PATH = "Resources/queries/test2020-queries.tsv.gz";

    //Path to the output file containing the results of the queries
    static final String RESULTS_PATH = "Files/queries_results";

    // Flag indicating if the scoring is bm25 (true) or tfidf (false)
    static Boolean bm25scoring = true;

    // Flag indicating if the query is disjunctive (true) or conjunctive (false)
    static Boolean queryType = true;


    public static void main( String[] args )
    {
        // Load configuration settings
        Settings settings = new Settings();

        //If configuration is not found, inverted index is not present; exits the program.
        if(!settings.loadSettings())
            return;

        System.out.println("[QUERY PROCESSOR] Building inverted index configuration:");
        System.out.println(settings);

        System.out.println("[QUERY PROCESSOR] Loading the lexicon in memory...");
        // Load the lexicon into memory
        Lexicon lexicon = new Lexicon();
        lexicon.loadLexicon();

        System.out.println("[QUERY PROCESSOR] Loading the document index in memory...");
        // Load the document index into memory
        DocIndex docIndex = new DocIndex();
        docIndex.loadDocumentIndex();

        // Evaluate disjunctive queries with BM25 scoring
        evaluateQueries(getQueries(), settings, docIndex, lexicon, 0);

        // Evaluate conjunctive queries with BM25 scoring
        queryType = false;
        bm25scoring = true;
        evaluateQueries(getQueries(), settings, docIndex, lexicon, 1);

        // Evaluate conjunctive queries with TFIDF scoring
        queryType = false;
        bm25scoring = false;
        evaluateQueries(getQueries(), settings, docIndex, lexicon, 2);

        // Evaluate disjunctive queries with TFIDF scoring
        queryType = true;
        bm25scoring = false;
        evaluateQueries(getQueries(), settings, docIndex, lexicon, 3);
    }

    /**
     * Read  queries from a file in the format of qid\tquery and return an array of tuple containing the
     * qid and the query: (qid, query)
     * @return an ArrayList of tuple containing the qid and the query: (qid, query)
     */
    private static ArrayList<Pair<Long, String>> getQueries(){

        //Path of the collection to be read
        File file = new File(QUERY_PATH);

        //Try to open the collection
        try (FileInputStream fileInputStream = new FileInputStream(file)){

            //Read the uncompressed tar file specifying UTF-8 as encoding
            InputStreamReader inputStreamReader = new InputStreamReader(new GzipCompressorInputStream(fileInputStream), StandardCharsets.UTF_8);

            //Create a BufferedReader to access one line of the file at a time
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            //Variable to keep the current line read from the buffer
            String line;

            //Array list for the results
            ArrayList<Pair<Long, String>> results = new ArrayList<>();

            //Iterate over the lines
            while ((line = bufferedReader.readLine()) != null ) {

                //Split the line qid\tquery in qid query
                String[] split = line.split("\t");

                //Add it to the results array if both qid and query are present
                if(split[0] != null && split[1] != null) {
                    results.add(new Pair<>(Long.parseLong(split[0]), split[1]));
                }
            }

            return results;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Performs all the queries in the array of queries, using the configuration parameters passed, for the scoring it
     * requires the document index and the lexicon.
     * @param queries array of tuples containing the query id and the query string (queryId, query)
     * @param settings configuration used during the creation of the previous inverted index
     * @param docIndex document index containing the document info
     * @param lexicon lexicon containing the terms information
     */
    private static void evaluateQueries(ArrayList<Pair<Long,String>> queries, Settings settings, DocIndex docIndex, Lexicon lexicon, int k){

        //Object used to build the lexicon line into a string
        StringBuilder stringBuilder;

        //Buffered writer used to format the output
        BufferedWriter bufferedWriter;

        try {
            String fileName = RESULTS_PATH;
            if(k == 0){
                fileName+= "_disj_bm25.txt";
            }
            else if(k == 1){
                fileName+= "_conj_bm25.txt";
            }
            else if(k == 2){
                fileName+= "_conj_tfidf.txt";
            }
            else{
                fileName+= "_disj_tfidf.txt";
            }

            bufferedWriter = new BufferedWriter(new FileWriter(fileName,false));

            double completionTimeTot = 0.0;
            int numberOfQueries = 0;

            for( Pair<Long,String> tuple : queries ){

                //Read the next query, add -1 to indicate that it is a query
                String query = "--\t" + tuple.getValue1();

                //Parse the query
                String[] queryTerms = parseQuery(query, lexicon, settings.getStemmingAndStopWords());


                //If the query string is equal to null it means that the query contains all stopwords or all the terms
                // are not present in the lexicon.
                if(queryTerms == null || queryTerms.length == 0) {
                    System.out.println("You're query is too vague, try to reformulate it.");
                    continue;
                }

                //Remove the duplicates
                queryTerms = Arrays.stream(queryTerms).distinct().toArray(String[]::new);


                //Load the posting list of the terms of the query
                PostingList[] postingLists = new PostingList[queryTerms.length];

                //For each term in the query terms array
                for (int i = 0; i < queryTerms.length; i++) {

                    //Instantiate the posting for the i-th query term
                    postingLists[i] = new PostingList();

                    //Load in memory the posting list of the i-th query term
                    postingLists[i].openList(lexicon.get(queryTerms[i]));

                }

                //Array containing the results of the query
                ArrayList<Pair<Long, Double>> result;

                //Score the collection

                //Retrieve the time at the beginning of the computation
                long begin = System.currentTimeMillis();

                if(queryType){
                    result = MaxScore.scoreCollectionDisjunctive(postingLists,docIndex, bm25scoring, false);
                }else {
                    result = MaxScore.scoreCollectionConjunctive(postingLists,docIndex, bm25scoring, false);
                }

                completionTimeTot += (System.currentTimeMillis() - begin);
                numberOfQueries++;

                //Write the results in a format valid for the TREC_EVAL tool
                for(int i = 0; i < result.size(); i++){

                    //New string builder for the current result
                    stringBuilder = new StringBuilder();

                    //build the string
                    stringBuilder
                            .append(tuple.getValue0()).append(" ")
                            .append("q0 ")
                            .append(docIndex.get(result.get(i).getValue0()).getDocNo()).append(" ")
                            .append(i+1).append(" ")
                            .append(result.get(i).getValue1()).append(" ")
                            .append("runid1").append("\n");

                    //Write the string in the file
                    bufferedWriter.write(stringBuilder.toString());

                }

                //Close the posting lists
                for (PostingList postingList : postingLists) {
                    postingList.closeList();
                }

            }

            System.out.println("Average completion time: " + completionTimeTot/numberOfQueries + "ms");
            System.out.println("Number of queries: " + numberOfQueries);

            //Close the writer
            bufferedWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Parses the input query string and retrieves the list of terms after applying the same parsing process used during the indexing phase.
     * @param query the query string to parse
     * @param stopwordsRemovalAndStemming if true remove the stopwords and applies the stemming procedure.
     * @return the array of terms after the parsing of the query
     */
    public static String[] parseQuery(String query, Lexicon lexicon ,boolean stopwordsRemovalAndStemming) {

        //Array of terms to build the result
        ArrayList<String> results = new ArrayList<>();

        System.out.println(query);

        //Parse the query using the same configuration of the indexer
        DocParsed parsedDocument = Parser.processDocument(query, stopwordsRemovalAndStemming);

        //Return null if no terms are returned by the parser
        if(parsedDocument == null){
            return null;
        }

        //Remove the query terms that are not present in the lexicon
        for(String term : parsedDocument.getTerms()){
            if(lexicon.get(term) != null){
                results.add(term);
            }
        }

        //Return an array of String containing the results of the parsing process
        return results.toArray(new String[0]);
    }


}