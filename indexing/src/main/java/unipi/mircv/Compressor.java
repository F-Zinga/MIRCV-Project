package unipi.mircv;

import org.javatuples.Pair;

import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Iterator;

import static unipi.mircv.Parameters.B;
import static unipi.mircv.Parameters.K1;


public class Compressor {


    /**
     * Computes the number of splits needed to encode the given number using variable-length encoding.
     * It calculates the base 128 logarithm of the number and adds 1 to determine the number of groups
     * of 7 bits composing the number's representation.
     * @param n Number for which to compute the splits.
     * @return The number of splits.
     */
    public static int splitsLog128(long n){
        return (int)(Math.floor(Math.log(n) / Math.log(128)) + 1);
    }


    /**
     * Compresses the given number using Variable Byte Encoding.
     * @param n Number to be compressed.
     * @return Array of bytes containing the code of the number.
     */
    public static byte[] variableByteEncodeNumber(long n){

        //number equal 0 --> return directly the code in VB of 0 (otherwise the algorithm doesn't work)
        //Remember that the log doesn't exist for x<=0
        if(n == 0){
            return new byte[]{(byte) 0x80};//In practice this case shouldn't happen, since a frequency is always > 0
        }

        //Determine the number of splits required to encode the number
        int numberOfBytes = splitsLog128(n);

        //Array to store the encoded bytes
        byte[] bytes = new byte[numberOfBytes];

        //The number representation in big-endian order from the MSByte to the LSByte
        for(int i = numberOfBytes - 1; i >= 0; i--){

            //Prepend of the reminder of the division by 128 (retrieve the 7 LSB)
            byte b = (byte) (n % 128);
            bytes[i] = b;

            //Shift the number right by 7 position
            n /= 128;
        }

        //Set the control bit of the last byte to 1, to indicate that it is the last byte
        bytes[numberOfBytes - 1] += 128;

        //Return the encoded number
        return bytes;
    }


    /**
     * Compresses a list of numbers using Variable Byte Encoding; returns a byte array
     * that is the concatenation of the numbers' codes.
     * @param numbersList Numbers to be compressed.
     * @return Array of bytes containing the compressed numbers.
     */
    public static byte[] variableByteEncodeDocId(ArrayList<Long> numbersList, ArrayList<Block> blocks){

        //Length of each skip block
        int blocksLength = (int) Math.floor(Math.sqrt(numbersList.size()));

        //Array to store the bytes of the encoded numbers
        ArrayList<Byte> bytes = new ArrayList<>();

        int counter = 0; // Counter for the currently processed number, used to determine skip positions.

        int offsetBlocks = 0; //Accumulator storing of the starting byte of the current skip block (relative to its posting list offset)

        int blockSize = 0;  //Accumulator to store the length of the current skip block

        // Iterate through each number in the list
        for (Long number : numbersList) {

            //Increase the counter for the number of numbers processed
            counter++;

            //Encode each number and add it to the end of the array
            for(byte b : variableByteEncodeNumber(number)){
                bytes.add(b);

                //Increment the skip blocks length in bytes
                blockSize++;
            }

            //If at a skip position, create a new skip block
            if(counter%blocksLength == 0 || counter == numbersList.size()){

                //Pass the current starting offset, the length of the encoded block and the current number
                // (is the greater seen until now)
                blocks.add(new Block(offsetBlocks,blockSize, number));

                //Set the starting offset for the next skip block
                offsetBlocks += blockSize;

                //Reset the variables for the next iteration
                blockSize = 0;

            }

        }

        byte[] result = new byte[bytes.size()]; // Convert the ArrayList into a byte array

        //Populates the array with bytes from the ArrayList
        for(int i = 0; i < bytes.size(); i++){
            result[i] = bytes.get(i);
        }

        //Return the array of bytes
        return result;
    }



