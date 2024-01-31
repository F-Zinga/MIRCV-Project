package unipi.mircv;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

/**
 * A class for reading text data from a file using a Scanner.
 */
public class TextReader {
        public Scanner scanner; //for reading text data.

        /**
         * Constructs a TextReader with the given file path.
         *
         * @param file The file path to read from.
         */
        public TextReader(String file){
            try{
                this.scanner = new Scanner(new File(file));
            }catch (IOException e){
                e.printStackTrace();
            }
        }

        /**
         * Reads an integer from the text input. Returns -1 if there are no more integers.
         *
         * @return The read integer or -1 if no more integers are available.
         */
        public int read() {
            if(scanner.hasNext()){
                return Integer.parseInt(scanner.next());
            }
            else return -1;
        }

        /**
         * Reads a line of text from the input.
         *
         * @return The read line of text.
         */
        public String readLine(){
            return scanner.nextLine();
        }


        /**
         * Closes the underlying Scanner.
         */
        public void close(){
            scanner.close();
        }

        public Object getReader() {
            return scanner;
        }

        /**
         * Checks if there is another line of text available for reading.
         *
         * @return true if there is another line, false otherwise.
         */
        public boolean hasNextLine(){
            return scanner.hasNextLine();
        }
}
