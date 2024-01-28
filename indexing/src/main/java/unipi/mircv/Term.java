package unipi.mircv;

public class Term {

    public int offsetDocId;
    public int offsetFreq;
    public int offsetLastDocIds;
    public int offsetSkipBlock;
    public int postingListLength;
    public float termUpperBound;

    public Term(int offsetDocId, int offsetFreq, int offsetLastDocIds, int offsetSkipBlock, int postingListLength, float termUpperBound) {
        this.offsetDocId = offsetDocId;
        this.offsetFreq = offsetFreq;
        this.offsetLastDocIds = offsetLastDocIds;
        this.offsetSkipBlock = offsetSkipBlock;
        this.postingListLength = postingListLength;
        this.termUpperBound = termUpperBound;
    }

    public float getTermUpperBound() {
        return termUpperBound;
    }

    public void setTermUpperBound(float termUpperBound) {
        this.termUpperBound = termUpperBound;
    }


    public int getPostingListLength() {
        return postingListLength;
    }

    public void setPostingListLength(int postingListLength) {
        this.postingListLength = postingListLength;
    }

    public int getOffsetDocId() {
        return offsetDocId;
    }

    public void setOffsetDocId(int offsetDocId) {
        this.offsetDocId = offsetDocId;
    }

    public int getOffsetFreq() {
        return offsetFreq;
    }

    public void setOffsetFreq(int offsetFreq) {
        this.offsetFreq = offsetFreq;
    }

    public int getOffsetSkipBlock() {
        return offsetSkipBlock;
    }

    public void setOffsetSkipBlock(int offsetSkipBlock) {
        this.offsetSkipBlock = offsetSkipBlock;
    }

    public int getOffsetLastDocIds() {
        return offsetLastDocIds;
    }
    public int getOffsetSkipPointers() {
        return offsetLastDocIds;
    }

    public void setOffsetLastDocIds(int offsetDocId) {
        this.offsetLastDocIds = offsetLastDocIds;
    }

    @Override
    public String toString() {
        return offsetDocId + " " + offsetFreq + " " + offsetLastDocIds + " "
                + offsetSkipBlock + " " + postingListLength + " " + termUpperBound;
    }
}


