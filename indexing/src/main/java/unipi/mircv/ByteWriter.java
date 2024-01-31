package unipi.mircv;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * A class for writing encoded integers using a buffered output stream and a specified compressor.
 */
public class ByteWriter {
        public BufferedOutputStream bufferedOutputStream; //for writing bytes.
        public Compressor compressor; //for writing encoded integers.

    /**
     * Constructs a ByteWriter with the given file and compressor.
     *
     * @param file       The file path to write to.
     * @param compressor The compressor implementation to use.
     */
        public ByteWriter(String file, Compressor compressor){
            this.compressor = compressor;
            try{
                this.bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(file));
            }catch (IOException e){
                e.printStackTrace();
            }
        }


        /**
         * Writes the encoded representation of an integer to the output stream using the assigned compressor.
         *
         * @param number The integer to be encoded and written.
         * @return The number of bytes written.
         */
        public int write(int number) {
            return compressor.writeBytes(bufferedOutputStream, number);
        }

        /**
         * Closes the underlying buffered output stream.
         */
        public void close(){
            try{
                bufferedOutputStream.close();
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }
