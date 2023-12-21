package unipi.mircv;

import java.io.*;

public class Settings {
    final static String PATH = "Files/settings.txt";

    private boolean compressed;
    private boolean stemmingAndStopWords;

    private boolean debug;

    public boolean getDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public boolean isCompressed() {
        return compressed;
    }

    public void setCompressed(boolean compressed) {
        this.compressed = compressed;
    }

    public boolean getStemmingAndStopWords() {
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

    public boolean loadSettings() {
        try {
            //creates a new file instance
            File file = new File(PATH);

            //reads the file
            FileReader fr = new FileReader(file);

            //creates a buffering character input stream
            BufferedReader br = new BufferedReader(fr);

            String line;

            if ((line = br.readLine()) != null) {
                stemmingAndStopWords = Boolean.parseBoolean(line);
            }
            if ((line = br.readLine()) != null) {
                compressed = Boolean.parseBoolean(line);
            }

            /*
            if((line = br.readLine()) != null){
                debug = Boolean.parseBoolean(line);
            }
             */

            fr.close();

        } catch (IOException e) {
            System.err.println("No indexing configuration found. Try to first create a new index, then start again the"+
                    " query processor.");
            return false;
        }
        return true;
    }

}
