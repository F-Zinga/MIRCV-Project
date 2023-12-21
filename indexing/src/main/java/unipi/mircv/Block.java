package unipi.mircv;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

// skip block
public class Block {

    private long docIDOffset;
    private int docIDSize;
    private long frqOffset;
    private int frqSize;
    //maximum doc id in the block represented by this skipBlock.
    private long maxDocID;

    //private int nPostings;

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


    public Block(long docIDOffset, int docIDSize, long frqOffset, int frqSize, long maxDocID) {
        this.docIDOffset = docIDOffset;
        this.docIDSize = docIDSize;
        this.frqOffset = frqOffset;
        this.frqSize = frqSize;
        this.maxDocID = maxDocID;
    }

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
     * Write the term info to a file. This method is used during the merge of the partial blocks, here we have
     * all the information directly inside the termInfo object.
     * @param skipBlocksFile Is the random access file on which the term info is written.
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
