package unipi.mircv;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

// Represents a skip block used during the merging process
public class Block {

    private long docIDOffset; // Offset in the document IDs file where the skip block starts
    private int docIDSize; // Size of the skip block in the document IDs file
    private long frqOffset; // Offset in the frequencies file where the skip block starts
    private int frqSize; // Size of the skip block in the frequencies file
    private long maxDocID; //maximum doc id in the block represented by this skipBlock.


    public long getDocIDOffset() {
        return docIDOffset;
    }

    public void setDocIDOffset(long docIDOffset) {
        this.docIDOffset = docIDOffset;
    }

    public int getDocIDSize() {
        return docIDSize;
    }

    public void setDocIDSize(int docIDSize) {
        this.docIDSize = docIDSize;
    }

    public long getMaxDocID() {
        return maxDocID;
    }

    public void setMaxDocID(long maxDocID) {
        this.maxDocID = maxDocID;
    }



    public int getFrqSize() {
        return frqSize;
    }

    public void setFrqSize(int frqSize) {
        this.frqSize = frqSize;
    }

    public long getFrqOffset() {
        return frqOffset;
    }

    public void setFrqInfo(long frqOffset,int frqSize) {
        this.frqOffset = frqOffset;
        this.frqSize = frqSize;
    }

    /**
     * Constructor for creating a Block with detailed information.
     * @param docIDOffset The offset in the document IDs file.
     * @param docIDSize The size of the skip block in the document IDs file.
     * @param frqOffset The offset in the frequencies file.
     * @param frqSize The size of the skip block in the frequencies file.
     * @param maxDocID The maximum document ID in the block.
     */

    public Block(long docIDOffset, int docIDSize, long frqOffset, int frqSize, long maxDocID) {
        this.docIDOffset = docIDOffset;
        this.docIDSize = docIDSize;
        this.frqOffset = frqOffset;
        this.frqSize = frqSize;
        this.maxDocID = maxDocID;
    }

    /**
     * Constructor for creating a Block with minimal information.
     * @param docIDOffset The offset in the document IDs file.
     * @param docIDSize The size of the skip block in the document IDs file.
     * @param maxDocID The maximum document ID in the block.
     */
    public Block(long docIDOffset, int docIDSize, long maxDocID) {
        this.docIDOffset = docIDOffset;
        this.docIDSize = docIDSize;
        this.maxDocID = maxDocID;
    }

    @Override
    public String toString() {
        return "Block{" +
                "docIDOffset=" + docIDOffset +
                ", docIDSize=" + docIDSize +
                ", frqOffset=" + frqOffset +
                ", frqSize=" + frqSize +
                ", maxDocID=" + maxDocID +
                '}';
    }

    /**
     * Write the skip block information to a file. This method is used during the merge of partial blocks,
     * where all the information is available directly within the Block object.
     * @param skipBlocksFile The random access file where the skip block information is written.
     */
    public void writeToFile(RandomAccessFile skipBlocksFile){
        byte[] startDocIdOffset = ByteBuffer.allocate(Parameters.OFFSET_LENGTH).putLong(this.docIDOffset).array();
        byte[] skipBlockDocIdLength = ByteBuffer.allocate(Parameters.BLOCK_DIMENSION_LENGTH).putInt(this.docIDSize).array();
        byte[] startFreqOffset = ByteBuffer.allocate(Parameters.OFFSET_LENGTH).putLong(this.frqOffset).array();
        byte[] skipBlockFreqLength = ByteBuffer.allocate(Parameters.BLOCK_DIMENSION_LENGTH).putInt(this.frqSize).array();
        byte[] maxDocId = ByteBuffer.allocate(Parameters.MAX_DOC_ID_LENGTH).putLong(this.maxDocID).array();
        try {
            skipBlocksFile.write(startDocIdOffset);
            skipBlocksFile.write(skipBlockDocIdLength);
            skipBlocksFile.write(startFreqOffset);
            skipBlocksFile.write(skipBlockFreqLength);
            skipBlocksFile.write(maxDocId);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}
