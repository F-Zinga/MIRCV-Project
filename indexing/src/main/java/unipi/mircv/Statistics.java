package unipi.mircv;
import java.io.*;

public class Statistics {
        final static String PATH = "Files/statistics.txt";
        private int nBlocks;
        private int nDocs;
        private int avdl;

        private long time;


    @Override
    public String toString() {
        return "Statistics{" +
                "nBlocks=" + nBlocks +
                ", nDocs=" + nDocs +
                ", avdl=" + avdl +
                ", time=" + time +
                '}';
    }

    public Statistics() {
            try {
                //creates a new file instance
                File file = new File(PATH);

                //reads the file
                FileReader fr = new FileReader(file);

                //creates a buffering character input stream
                BufferedReader br = new BufferedReader(fr);

                String line;

                if ((line = br.readLine()) != null) {
                    nBlocks = Integer.parseInt(line);
                }
                if ((line = br.readLine()) != null) {
                    nDocs = Integer.parseInt(line);
                }
                if ((line = br.readLine())!= null) {
                    avdl = Integer.parseInt(line);
                }
                if ((line = br.readLine())!= null) {
                    time = Long.parseLong(line);
                }
                fr.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * Write the statistics of the execution, in particular the number of blocks written and the total number of
         * documents parsed.
         * @param nBlocks Number of blocks written
         * @param nDocs Number of documents parsed in total
         */
        public static void writeStats(int nBlocks, int nDocs, float avdl, long time){

            //Object used to build the lexicon line into a string
            StringBuilder stringBuilder = new StringBuilder();

            //Buffered writer used to format the output
            BufferedWriter bufferedWriter;

            try {
                bufferedWriter = new BufferedWriter(new FileWriter(PATH,false));

                //build the string
                stringBuilder
                        .append(nBlocks).append("\n")
                        .append(nDocs).append("\n")
                        .append(Math.round(avdl)).append("\n")
                        .append(time).append("\n");


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

        public long getTime() { return time; }

}
