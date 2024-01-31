package unipi.mircv;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * A class for reading bytes from a random access file using a specified compressor.
 */
public class RandomByteReader {

        public RandomAccessFile randomAccessFile; //for seeking and reading bytes.
        public FileInputStream fileInputStream; //for reading from the random access file.
        public BufferedInputStream bufferedInputStream; // for reading bytes from the file input stream.
        public Compressor compressor; //for reading encoded integers.


    /**
     * Constructs a RandomByteReader with the given file path and compressor.
     *
     * @param file       The file path to read from.
     * @param compressor The compressor implementation to use.
     */
        public RandomByteReader(String file, Compressor compressor){
            this.compressor = compressor;
            try{
                this.randomAccessFile = new RandomAccessFile(file, "r");
                this.fileInputStream = new FileInputStream(this.randomAccessFile.getFD());
                this.bufferedInputStream = new BufferedInputStream(this.fileInputStream);
            }catch (IOException e){
                e.printStackTrace();
            }
        }

    /**
     * Reads an encoded integer from the input stream using the assigned compressor.
     *
     * @return The decoded integer.
     */
        public int read() {
            return compressor.readBytes(bufferedInputStream);
        }


    /**
     * Closes the underlying buffered input stream.
     */
        public void close() {
            try {
                bufferedInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        public Object getReader() {
            return bufferedInputStream;
        }

    /**
     * Moves the file pointer to the specified offset and updates the input streams accordingly.
     *
     * @param offset The offset to seek within the random access file.
     */
        public void goToOffset(int offset){
            try{
                randomAccessFile.seek(offset);
                fileInputStream = new FileInputStream(randomAccessFile.getFD());
                bufferedInputStream = new BufferedInputStream(fileInputStream);
            }catch (IOException e){
                e.printStackTrace();
            }

        }
    }