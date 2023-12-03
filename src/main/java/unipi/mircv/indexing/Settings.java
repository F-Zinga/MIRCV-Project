package unipi.mircv.indexing;

public class Settings {

    private boolean compressed;
    private boolean stemmingAndStopWords;

    public boolean isCompressed() {
        return compressed;
    }

    public void setCompressed(boolean compressed) {
        this.compressed = compressed;
    }

    public boolean isStemmingAndStopWords() {
        return stemmingAndStopWords;
    }

    public void setStemmingAndStopWords(boolean stemmingAndStopWords) {
        this.stemmingAndStopWords = stemmingAndStopWords;
    }
}
