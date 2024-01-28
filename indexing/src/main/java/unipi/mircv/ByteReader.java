package unipi.mircv;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;

public class ByteReader {

    //class that reads encoded integers using a buffered input stream and a specific
//compressor taken as argument.
        public BufferedInputStream bufferedInputStream;
        public Compressor compressor;

        public ByteReader(String file, Compressor compressor){
            this.compressor = compressor;
            try{
                this.bufferedInputStream = new BufferedInputStream(new FileInputStream(file));
            }catch (IOException e){
                e.printStackTrace();
            }
        }

        public int read() {
            return compressor.readBytes(bufferedInputStream);
        }

        public void close() {
            try {
                bufferedInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

}
