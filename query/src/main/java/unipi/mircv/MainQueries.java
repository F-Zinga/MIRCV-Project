package unipi.mircv;

import org.javatuples.Pair;

import java.util.ArrayList;
import java.util.Scanner;

/**
 * The main class for processing queries. It interacts with the user to enter queries,
 * change settings, and execute queries using an inverted index.
 */

public class MainQueries {

        // The lexicon for storing and retrieving terms during query processing
        private static Lexicon lexicon = new Lexicon();

        // Flag indicating whether the scoring function is BM25 (true) or TFIDF (false)
        private static boolean bm25scoring = false;

        // Flag indicating whether the query type is disjunctive (true) or conjunctive (false)
        private static boolean queryType = true;


        public static void main( String[] args )
        {

            // Load configuration settings
            Settings settings = new Settings();

            //If the configuration is not found, inverted index is not present; exit the program
            if(!settings.loadSettings())
                return;

            // Build inverted index configuration
            System.out.println(" *** loadSettings... ***");

            System.out.println(" *** loadLexicon... *** ");
            // Load lexicon into memory
            lexicon = new Lexicon();
            lexicon.loadLexicon();

            // Load document index into memory
            System.out.println(" *** loadDocumentIndex... *** ");
            DocIndex documentIndex = new DocIndex();
            documentIndex.loadDocumentIndex();

            System.out.println("*** DataStructure loaded! *** ");

            //Flag indicating if the stopwords removal and stemming are enabled, retrieved from the configuration
            boolean stopwordsRemovalAndStemming = settings.getStemmingAndStopWords();

            //Set the initial parameters for the query processor
            setQueryProcessorParameters(settings);

            //Wait for a new command
            //This must be modified in order to have also the possibility to change the query parameters
            while (true) {

                //Get a command from the user
                int command = getCommand();

                //Check the command
                if(command == 0) {

                    //Read the next query
                    String query = getQuery();

                    //Parse the query
                    String[] queryTerms = parseQuery(query, stopwordsRemovalAndStemming);

                    //If the query string is equal to null it means that the query contains all stopwords or all the terms
                    // not present in the lexicon.
                    if(queryTerms == null || queryTerms.length == 0) {
                        System.out.println("Please rewrite the query: it is too generic.");
                        continue;
                    }

                    //Load the posting list of the terms in the query
                    PostingList[] postingLists = new PostingList[queryTerms.length];

                    //For each term in the query terms array
                    for (int i = 0; i < queryTerms.length; i++) {

                        //Instantiate the posting for the i-th query term
                        postingLists[i] = new PostingList();

                        //Load in memory the posting list of the i-th query term
                        postingLists[i].openList(lexicon.get(queryTerms[i]));

                        //Debug
                        System.out.println(queryTerms[i] + ": " + postingLists[i].size());
                    }

                    // Execute the query and score the collection
                    ArrayList<Pair<Long, Double>> result;

                    //Score the collection
                    if(queryType){
                        result = MaxScore.scoreCollectionDisjunctive(postingLists,documentIndex, bm25scoring, settings.getDebug());
                    }else {
                        result = MaxScore.scoreCollectionConjunctive(postingLists,documentIndex, bm25scoring, settings.getDebug());
                    }


                    //Print the results
                    System.out.println("\n#\tDOCNO\t\tSCORE");
                    for(int i = 0; i < result.size(); i++){
                        System.out.println((i+1) +
                                ")\t" +
                                documentIndex.get(result.get(i).getValue0()).getDocNo() +
                                "\t"+result.get(i).getValue1());
                    }

                    System.out.println();

                    //Close the posting lists
                    for (PostingList postingList : postingLists) {
                        postingList.closeList();
                    }

                } else if(command == 1) { //Change settings command

                    // Change query processor settings
                    changeSettings(settings);
                    System.out.println("Settings changed!");

                } else if (command == 2) { //Exit command

                    return;
                }
            }

        }


        /**
         * Retrieves a query from the user.
         * @return The input query.
         */
        private static String getQuery(){

            //Scanner to read from the standard input stream
            Scanner scanner = new Scanner(System.in);

            System.out.println("Insert a query:");

            return "-1\t" + scanner.nextLine(); // -1 indicates a query during parsing
        }


        /**
         * Retrieves a command from the user.
         * @return 0 for a new query, 1 to change settings, 2 to exit.
         */
        private static int getCommand(){
            do {

                //Scanner to read from the standard input stream
                Scanner scanner = new Scanner(System.in);

                System.out.println(
                        "0 -> Insert a query\n" +
                                "1 -> Change settings\n" +
                                "2 -> Exit");

                String result;

                if(scanner.hasNext()) {
                    result = scanner.nextLine();
                    switch (result) {
                        case "0":
                            return 0;
                        case "1":
                            return 1;
                        case "2":
                            return 2;
                    }
                }

                System.out.println("Input not valid, choose one of the following: ");
            } while (true);
        }

        /**
         * Changes the settings of the query processor
         */
        private static void changeSettings(Settings settings){
            setQueryProcessorParameters(settings);
        }

        /**
         * Parses the query and returns the list of terms, applying stemming and stopwords removal if specified.
         * @param query The input query string
         * @param stopStemming If true, removes stopwords and applies stemming.
         * @return the array of terms after the parsing of the query
         */
        public static String[] parseQuery(String query, boolean stopStemming) {

            //Array of terms to obtain the result
            ArrayList<String> results = new ArrayList<>();

            //Parse the query using the same configuration of the indexer
            DocParsed docParsed = Parser.processDocument(query, stopStemming);

            //If no terms are returned by the parser then return null
            if(docParsed == null){
                return null;
            }

            //Remove the query terms that are not present in the lexicon
            for(String term : docParsed.getTerms()){
                if(lexicon.get(term) != null){
                    results.add(term);
                }
            }

            //Return an array of String containing the results of the parsing process
            return results.toArray(new String[0]);
        }

        /**
         * Sets the query processor parameters, including scoring function, query type, and debug mode
         */
        private static void setQueryProcessorParameters(Settings settings){
            //Scanner to read from the standard input stream
            Scanner scanner = new Scanner(System.in);
            boolean correctParameters = false;

            // Set scoring function
            while (!correctParameters) {
                System.out.println("\nSet the query processor parameters:");
                System.out.println("Scoring function:\n0 -> TFIDF\n1 -> BM25");

                String result;

                if (scanner.hasNext()) {
                    result = scanner.nextLine();
                    //If 0 => bm25scoring is false, otherwise is true
                    switch (result) {
                        case "0":
                            bm25scoring = false;
                            correctParameters = true;
                            break;
                        case "1":
                            bm25scoring = true;
                            correctParameters = true;
                            break;
                    }
                }

                if(!correctParameters)
                    System.out.println("Input not valid, choose one of the following: ");
            }

            correctParameters = false;
            // Set query type
            while (!correctParameters) {
                System.out.println("Query type:\n0 -> Disjunctive\n1 -> Conjunctive");

                String result;

                if (scanner.hasNext()) {
                    result = scanner.nextLine();
                    //If 0 (true) => disjunctive, 1 (false) => conjunction
                    switch (result) {
                        case "0":
                            queryType = true;
                            correctParameters = true;
                            break;
                        case "1":
                            queryType = false;
                            correctParameters = true;
                            break;
                    }
                }

                if(!correctParameters)
                    System.out.println("Input not valid, choose one of the following: ");
            }

        }
}