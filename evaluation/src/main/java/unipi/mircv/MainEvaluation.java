package unipi.mircv;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.PriorityQueue;
import java.util.Scanner;
import java.util.Stack;

import static unipi.mircv.Parameters.EVALUATION_RESULTS;

/**
 * The MainEvaluation class manages the evaluation process by processing a collection of queries
 * and writing the results to an output file.
 */
public class MainEvaluation {

    private File resultFile;
    private PrintWriter writer;

    /**
     * Constructor for MainEvaluation class.
     */
    public MainEvaluation() {
        try {
            resultFile = new File(EVALUATION_RESULTS);
            if (resultFile.createNewFile()) {
                System.out.println("*** File created: " + resultFile.getName());
            } else {
                System.out.println("*** FILE ALREADY CREATED. ***");
            }
        } catch (IOException e) {
            System.out.println("** Error during creation of the results file. ***");
            e.printStackTrace();
        }

        try {
            writer = new PrintWriter(resultFile);
        } catch (FileNotFoundException e) {
            System.out.println("*** QUERY RESULT FILE NOT FOUND ***");
            e.printStackTrace();
        }
    }

    /**
     * Processes a collection of queries from a specified file.
     *
     * @param file The path to the file containing queries
     */
    public void processCollection(String file) {
        try {
            File myFile = new File(file);
            Scanner myReader;
            myReader = new Scanner(myFile, StandardCharsets.UTF_8);
            Boolean stopwordStemming= Boolean.valueOf("true");
            String encodingType="byte";
            MainQueries mainQueries = new MainQueries(10, "tfidf", "maxscore", "disjunctive", stopwordStemming, encodingType);
            int counter = 0;
            while (myReader.hasNextLine()) {
                System.out.println("Processing Query number: " + counter);
                String[] line = myReader.nextLine().split("\t", 2); // Reads and splits the line (formatted as docNo \t document)


                int qid;
                try {
                    qid = Integer.parseInt(line[0]); // Extracts docNo
                } catch (NumberFormatException e) {
                    continue;
                }
                processQuery(line[1], qid, mainQueries, stopwordStemming, encodingType);
                counter += 1;
            }
            myReader.close();
            writer.close();
        } catch (FileNotFoundException e) {
            System.out.println("*** The file is not found. Please try again. ***");
            e.printStackTrace();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Processes an individual query and writes the results to the output file.
     *
     * @param query         The query to process
     * @param qid           The query ID
     * @param mainQueries   The MainQueries instance for query processing
     * @param stopwordStemming Flag for enabling or disabling stopwords removal and stemming
     * @param encodingType     Encoding type for processing the query
     */
    public void processQuery(String query, int qid, MainQueries mainQueries, boolean stopwordStemming, String encodingType ) {
        PQueue results = mainQueries.processQuery(query,stopwordStemming,encodingType);
        results.printResults();

        PriorityQueue<DocsRanked> queue = results.getQueue();
        Stack<DocsRanked> stack = new Stack<>();

        // Iterate through the priority queue and add each element to the stack
        while (!queue.isEmpty()) {
            stack.push(queue.poll());
        }

        int position = 1;
        while (!stack.isEmpty()) {
            DocsRanked fs = stack.pop();
            writer.println(qid + " " + "Q0" + " " + fs.getKey() + " " + position + " " + fs.getValue() + " " + "runid1");
            position += 1;
        }
    }

    /**
     * The main method for running the evaluation process.
     *
     * @param args Command line arguments specifying the file with queries
     * @throws IOException          If an IO error occurs during file processing
     * @throws InterruptedException If the execution is interrupted
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println("*** MAIN EVALUATION ***");
        String file = args[0];

        MainEvaluation eval = new MainEvaluation();
        long start = System.currentTimeMillis();
        eval.processCollection(file);
        long end = System.currentTimeMillis();
        System.out.println("*** Time in milliseconds: " + (end - start)+ " ***");
    }


}
