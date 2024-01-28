package unipi.mircv;

import org.javatuples.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

/**
 * The main class for processing queries. It interacts with the user to enter queries,
 * change settings, and execute queries using an inverted index.
 */

public class MainQueries {

        private int k;
        public QueryProcessor queryProcessor;
        public String scoreFunction;
        public String documentProcessor;
        public Parser parser;

        // Flag indicating whether the query type is disjunctive (true) or conjunctive (false)
        private final String queryType;


        public MainQueries(int k, String scoreFunction, String documentProcessor, String queryType, boolean stopwordStemming, String encodingType) {
            this.k = k;
            this.scoreFunction = scoreFunction;
            this.documentProcessor = documentProcessor;
            this.queryType = queryType;
            this.queryProcessor = new QueryProcessor(encodingType);
            this.parser = new Parser();
        }

        /**
         * processQuery method is used to process a query and return top-k results
         * @param query the query to process
         * @return BoundedPriorityQueue of top-k results
         */
        public PQueue processQuery(String query, boolean stopwordStemming){
            String[] queryTerms = parser.processDocument(query, stopwordStemming).split(" "); //Parse the query
            HashMap<String, ArrayList<Posting>> postingLists;

            if (documentProcessor.equals("daat")){
                postingLists = queryProcessor.lookup(queryTerms,encodingType);
            }else{
                postingLists = queryProcessor.initialLookUp(queryTerms,encodingType); //Retrieve candidate postinglists
            }

            return scoreDocuments(queryTerms, postingLists); //Return scores
        }

    /**
     * scoreDocuments method is used to score the documents using the specified scoring function
     * @param queryTerms terms of the query
     * @param postingLists postinglists of the query terms
     * @return BoundedPriorityQueue of top-k results
     */
    public PQueue scoreDocuments(String[] queryTerms, HashMap<String, ArrayList<Posting>> postingLists) {
        ScoreFunction scoreFunction = null;

        if (scoreFunction.equals("tfidf")) {
            scoreFunction = new ScoreFunction(postingLists, queryTerms, queryProcessor, queryType);
        } else if (scoreFunction.equals("bm25")) {
            scoreFunction = new ScoreFunction(postingLists, queryTerms, queryProcessor, queryType);
        } else {
            // Handle the case when an unsupported score function type is provided
            return null;
        }


        if (documentProcessor.equals("maxscore")) {
            MaxScore maxScore = new MaxScore(queryType, queryProcessor);
            return maxScore.scoreDocuments(queryTerms, postingLists, scoreFunction, k);
        } /*else if (documentProcessor.equals("daat")) {
            DAAT daat = new DAAT(queryType, handleIndex);
            return daat.scoreDocuments(queryTerms, postingLists, scoreFunction, k);
        }
        */


        // Handle the case when an unsupported document processor type is provided
        return null;
    }




    public static void main(String[] args) {

        System.out.println("Welcome to Query Processing ");
        int nResults = Integer.parseInt(args[0]); //number of results
        String scoreType = args[1]; //Which scoring function to use
        String documentProcessor = args[2]; //How to process the postinglist
        String queryType = args[3]; //Type of relation (conjunctive or disjunctive)
        Boolean stopwordStemming = Boolean.valueOf(args[4]); //Stopwords Removal
        String encodingType = args[5]; //encoding type

        MainQueries mainQueries = new MainQueries(nResults, scoreType, documentProcessor, queryType, stopwordStemming, encodingType);

        Scanner sc = new Scanner(System.in);

        while(true){
            System.out.print("Insert the new query: ");
            String query = sc.nextLine();

            long start = System.currentTimeMillis();
            PQueue results = mainQueries.processQuery(query, stopwordStemming);
            long end = System.currentTimeMillis();

            System.out.println("Elapsed Time in milliseconds: "+ (end-start));
            results.printResults();
        }
    }


}