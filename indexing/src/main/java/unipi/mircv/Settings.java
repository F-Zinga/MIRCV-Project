package unipi.mircv;

import java.io.*;

/**
 * Represents the configuration settings for the inverted index, such as stemming and stopwords removal, compression,
 * and debug mode.
 */

public class Settings {
    final static String PATH = "Files/settings.txt";

    private boolean compressed; // Indicates whether compression was enabled during indexing
    private boolean stemmingAndStopWords; // Indicates whether stemming and stopwords removal were enabled during indexing

    private boolean debug; // Indicates whether debug mode is enabled


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
     * Writes the configuration of the inverted index, specifying whether stemming and stopwords removal, compression,
     * and debug mode were enabled.
     * @param stemmingAndStopwordsRemoval true if the stemming and stopwords removal were enabled during the indexing.
     * @param compressed true if the compression was enabled during the indexing.
     */
    public static void saveConfiguration(boolean stemmingAndStopwordsRemoval, boolean compressed){

        //Object used to build the lexicon line into a string
        StringBuilder stringBuilder = new StringBuilder();

        //Buffered writer used to format the output
        BufferedWriter bufferedWriter;

        try {
            // Initializes the buffered writer with a FileWriter set to append mode
            bufferedWriter = new BufferedWriter(new FileWriter(PATH,false));

            //build the string containing the settings
            stringBuilder
                    .append(stemmingAndStopwordsRemoval).append("\n")
                    .append(compressed).append("\n");

            //Write the string in the file
            bufferedWriter.write(stringBuilder.toString());

            //Close the writer
            bufferedWriter.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Loads the settings from the configuration file, including whether stemming and stopwords removal, compression,
     * and debug mode were enabled during indexing.
     * @return True if the settings were successfully loaded, false otherwise.
     */
    public boolean loadSettings() {
        try {
            //creates a new file instance
            File file = new File(PATH);

            //reads the file
            FileReader fr = new FileReader(file);

            //creates a buffering character input stream
            BufferedReader br = new BufferedReader(fr);

            String line;

            // Reads and sets the status of stemming and stopwords removal
            if ((line = br.readLine()) != null) {
                stemmingAndStopWords = Boolean.parseBoolean(line);
            }
            // Reads and sets the status of compression
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
            System.err.println("Configuration not found.");
            return false;
        }
        return true;
    }

}
