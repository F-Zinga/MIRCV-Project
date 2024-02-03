package unipi.mircv;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

/**
 * The Compressor class is the class that implements the encode and decode methods.
 */

public class Compressor {

    /**
     * Encodes an integer into a list of integers and returns the encoded result.
     *
     * @param number The integer to be encoded.
     * @return An ArrayList of integers representing the encoded form of the input integer.
     */
        public ArrayList<Integer> encode(int number){
            ArrayList<Integer> numbers = new ArrayList<>();
            ArrayList<Integer> encoded = new ArrayList<>();

            // Continue encoding until the entire number is represented
            while(true){
                numbers.add(number % 128);
                if (number < 128){
                    break;
                }
                number /= 128;
            }
            // Adjust the first element to indicate the end of encoding
            numbers.set(0, numbers.get(0) + 128);

            // Reverse the list for the final encoded form
            for(int i = 0; i<numbers.size(); i++){
                encoded.add(numbers.get(numbers.size() - i - 1));
            }
            return encoded;
        }

    /**
     * Decodes a list of integers and returns the original integer.
     *
     * @param bytes The ArrayList of integers representing the encoded form.
     * @return The decoded integer.
     */
        public int decode(ArrayList<Integer> bytes){
            int decoded = 0;
            for(Integer number : bytes){
                if (number < 128){
                    decoded = 128 * decoded + number;
                }
                else{
                    decoded = 128 * decoded + number - 128;
                }
            }
            return decoded;
        }

    /**
     * Reads bytes from a BufferedInputStream and returns the decoded integer.
     *
     * @param file The BufferedInputStream to read bytes from.
     * @return The decoded integer.
     */
        public int readBytes(BufferedInputStream file){
            ArrayList<Integer> bytes = new ArrayList<>();
            int byteRead;
            int decoded = 0;
            try{
                // Continue reading until the end of the file or the end of the encoded integer
                while(true){
                    byteRead = file.read();
                    if(byteRead == -1) break;
                    bytes.add(byteRead);
                    if(byteRead >= 128){
                        break;
                    }
                }
                if (byteRead != -1) decoded = decode(bytes);
                else decoded = -1;
            }catch (IOException e){
                e.printStackTrace();
            }
            return decoded;
        }

    /**
     * Reads bytes from a RandomAccessFile and returns the decoded integer.
     *
     * @param file The RandomAccessFile to read bytes from.
     * @return The decoded integer.
     */
        public int readBytes(RandomAccessFile file){
            ArrayList<Integer> bytes = new ArrayList<>();
            int byteRead;
            int n = 0;
            try{
                // Continue reading until the end of the file
                while(true){
                    byteRead = file.read();
                    bytes.add(byteRead);
                    if(byteRead >= 128){
                        break;
                    }
                }
                n = decode(bytes);
            }catch (IOException e){
                e.printStackTrace();
            }
            return n;
        }

    /**
     * Writes the codification of an integer to a BufferedOutputStream and returns the number of bytes written.
     * Take a file and an int and write the codification of that integer to that file using the encode function.
     *
     * @param file   The BufferedOutputStream to write bytes to.
     * @param number The integer to be codified and written.
     * @return The number of bytes written.
     */
        public int writeBytes(BufferedOutputStream file, int number){
            ArrayList<Integer> bytes;
            bytes = encode(number);
            int nBytesWrite = 0;
            try{
                // Write each byte of the codification to the file
                for(Integer value : bytes){
                    file.write(value);
                    nBytesWrite ++;
                }
            }catch (IOException e){
                e.printStackTrace();
            }
            return  nBytesWrite;
        }
    }

