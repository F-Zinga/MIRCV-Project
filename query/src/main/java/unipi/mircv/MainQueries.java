package unipi.mircv;

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
    public String stringScore;
    public String documentProcessor;
    public Parser parser;

    // Flag indicating whether the query type is disjunctive (true) or conjunctive (false)
    private final String queryType;


    public MainQueries(int k, String stringScore, String documentProcessor, String queryType, boolean stopwordStemming, String encodingType) {

        this.k = k;
        this.stringScore = stringScore;
        this.documentProcessor = documentProcessor;
        this.queryType = queryType;
        this.queryProcessor = new QueryProcessor(encodingType);
        this.parser = new Parser();

    }

    /**
     * processQuery method is used to process a query and return top-k results
     *
     * @param query the query to process
     * @return BoundedPriorityQueue of top-k results
     */
    public PQueue processQuery(String query, boolean stopwordStemming, String encodingType) {
        String[] queryTerms = parser.processDocument(query, stopwordStemming).split(" "); //Parse the query
        HashMap<String, ArrayList<Posting>> postingLists;

        if (documentProcessor.equals("daat")) {
            postingLists = queryProcessor.lookup(queryTerms, encodingType);
        } else {
            postingLists = queryProcessor.initialLookUp(queryTerms, encodingType); //Retrieve candidate postinglists
        }

        return scoreDocuments(queryTerms, postingLists, encodingType); //Return scores
    }


    /**
     * scoreDocuments method is used to score the documents using the specified scoring function
     *
     * @param queryTerms   terms of the query
     * @param postingLists postinglists of the query terms
     * @return BoundedPriorityQueue of top-k results
     */
    public PQueue scoreDocuments(String[] queryTerms, HashMap<String, ArrayList<Posting>> postingLists, String encodingType) {
        /*
        if (documentProcessor.equals("daat")) {
            DAAT daat = new DAAT(relationType, handleIndex);
            if (scoringFunction.equals("tfidf")) {
                TFIDF tfidf = new TFIDF(postingLists, queryTerms, handleIndex);
                return daat.scoreDocuments(queryTerms, postingLists, tfidf, k);
            } else if (scoringFunction.equals("bm25")) {
                BM25 bm25 = new BM25(postingLists, queryTerms, handleIndex, 1.2, 0.75);
                return daat.scoreDocuments(queryTerms, postingLists, bm25, k);
            }
        } else
        */
        if (documentProcessor.equals("maxscore")) {
            MaxScore maxScore = new MaxScore(queryType, queryProcessor);
            ScoreFunction x = new ScoreFunction(postingLists, queryTerms, queryProcessor,stringScore);
            return maxScore.scoreDocuments(queryTerms, postingLists, x, k, encodingType,stringScore);
            }
        return null;
    }

    public static void main(String[] args) {

        System.out.println("Welcome to Query Processing ");
        int nResults = Integer.parseInt(args[0]); //number of results
        String scoreType = args[1]; //Which scoring function to use
        String documentProcessor = args[2]; //How to process the postinglist (daat, maxscore)
        String queryType = args[3]; //Type of relation (conjunctive or disjunctive)
        Boolean stopwordStemming = Boolean.valueOf(args[4]); //Stopwords Removal
        String encodingType = args[5]; //encoding type

        MainQueries mainQueries = new MainQueries(nResults, scoreType, documentProcessor, queryType, stopwordStemming, encodingType);

        Scanner sc = new Scanner(System.in);

        while (true) {
            System.out.print("Insert the new query: ");
            String query = sc.nextLine();

            long start = System.currentTimeMillis();
            PQueue results = mainQueries.processQuery(query, stopwordStemming, encodingType);
            long end = System.currentTimeMillis();

            System.out.println("Elapsed Time in milliseconds: " + (end - start));
            results.printResults();
        }
    }
}