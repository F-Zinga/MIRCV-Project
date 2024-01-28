package unipi.mircv;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

public class TextReader {
        public Scanner scanner;

        public TextReader(String file){
            try{
                this.scanner = new Scanner(new File(file));
            }catch (IOException e){
                e.printStackTrace();
            }
        }

        public int read() {
            if(scanner.hasNext()){
                return Integer.parseInt(scanner.next());
            }
            else return -1;
        }

        public String readLine(){
            return scanner.nextLine();
        }


        public void close(){
            scanner.close();
        }

        public Object getReader() {
            return scanner;
        }

        public boolean hasNextLine(){
            return scanner.hasNextLine();
        }
}
