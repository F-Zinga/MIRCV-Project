package unipi.mircv;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.util.HashMap;

/**
 * Represents the lexicon of terms used in the inverted index. Extends HashMap to provide a mapping from terms to their corresponding Term objects.
 */
public class Lexicon {

    //the lexicon is represented as a hashmap  between a term and the posting list information.
    private HashMap<String, Term> lexicon = new HashMap<>();

    public class Term{
        public int offsetDocId;
        public int offsetFreq;
        public int offsetLastDocIds;
        public int offsetSkipBlock;
        public int postingListLength;
        public float bm25termUpperBound;
        public float tfidftermUpperBound;

        public Term(int offsetDocId, int offsetFreq, int offsetLastDocIds, int offsetSkipBlock, int postingListLength, float bm25termUpperBound, float tfidftermUpperBound) {
            this.offsetDocId = offsetDocId;
            this.offsetFreq = offsetFreq;
            this.offsetLastDocIds = offsetLastDocIds;
            this.offsetSkipBlock = offsetSkipBlock;
            this.postingListLength = postingListLength;
            this.bm25termUpperBound = bm25termUpperBound;
            this.tfidftermUpperBound = tfidftermUpperBound;
        }

        public float gettfidftermUpperBound() {
            return tfidftermUpperBound;
        }

        public void settfidftermUpperBound(int tfidftermUpperBound) {
            this.tfidftermUpperBound = tfidftermUpperBound;
        }

        public float getbm25termUpperBound() {
            return bm25termUpperBound;
        }

        public void setbm25termUpperBound(int bm25TermUpperBound) {
            this.bm25termUpperBound = bm25termUpperBound;
        }

        public int getPostingListLength() {
            return postingListLength;
        }

        public void setPostingListLength(int postingListLength) {
            this.postingListLength = postingListLength;
        }

        public int getoffsetDocId() {
            return offsetDocId;
        }

        public void setoffsetDocId(int offsetDocId) {
            this.offsetDocId = offsetDocId;
        }

        public int getoffsetFrequency() {
            return offsetFreq;
        }

        public void setoffsetFrequency(int offsetFrequency) {
            this.offsetFreq = offsetFreq;
        }

        public long getOffsetSkipBlock() {
            return offsetSkipBlock;
        }

        public void setOffsetSkipBlock(int offsetSkipBlock) {
            this.offsetSkipBlock = offsetSkipBlock;
        }

        public int getoffsetLastDocIds() {
            return offsetLastDocIds;
        }

        public void setoffsetLastDocIds(int offsetDocId) {
            this.offsetLastDocIds = offsetLastDocIds;
        }



    //TODO: in beans
    /**
     * Loads the lexicon from the specified lexicon file.
     */
    public void loadLexicon() {
            System.out.println("*** loadLexicon... ***");
            try {
                //Start the stream from the lexicon file
                lexiconFile = new RandomAccessFile(Parameters.LEXICON_PATH, "r");

                //Accumulator for the current offset in the file
                int offset = 0;

                //Accumulator for the current Term reading
                Term term;


                //System.out.print("Lenght of lexicon (bytes): " + lexiconFile.length());
                //While we are not at the end of the file
                while (offset < (lexiconFile.length()))
                {

                    //Read the next Term from the file starting at the current offset
                    term = readNextTerm(offset);

                    //If the Term is not null (no problem encountered, or we are no at the end of the file)
                    if (term!= null){

                        //Insert the Term into the HashMap
                        this.put(term.getTerm(), term);

                        //Increment the offset
                        offset += Parameters.TERM_INFO_LENGTH;
                    }
                    else{
                       // System.out.println(" *** NULL TERM ***");

                    }
                }

                //System.out.println(" ***loaded Lexicon ***");

            } catch (Exception e) {
                //System.out.println(" *** Error loading lexicon: " + e.getMessage());
                e.printStackTrace();
            }
        }

        /**
         * Read the next term from the lexicon file.
         * @param offset starting offset of the next term to be read
         * @return The next term from the lexicon file.
         */

        //TODO: in beans
        private Term readNextTerm(int offset) {
            //Array of bytes to store the term
            byte[] termBytes = new byte[Parameters.TERM_BYTES];

            //String containing the term
            String term;

            //Term containing the term information to be returned
            Term Term;

            try {
                //Set the file pointer to the start of the lexicon entry
                lexiconFile.seek(offset);

                //Read the first 48 containing the term
                lexiconFile.readFully(termBytes, 0, Parameters.TERM_BYTES);

                //Convert the bytes to a string and trim it
                term = new String(termBytes, Charset.defaultCharset()).trim();

                //Instantiate the Term object reading the next 3 integers from the file
                Term = new Term(term,   //Term
                        lexiconFile.readLong(),  //Offset docids file
                        lexiconFile.readLong(),  //Offset frequencies file
                        lexiconFile.readDouble(), //idf
                        lexiconFile.readInt(),  //Length in bytes of the docids list
                        lexiconFile.readInt(),  //Length in bytes of the frequencies list
                        lexiconFile.readInt(),  //Length of the term's posting list
                        lexiconFile.readLong(), //Offset of the skipBlocks in the skipBlocks file
                        lexiconFile.readInt(),  //Number of skipBlocks
                        lexiconFile.readInt(), //term upper bound (TFIDF)
                        lexiconFile.readInt()  //term lower bound (BM25)
                );


                return Term;

            } catch (IOException e) {
                return null;
            }
        }

    public void setLexicon(HashMap<String, Term> lexicon){ this.lexicon = lexicon; }
    public HashMap<String, Term> getLexicon(){ return lexicon; }

    public void addInformation(String term, int offsetDocIds, int offsetFreq, int offsetLastDocIds, int postingListLength, float tfidfTermUpperBound, float bm25TermUpperBound){
        if(!lexicon.containsKey(term)){
            lexicon.put(term, new Term(offsetDocIds, offsetFreq, offsetLastDocIds, postingListLength, tfidfTermUpperBound, bm25TermUpperBound));
        }
        else{ //it computes the highest term frequency
            lexicon.get(term).setbm25TermUpperBound(Math.max(bm25TermUpperBound, lexicon.get(term).getbm25TermUpperBound()));
            lexicon.get(term).settfidfTermUpperBound(Math.max(tfidfTermUpperBound, lexicon.get(term).gettfidfTermUpperBound()));
        }
    }

    //function that returns a list of sorted terms taken from the lexicon.
    public ArrayList<String> sortLexicon(){
        ArrayList<String> sortedTerms = new ArrayList<>(lexicon.keySet());
        Collections.sort(sortedTerms);
        return sortedTerms;
    }

}
