package unipi.mircv;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;

public class ByteReader {


    /**
     * A class for reading encoded integers using a buffered input stream and a specified compressor.
     */
        public BufferedInputStream bufferedInputStream;
        public Compressor compressor;

        /**
         * Constructs a ByteReader with the given file and compressor.
         *
         * @param file       The file path to read from.
         * @param compressor The compressor implementation to use.
         */
        public ByteReader(String file, Compressor compressor){
            this.compressor = compressor;
            try{
                this.bufferedInputStream = new BufferedInputStream(new FileInputStream(file));
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

}
