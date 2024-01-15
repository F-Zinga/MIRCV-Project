package unipi.mircv;
import java.io.*;

/**
 * Represents statistics related to the execution, such as the number of blocks, the total number of parsed documents,
 * and the average document length.
 */
public class Statistics {

        // Path to the statistics file
        final static String PATH = "Files/statistics.txt";
        private int nBlocks; // Number of blocks
        private int nDocs; // Total number of documents
        private int avdl; // Average document length

    @Override
    public String toString() {
        return "Statistics{" +
                "nBlocks=" + nBlocks +
                ", nDocs=" + nDocs +
                ", avdl=" + avdl +
                '}';
    }

    /**
     * Constructs a Statistics object by reading values from the statistics file.
     */
    public Statistics() {
            try {
                //creates a new file instance
                File file = new File(PATH);

                //reads the file
                FileReader fr = new FileReader(file);

                //creates a buffering character input stream
                BufferedReader br = new BufferedReader(fr);

                String line;

                // Reads and sets the number of blocks
                if ((line = br.readLine()) != null) {
                    nBlocks = Integer.parseInt(line);
                }
                // Reads and sets the total number of documents
                if ((line = br.readLine()) != null) {
                    nDocs = Integer.parseInt(line);
                }
                // Reads and sets the average document length
                if ((line = br.readLine())!= null) {
                    avdl = Integer.parseInt(line);
                }
                // Closes the FileReader
                fr.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * Writes the execution statistics, including the number of blocks, the total number of documents parsed,
         * and the average document length, to the statistics file.
         * @param nBlocks Number of blocks written
         * @param nDocs Number of documents parsed in total
         */
        public static void writeStats(int nBlocks, int nDocs, float avdl){

            //Object used to build the lexicon line into a string
            StringBuilder stringBuilder = new StringBuilder();

            //Buffered writer used to format the output
            BufferedWriter bufferedWriter;

            try {
                // Initializes the buffered writer with a FileWriter set to append mode
                bufferedWriter = new BufferedWriter(new FileWriter(PATH,false));

                //build the string containing the statistics
                stringBuilder
                        .append(nBlocks).append("\n")
                        .append(nDocs).append("\n")
                        .append(Math.round(avdl)).append("\n");

                //Write the string in the file
                bufferedWriter.write(stringBuilder.toString());

                //Close the writer
                bufferedWriter.close();

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public int getNBlocks() {
            return nBlocks;
        }

        public int getNDocs() { return nDocs; }

        public int getAvdl() { return  avdl; }

}
