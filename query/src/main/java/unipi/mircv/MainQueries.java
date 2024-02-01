package unipi.mircv;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

/**
 * The MainQueries class serves as the central hub for handling user queries. It facilitates user interactions
 * for query input, and query execution using an inverted index.
 */

public class MainQueries {

    private int k; // Number of top results to retrieve
    public QueryProcessor queryProcessor;
    public String stringScore;  // string for the type of scoring documents
    public String documentProcessor;
    public Parser parser;

    // Flag indicating the query type:disjunctive (true) or conjunctive (false)
    private final String queryType;

    /**
     * Constructor for MainQueries class.
     *
     * @param k                Number of top results to retrieve
     * @param stringScore      Type of document scoring
     * @param documentProcessor How to process the posting list (daat, maxscore)
     * @param queryType        Type of relation (conjunctive or disjunctive)
     * @param stopwordStemming Flag for enabling or disabling stopwords removal and stemming
     * @param encodingType     Encoding type for processing the query
     */
    public MainQueries(int k, String stringScore, String documentProcessor, String queryType, boolean stopwordStemming, String encodingType) {

        this.k = k;
        this.stringScore = stringScore;
        this.documentProcessor = documentProcessor;
        this.queryType = queryType;
        this.queryProcessor = new QueryProcessor(encodingType);
        this.parser = new Parser();

    }


    /**
     * Processes the user query and returns the top-k results.
     *
     * @param query             The query to process
     * @param stopwordStemming Flag for enabling or disabling stopwords removal and stemming
     * @param encodingType     Encoding type for processing the query
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
     * Scores the documents using the specified score function.
     *
     * @param queryTerms   Terms of the query
     * @param postingLists Posting lists of the query terms
     * @param encodingType type of encoding (byte or text)
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

    /**
     * The main method to execute queries and display results.
     *
     * @param args Command line arguments for configuring query parameters
     */
    public static void main(String[] args) {

        System.out.println("*** MAIN QUERIES ***");

        Scanner scanner = new Scanner(System.in);

        // Getting input for each parameter
        System.out.print("Number of results: ");
        int nResults = Integer.parseInt(scanner.nextLine());

        System.out.print("Score function to use (tfidf/bm25): ");
        String scoreType = scanner.nextLine(); // Use next() for reading a single word

        System.out.print("How to process the postinglist (daat/maxscore): ");
        String documentProcessor = scanner.nextLine();

        System.out.print("Type of relation (conjunctive/disjunctive): ");
        String queryType = scanner.nextLine();

        System.out.print("Stopwords Removal and stemming (true/false): ");
        boolean stopwordStemming = Boolean.parseBoolean(scanner.nextLine());

        System.out.print("Encoding type (bytes/text): ");
        String encodingType = scanner.nextLine();

        System.out.println("\n*** LOADING STRUCTURES .... ***\n");
        MainQueries mainQueries = new MainQueries(nResults, scoreType, documentProcessor, queryType, stopwordStemming, encodingType);


        while (true) {
            System.out.print("*** INSERT A QUERY: \n");
            String query=scanner.nextLine();

            long start = System.currentTimeMillis();
            PQueue results = mainQueries.processQuery(query, stopwordStemming, encodingType);
            long end = System.currentTimeMillis();

            System.out.println("*** Time in milliseconds: " + (end - start) + " ***");
            results.printResults();
        }
    }
}