    public static Pair<byte[],Pair<Double,Double>> variableByteEncodeFreq(ArrayList<Integer> frequencies, ArrayList<Block> blocks,
                                                ArrayList<Long> docIds, Pair<Double,Double> scores,
                                                RandomAccessFile documentIndex, Statistics statistics)
    {

        //Length of each skip block
        int blocksLength = (int) Math.floor(Math.sqrt(frequencies.size()));

        //Array to store the bytes of the encoded numbers
        ArrayList<Byte> bytes = new ArrayList<>();


        int counter = 0; // Counter for the currently processed number, used to determine skip positions.

        int offsetSkipBlocks = 0; //Accumulator storing the starting byte of the current skip block (relative to its posting list offset)

        int blockSize = 0; // Accumulator to store the length of the current skip block.

        Iterator<Block> blocksIterator = blocks.iterator();

        int maxFreq = 0;

        //Store the bm25 score for the current doc id
        double tf_currentBm25;

        //Store the max score for bm25
        double tf_maxScoreBm25 = 0;

        // Iterate through each frequency in the list
        for (Integer freq : frequencies) {

            //Retrieve the maximum to compute the TFIDF term upper bound
            if(freq > maxFreq){
                maxFreq = freq;
            }

            //Compute the bm25 score for the current document
            tf_currentBm25 = freq/ (K1 * ((1-B) + B * ( (double) DocInfo.getDocLenFromFile(documentIndex, docIds.get(counter)) / statistics.getAvdl()) + freq));

            //If the current max score for bm25 is greater than the previous score, update it
            if(tf_currentBm25 > tf_maxScoreBm25){
                tf_maxScoreBm25 = tf_currentBm25;
            }

            //Increase the counter for the number of numbers processed
            counter++;

            //Encode each number and add it to the end of the array
            for(byte b : variableByteEncodeNumber(freq)){
                bytes.add(b);

                //Increment the skip blocks length in bytes
                blockSize++;
            }

            //If at a skip position, complete the information about the skip previously created
            if(counter%blocksLength == 0 || counter == frequencies.size()){

                //Pass the current starting offset and the current number (is the greater seen until now)
                if(blocksIterator.hasNext()){

                    //Set starting offset of the skip block and the length of the encoded frequencies
                    blocksIterator.next().setFrqInfo(offsetSkipBlocks,blockSize);
                }
                //Set starting offset for the next skip block
                offsetSkipBlocks += blockSize;

                //Reset variables for the next iteration
                blockSize = 0;

            }
        }

        //Set the max score parameters
        scores = scores.setAt0((double) maxFreq);
        scores = scores.setAt1(tf_maxScoreBm25);


        byte[] result = new byte[bytes.size()]; // Convert the ArrayList into a byte array.

        //Populates the array with bytes from the ArrayList
        for(int i = 0; i < bytes.size(); i++){
            result[i] = bytes.get(i);
        }

        Pair<byte[],Pair<Double,Double>> tuple = Pair.with(result,scores);

        //Return the array of bytes
        return tuple;
    }


    /**
     * Decodes the given byte array containing Variable Byte Encoding of a list of integers,
     * returning the corresponding decompressed list of integers.
     * @param bytes Compressed list of integers.
     * @return Decompressed list of integers.
     */

    public static ArrayList<Integer> variableByteDecode(byte[] bytes){

        //Array storing the decoded numbers
        ArrayList<Integer> numbers = new ArrayList<>();

        //Accumulator for the current decoded number
        int number = 0;

        // Iterate through each byte in the array
        for (byte aByte : bytes) {

            // Check if the MSB of the byte is 0 using the mask 0x80 = 1000 0000.
            if ((aByte & 0x80) == 0x00) {
                //The MSB is 0, so are not at the end of the sequence of bytes of the code
                number = number * 128 + aByte;
            } else {
                //The MSB is 1, so are at the end

                //Add to the accumulator number*128 + the integer value in aByte discarding the 1 in the MSB
                number = number * 128 + (aByte &  0x7F);

                //Add the decoded number to the list of numbers
                numbers.add(number);

                //Reset the accumulator
                number = 0;
            }
        }

        //Return the list of numbers
        return numbers;
    }

    /**
     * Decodes the given byte array containing Variable Byte Encoding of a list of longs,
     * returning the corresponding decompressed list of longs.
     * @param bytes Compressed list of longs.
     * @return Decompressed list of longs.
     */
    public static ArrayList<Long> variableByteDecodeLong(byte[] bytes){

        //Array storing the decoded numbers
        ArrayList<Long> numbers = new ArrayList<>();

        //Accumulator for the current decoded number
        long number = 0;

        // Iterate through each byte in the array
        for (byte aByte : bytes) {

            // Check if the MSB of the byte is 0 using the mask 0x80 = 1000 0000
            if ((aByte & 0x80) == 0x00) {
                //The MSB is 0, so are not at the end of the sequence of bytes of the code
                number = number * 128 + aByte;
            } else {
                //The MSB is 1, are at the end

                //Add to the accumulator number*128 + the integer value in aByte discarding the 1 in the MSB
                number = number * 128 + (aByte &  0x7F);

                //Add the decoded number to the list of numbers
                numbers.add(number);

                //Reset the accumulator
                number = 0;
            }
        }

        //Return the list of numbers
        return numbers;
    }
}