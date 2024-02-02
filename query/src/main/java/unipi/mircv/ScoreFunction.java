package unipi.mircv;

import java.util.ArrayList;
import java.util.HashMap;

import static unipi.mircv.Parameters.*;

/**
 * Represents a scoring function for document ranking based on information retrieval metrics.
 */
public class ScoreFunction {
    private String[] queryTerms;
    private HashMap<String, Double> idf;
    private HashMap<Integer, DocInfo> docInfo;
    private double avgDocumentLength;
    private String scoreType;


    // Constructor for initializing the scoring function with necessary parameters
    public ScoreFunction(HashMap<String, ArrayList<Posting>> postingLists, String[] queryTerms, QueryProcessor queryProcessor, String scoreType) {
        double nDocuments = queryProcessor.getStatistics().getNDocs();
        this.queryTerms = queryTerms;

        // Calculate IDF values for each term in the query
        this.idf = new HashMap<>();
        for (String term : postingLists.keySet()) {
            double df = queryProcessor.getLexicon().getLexicon().get(term).getPostingListLength();
            idf.put(term, Math.log(nDocuments / df));
        }

        // Initialize average document length and document information
        this.avgDocumentLength = queryProcessor.getStatistics().getAvdl();
        this.docInfo = queryProcessor.getDocIndex().getDocIndex();

        this.scoreType = scoreType;
    }

    /**
     *  Method to calculate the score for a specific term and posting based on the selected scoring type
     * @param term term to analyze
     * @param posting posting of this specified term
     * @param scoreType type of scoring selected
     * @return the score computed
     */
    public double computeScore(String term,Posting posting, String scoreType) {

        double result= 0;


        // Calculate the score based on the BM25 scoring function
        if (scoreType.equals("bm25")) {

            double tf = posting.getTermFrequency();
            double denominator = K1 * ((1 - B) + B * ((double) docInfo.get(posting.getDocID()).getDocLen() / avgDocumentLength)) + tf;
            double idf = this.idf.get(term);

            result = (tf * idf) / denominator;
        }
        // Calculate the score based on the TF-IDF scoring function
        else if (scoreType.equals("tfidf")) {
            double tf = 1 + Math.log(posting.getTermFrequency());
            double idf = this.idf.get(term);
            result = tf * idf;
        }
        // Default case returns a score of 1 if the scoring type is not recognized
        return result;
    }
}

