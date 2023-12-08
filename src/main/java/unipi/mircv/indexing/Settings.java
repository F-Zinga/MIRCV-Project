package unipi.mircv.indexing;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class Settings {
    final static String PATH = "Files/settings.txt";

    private boolean compressed;
    private boolean stemmingAndStopWords;

    public boolean isCompressed() {
        return compressed;
    }

    public void setCompressed(boolean compressed) {
        this.compressed = compressed;
    }

    public boolean isStemmingAndStopWords() {
        return stemmingAndStopWords;
    }

    public void setStemmingAndStopWords(boolean stemmingAndStopWords) {
        this.stemmingAndStopWords = stemmingAndStopWords;
    }

    /**
     * Write the configuration of the inverted index, in particular if the stemming and stopwords removal were enabled
     * and the same for compression.
     * @param stemmingAndStopwordsRemoval true if the stemming and stopwords removal were enabled during the indexing.
     * @param compressed true if the compression was enabled during the indexing.
     */
    public static void saveConfiguration(boolean stemmingAndStopwordsRemoval, boolean compressed, boolean debug){

        //Object used to build the lexicon line into a string
        StringBuilder stringBuilder = new StringBuilder();

        //Buffered writer used to format the output
        BufferedWriter bufferedWriter;

        try {
            bufferedWriter = new BufferedWriter(new FileWriter(PATH,false));

            //build the string
            stringBuilder
                    .append(stemmingAndStopwordsRemoval).append("\n")
                    .append(compressed).append("\n")
                    .append(debug).append("\n");

            //Write the string in the file
            bufferedWriter.write(stringBuilder.toString());

            //Close the writer
            bufferedWriter.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
