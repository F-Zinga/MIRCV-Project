package unipi.mircv;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

/**
 * A class for writing text data to a file using a BufferedWriter.
 */
public class TextWriter{

        public BufferedWriter bufferedWriter; //for writing text data.

        /**
         * Constructs a TextWriter with the given file path.
         *
         * @param file The file path to write to.
         */
        public TextWriter(String file){
            try{
                this.bufferedWriter = new BufferedWriter(new FileWriter(file));
            }catch (IOException e){
                e.printStackTrace();
            }
        }

        /**
         * Writes an integer to the text output, followed by a space.
         *
         * @param number The integer to write.
         * @return 1 if successful, 0 otherwise.
         */
        public int write(int number) {
            try{
                bufferedWriter.write(number + " ");
                return 1;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return 0;
        }

        /**
         * Writes a line of text to the output.
         *
         * @param line The line of text to write.
         */
        public void write(String line){
            try{
                bufferedWriter.write(line);
            }catch (IOException e){
                e.printStackTrace();
            }
        }

        /**
         * Closes the underlying BufferedWriter.
         */
        public void close(){
            try{
                bufferedWriter.close();
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }
