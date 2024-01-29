package unipi.mircv;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.PriorityQueue;
import java.util.Scanner;
import java.util.Stack;

public class MainEvaluation {

    private File resultFile;
    private PrintWriter writer;

    public MainEvaluation() {
        try {
            resultFile = new File("Data/Output/queryResults.txt");
            if (resultFile.createNewFile()) {
                System.out.println("File created: " + resultFile.getName());
            } else {
                System.out.println("File already exists.");
            }
        } catch (IOException e) {
            System.out.println("An error occurred during the creation of the output file.");
            e.printStackTrace();
        }

        try {
            writer = new PrintWriter(resultFile);
        } catch (FileNotFoundException e) {
            System.out.println("Query result file not founded");
            e.printStackTrace();
        }
    }


    public void processCollection(String file) {
        try {
            File myFile = new File(file);
            Scanner myReader;
            myReader = new Scanner(myFile, StandardCharsets.UTF_8);
            Boolean stopwordStemming= Boolean.valueOf("true");
            String encodingType="byte";
            MainQueries mainQueries = new MainQueries(10, "tfidf", "daat", "disjunctive", stopwordStemming, encodingType);
            int counter = 0;
            while (myReader.hasNextLine()) {
                System.out.println("Processing Query number: " + counter);
                String[] line = myReader.nextLine().split("\t", 2); //Read the line and split it (cause the line is composed by (docNo \t document))

                int qid;
                try {
                    qid = Integer.parseInt(line[0]); //Get docNo
                } catch (NumberFormatException e) {
                    continue;
                }
                processQuery(line[1], qid, mainQueries, stopwordStemming, encodingType);
                counter += 1;
            }
            myReader.close();
            writer.close();
        } catch (FileNotFoundException e) {
            System.out.println("The specified file is not found. Please try again.");
            e.printStackTrace();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

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
            writer.println(qid + " " + "Q0" + " " + fs.getKey() + " " + position + " " + fs.getValue() + " " + "STANDARD");
            position += 1;
        }
    }


    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println("Welcome to the Evaluation");
        String file = args[0];

        MainEvaluation eval = new MainEvaluation();
        long start = System.currentTimeMillis();
        eval.processCollection(file);
        long end = System.currentTimeMillis();
        System.out.println("Elapsed Time in milliseconds: " + (end - start));
    }


}
