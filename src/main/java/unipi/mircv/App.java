package unipi.mircv;
import java.io.IOException;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.FileUtils;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main(String[] args) throws IOException {

            
            String path = "C:/Users/kecco/Desktop/collection.tar.gz";
            //Path of the collection to be read
            File file = new File(path);
    
            //Try to open the collection provided
            try (FileInputStream fileInputStream = new FileInputStream(file);
                 RandomAccessFile documentIndexFile = new RandomAccessFile(DOCUMENT_INDEX_PATH, "rw")) {
    
                //Create an input stream for the tar archive
                TarArchiveInputStream tarInput = new TarArchiveInputStream(new GzipCompressorInputStream(fileInputStream));
    
                //Get the first file from the stream, that is only one
                TarArchiveEntry currentEntry = tarInput.getNextTarEntry();
    
                //If the file exist
                if(currentEntry != null) {
    
                    //Read the uncompressed tar file specifying UTF-8 as encoding
                    InputStreamReader inputStreamReader = new InputStreamReader(tarInput, StandardCharsets.UTF_8);
    
                    //Create a BufferedReader in order to access one line of the file at a time
                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
    
                    //Variable to keep the current line read from the buffer
                    String line;
                }
            }
    }
}