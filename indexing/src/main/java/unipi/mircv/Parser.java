package unipi.mircv;

//import it.unipi.mircv.beans.ParsedDocument;
import opennlp.tools.stemmer.PorterStemmer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The Parser class is responsible for processing and tokenizing documents, including tasks such as removing punctuation,
 * handling stop words, and applying stemming.
 */
public class Parser {

    //List of strings that contain stopwords
    static List<String> stopWords = loadStopWords();

    //Path to the file containing the list of stopwords
    static final String STOPWORDS_FILE = "resources/utility/stopwords";

    /**
     * Processes a document by tokenizing it in the format: [doc_id]\t[token1 token2 ... tokenN]\n.
     * @param line           String containing a document of the collection in the format: [doc_id]\t[text]\n
     * @param stopStemming  Flag indicating whether to perform stop word removal and stemming
     * @return Document tokenized based on the specified conditions
     */
    public static DocParsed processDocument(String line,boolean stopStemming){
        //Utility variables to keep the current docno and text
        String docno;
        String text;

        //Divide the line using \t as delimiter, splitting the docNo and the text
        StringTokenizer stringTokenizer = new StringTokenizer(line, "\t");

        //Retrieve the first token, that is the docno
        if(stringTokenizer.hasMoreTokens()){
            docno = stringTokenizer.nextToken();

            //Retrieve the second token, that is the text and cast it to lower case
            if(stringTokenizer.hasMoreTokens()){
                text = stringTokenizer.nextToken().toLowerCase();
            }else{
                //The text is empty, or it was not possible to retrieve it
                return null;
            }
        }else{
            //The line is empty, or it was not possible to retrieve it
            return null;
        }

        //Remove punctuation, then split when there are one or more whitespace characters
        String[] splittedText = removePunctuation(text).split("\\s+");


        if(stopStemming) {
            //Remove stop words
        splittedText = removeStopWords(splittedText, stopWords);

            //Stemming
        splittedText = getStems(splittedText);
        }


        DocParsed doc = new DocParsed(docno, splittedText);

        return doc;

    }

    /**
     * Remove the punctuation replacing it with an empty string
     * @param text String containing a text
     * @return Text without punctuation
     */
    private static String removePunctuation(String text){
        //Replace all punctuation marks with a whitespace character, then trim (cut) the string to remove the whitespaces
        // at the beginning or end of the string.
        return text.replaceAll("[^\\w\\s]", " ").trim();
    }

    /**
     * Remove the stopwords from the text
     * @param text String containing a text
     * @param stopwords List of strings containing the stopwords
     * @return Text without the stopwords
     */
    private static String[] removeStopWords(String[] text, List<String> stopwords){

        //The performance are x6 faster with the streams, than using the manual remove
        ArrayList<String> words = Stream.of(text).collect(Collectors.toCollection(ArrayList<String>::new));
        words.removeAll(stopwords);
        String[] terms = new String[words.size()];
        return words.toArray(terms);
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
        //  The stream is obtained by splitting the text using the whitespace as delimiter;
        //  It's used a map stage where each word is stemmed
        //  The result is collected into an Array of strings
        return Stream.of(terms).map(porterStemmer::stem).toArray(String[]::new);
    }

    /**
     * Loads stop words from the specified file.
     *
     * @return List of stop words
     */
    private static List<String> loadStopWords(){
        System.out.println(" *** loadStopWords... *** ");
        //If the stopwords removal and the stemming is requested, the stopwords are read from a file
        try {
            return Files.readAllLines(Paths.get(STOPWORDS_FILE));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
