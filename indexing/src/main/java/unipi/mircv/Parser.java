package unipi.mircv;

import opennlp.tools.stemmer.PorterStemmer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

/**
 * The Parser class is responsible for processing and tokenizing documents, including tasks such as removing punctuation,
 * handling stop words, and applying stemming.
 */

public class Parser {

    // A set of stop words for faster lookup
    static Set<String> stopWords= new HashSet<>(loadStopWords());

    //Path to the file containing the list of stopwords
    static final String STOPWORDS_FILE = "resources/utility/stopwords";

    /**
     * Default constructor for the Parser class.
     */
    public Parser() {

    }

    /**
     * Processes a document by remove stopwords and punctuation and applying stemming
     * @param line           String containing a document of the collection in the format: [doc_id]\t[text]\n
     * @param stopStemming  Flag indicating whether to perform stop word removal and stemming
     * @return Document tokenized based on the specified conditions
     */
    public static String processDocument(String line,boolean stopStemming){

        //Remove punctuation, then split when there are one or more whitespace characters
        String[] splittedText = removePunctuation(line).split("\\s+");


        if(stopStemming) {
            //Remove stop words
        splittedText = removeStopWords(splittedText, stopWords);

            //Stemming
        splittedText = getStems(splittedText);
        }

        return String.join(" ",splittedText);

    }

    /**
     * Remove the punctuation by replacing it with an empty string.
     * @param text String containing a text
     * @return Text without punctuation
     */
    private static String removePunctuation(String text){
        //Replace all punctuation marks with a whitespace character, then trim (cut) the string to remove the whitespaces
        // at the beginning or end of the string.
        return text.replaceAll("[^a-zA-Z0-9]", " ").trim().toLowerCase().replaceAll(" +"," ");
    }

    /**
     * Remove the stopwords from the text
     * @param text String containing a text
     * @param stopwords List of strings containing the stopwords
     * @return Text without the stopwords
     */
    private static String[] removeStopWords(String[] text, Set<String> stopwords){

        // Use Java Stream API to filter out stop words
        return Arrays.stream(text)
                .filter(word -> !stopWords.contains(word))
                .toArray(String[]::new);
    }

    /**
     * Apply the Porter Stemmer in order to stem each token in a text
     * @param terms Array of String containing a tokenized text
     * @return Array of stems
     */
    private static String[] getStems(String[] terms){

        //Instance of a porter stemmer
        PorterStemmer porterStemmer = new PorterStemmer();

        //Create an array list of stems:
        //  The result is collected into an Array of strings
        return Stream.of(terms).map(porterStemmer::stem).toArray(String[]::new);
    }

    /**
     * Loads stop words from the specified file.
     *
     * @return List of stop words
     */
    private static List<String> loadStopWords(){
        //If the stopwords removal and the stemming is requested, read the stopwords from a file
        try {
            return Files.readAllLines(Paths.get(STOPWORDS_FILE));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
