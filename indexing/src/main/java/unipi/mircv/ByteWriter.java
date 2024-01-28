package unipi.mircv;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class ByteWriter {
        public BufferedOutputStream bufferedOutputStream;
        public Compressor compressor;

        public ByteWriter(String file, Compressor compressor){
            this.compressor = compressor;
            try{
                this.bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(file));
            }catch (IOException e){
                e.printStackTrace();
            }
        }


        public int write(int number) {
            return compressor.writeBytes(bufferedOutputStream, number);
        }

        public void close(){
            try{
                bufferedOutputStream.close();
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }
