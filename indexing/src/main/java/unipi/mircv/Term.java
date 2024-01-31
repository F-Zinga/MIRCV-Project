package unipi.mircv;

public class Term {

    public int offsetDocId;
    public int offsetFreq;
    public int offsetLastDocIds;
    public int offsetSkipPointer;
    public int postingListLength;
    public float termUpperBound;

    public Term(int offsetDocId, int offsetFreq, int offsetLastDocIds, int offsetSkipPointer, int postingListLength, float termUpperBound) {
        this.offsetDocId = offsetDocId;
        this.offsetFreq = offsetFreq;
        this.offsetLastDocIds = offsetLastDocIds;
        this.offsetSkipPointer = offsetSkipPointer;
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

    public int getOffsetFreq() {
        return offsetFreq;
    }


    public int getOffsetLastDocIds() {
        return offsetLastDocIds;
    }
    public int getOffsetSkipPointers() {
        return offsetSkipPointer;
    }

    public void setOffsetLastDocIds(int offsetDocId) {
        this.offsetLastDocIds = offsetLastDocIds;
    }

    @Override
    public String toString() {
        return offsetDocId + " " + offsetFreq + " " + offsetLastDocIds + " "
                + offsetSkipPointer + " " + postingListLength + " " + termUpperBound;
    }
}


