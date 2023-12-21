package unipi.mircv;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.util.HashMap;

public class Lexicon extends HashMap<String,Term> {

        //Object to open the stream from the lexicon file
        private RandomAccessFile lexiconFile;

        /**
         * Constructor, it uses the HashMap constructor.
         */
    public Lexicon() {
            super();
        }


    public void loadLexicon() {
            System.out.println("[LEXICON LOADER] Lexicon loading...");
            try {
                //Start the stream from the lexicon file
                lexiconFile = new RandomAccessFile(Parameters.LEXICON_PATH, "r");

                //Accumulator for the current offset in the file
                int offset = 0;

                //Accumulator for the current Term reading
                Term term;


                System.out.print("Lexicon lenght in byte: " + lexiconFile.length());
                //While we're not at the end of the file
                //TODO
                while (offset < (lexiconFile.length()))
                {

                    //Read the next Term from the file starting at the current offset
                    term = readNextTerm(offset);

                    //If the Term is not null (no problem encountered, or we aren't at the end of the file)
                    if (term!= null){

                        //Insert the Term into the HashMap
                        this.put(term.getTerm(), term);

                        //Increment the offset
                        offset += Parameters.TERM_INFO_LENGTH;
                    }
                    else{
                        System.out.println("NULL TERM");

                    }
                }

                System.out.println("[LEXICON LOADER] Lexicon loaded");

            } catch (Exception e) {
                System.out.println("[LEXICON LOADER] Error loading lexicon: " + e.getMessage());
                e.printStackTrace();
            }
        }

        /**
         * Read the next term from the lexicon file.
         * @param offset starting offset of the next term to be read
         * @return The next term from the lexicon file.
         */
        private Term readNextTerm(int offset) {
            //Array of bytes in which put the term
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
                        lexiconFile.readInt(), //TFIDF term upper bound
                        lexiconFile.readInt()  //BM25 term lower bound
                );


                return Term;

            } catch (IOException e) {
                //System.err.println("[ReadNextTerm] EOF reached while reading the next lexicon entry");
                return null;
            }
        }

}
