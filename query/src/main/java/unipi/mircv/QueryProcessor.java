package unipi.mircv;

import javax.print.Doc;
import java.util.*;

public class QueryProcessor {
    public ManagerIO managerIO;
    public Lexicon lexicon;
    public Statistics statistics;
    public DocIndex docIndex;
    public int postingListLength;


    public QueryProcessor(){
        this.managerIO = new ManagerIO();
        this.lexicon = new Lexicon();
        this.docIndex = new DocIndex();

        managerIO.openLookupFiles();
        managerIO.openObtainFiles();
        obtainLexicon(lexicon, managerIO);
        obtainCollectionStatistics();
        obtainDocumentIndex();
        managerIO.closeObtainFiles();
        postingListLength = 500;
    }

    //function that given a query returns a hashmap between the term and the relative whole posting list.
    public HashMap<String, ArrayList<Posting>> lookup(String[] queryTerms){
        int offsetDocId;
        int offsetFreq;
        int postingListLength;
        int docId;
        int freq;
        HashMap<String, ArrayList<Posting>> postingLists = new HashMap<>();
        Set<String> queryTermsSet = new HashSet<>(List.of(queryTerms));
        for(String term : queryTermsSet){
            try {
                //it gets the offset information from the lexicon to read in the docIds and freq files.
                offsetDocId = lexicon.getLexicon().get(term).getOffsetDocId();
                offsetFreq = lexicon.getLexicon().get(term).getOffsetFreq();
                postingListLength = lexicon.getLexicon().get(term).getPostingListLength();
                managerIO.goToOffset((RandomByteReader) managerIO.getDocIdsReader(), offsetDocId);
                managerIO.goToOffset((RandomByteReader) managerIO.getFreqReader(), offsetFreq);
                for (int i = 0; i < postingListLength; i++) {
                    //for the length of the posting list it reads docId and frequency from the relative files
                    //and adds a posting to the relative posting list.
                    docId = managerIO.readFromFile(managerIO.getDocIdsReader());
                    freq = managerIO.readFromFile(managerIO.getFreqReader());
                    addPosting(postingLists, term, docId, freq);
                }
            }catch (NullPointerException e){
                postingLists.put(term, new ArrayList<>());
                lexicon.addInformation(term, 0, 0, 0, 0, 0, 0);
            }
        }
        return postingLists;
    }

    //lookup function that given a query returns a hashmap between the term and the first block on the relative posting list
    public HashMap<String, ArrayList<Posting>> initialLookUp(String[] queryTerms){
        int offsetDocId;
        int offsetFreq;
        int postingListLength;
        int docId;
        int freq;
        int postingToRead;
        HashMap<String, ArrayList<Posting>> postingLists = new HashMap<>();
        Set<String> queryTermsSet = new HashSet<>(List.of(queryTerms));
        for(String term : queryTermsSet){
            try {
                //it gets the offset information from the lexicon to read in the docIds and freq files.
                offsetDocId = lexicon.getLexicon().get(term).getOffsetDocId();
                offsetFreq = lexicon.getLexicon().get(term).getOffsetFreq();
                postingListLength = lexicon.getLexicon().get(term).getPostingListLength();
                managerIO.goToOffset((RandomByteReader) managerIO.getDocIdsReader(), offsetDocId);
                managerIO.goToOffset((RandomByteReader) managerIO.getFreqReader(), offsetFreq);
                postingToRead = Math.min(postingListLength, postingListBlockLength);
                for (int i = 0; i < postingToRead; i++) {
                    //for the number of posting to read it reads docId and frequency from the relative files
                    //and adds a posting to the relative posting list.
                    docId = managerIO.readFromFile(managerIO.getDocIdsReader());
                    freq = managerIO.readFromFile(managerIO.getFreqReader());
                    addPosting(postingLists, term, docId, freq);
                }
            }catch (NullPointerException e){
                postingLists.put(term, new ArrayList<>());
                lexicon.addInformation(term, 0, 0, 0, 0, 0, 0);
            }
        }
        return postingLists;
    }

    //function that given a term and a doc id returns a hashmap between the term and the posting list block containing
    //that docId.
    public HashMap<String, ArrayList<Posting>> lookupDocId(String term, int docId){
        HashMap<String, ArrayList<Posting>> postingLists = new HashMap<>();
        int[] skipPointers = new int[4];
        int newDocId;
        int newFreq;
        int postingToRead;
        int postingListLength;
        int numberOfBlocks;
        searchBlock(skipPointers, term, docId); //search the block to read.
        //if a posting with a docId greater or equal to the one passed as argument doesn't exist the returned hash map is empty.
        if(skipPointers[3] == 0) return postingLists;
        managerIO.goToOffset((RandomByteReader) managerIO.getDocIdsReader(), skipPointers[0]);
        managerIO.goToOffset((RandomByteReader) managerIO.getFreqReader(), skipPointers[1]);
        postingListLength = lexicon.getLexicon().get(term).getPostingListLength();
        //skiPointers[2] == 0 if the docId is not contained in the last block of the posting list, 1 otherwise.
        if(skipPointers[2] == 0){
            postingToRead = Math.min(postingListLength, postingListLength);
        }
        else {
            //if the posting containing the docId is the last block of the posting list it computes the right
            //length to read.
            numberOfBlocks = (lexicon.getLexicon().get(term).getPostingListLength() / postingListLength) + 1;
            postingToRead = postingListLength - (numberOfBlocks - 1) * postingListLength;
        }
        for(int i = 0; i<postingToRead; i++){
            //for the number of posting to read it reads docId and frequency from the relative files
            //and adds a posting to the relative posting list.
            newDocId = managerIO.readFromFile(managerIO.getDocIdsReader());
            newFreq = managerIO.readFromFile(managerIO.getFreqReader());
            addPosting(postingLists, term, newDocId, newFreq);
        }
        return postingLists;
    }

    //function that given a term and a docId returns the skip pointers to the relative block of the term's posting list
    //containing that docId.
    public void searchBlock(int[] skipPointers, String term, int docId){
        ArrayList<Integer> pointersDocIds = new ArrayList<>();
        ArrayList<Integer> pointerFreq = new ArrayList<>();
        ArrayList<Integer> docIds = new ArrayList<>();
        int offsetLastDocIds;
        int offsetSkipPointers;
        //it gets the number of blocks of the term's posting list
        int blockNumber = (lexicon.getLexicon().get(term).getPostingListLength() / postingListLength) + 1;
        offsetLastDocIds = lexicon.getLexicon().get(term).getOffsetLastDocIds();
        offsetSkipPointers = lexicon.getLexicon().get(term).getOffsetSkipBlock();
        managerIO.goToOffset((RandomByteReader) managerIO.getLastDocIdsReader(), offsetLastDocIds);
        managerIO.goToOffset((RandomByteReader) managerIO.getSkipPointersReader(), offsetSkipPointers);
        for(int i = 0; i<blockNumber; i++){
            //for the number of blocks it reads and add to the relative array the posting list block information.
            docIds.add(managerIO.readFromFile(managerIO.getLastDocIdsReader()));
            pointersDocIds.add(managerIO.readFromFile(managerIO.getSkipPointersReader()));
            pointerFreq.add(managerIO.readFromFile(managerIO.getSkipPointersReader()));
        }
        for(int i = 0; i<docIds.size(); i++){
            //it searches for the block containing the posting with the docId grater or equal to the one passed as argument.
            //if a posting with a docId greater than the docId passed is not present in the posting list, skipPointers[3] is put equal 0.
            if(i == 0){
                if(docId < docIds.get(i)){
                    skipPointers[0] = pointersDocIds.get(i);
                    skipPointers[1] = pointerFreq.get(i);
                    skipPointers[2] = 0;
                    skipPointers[3] = 1;
                    return;
                }
            }
            else{
                if(docId <= docIds.get(i) && docId > docIds.get(i-1)){
                    skipPointers[0] = pointersDocIds.get(i);
                    skipPointers[1] = pointerFreq.get(i);
                    skipPointers[2] = 0;
                    if(i == (docIds.size() - 1)){
                        skipPointers[2] = 1; //this is the last block of the posting list
                    }
                    skipPointers[3] = 1;
                    return;
                }
                else{
                    skipPointers[3] = 0; //no posting with a docId greater or equal to the one passed is found.
                }
            }
        }
    }

    //function to add a posting to the posting list of a term.
    public void addPosting(HashMap<String, ArrayList<Posting>> postingLists, String term, int docId, int freq){
        if (!postingLists.containsKey(term)){
            postingLists.put(term, new ArrayList<>());
        }
        postingLists.get(term).add(new Posting(docId, freq));
    }

    //function the load in main memory the lexicon from the disk.
    public static void obtainLexicon(Lexicon lexicon, ManagerIO managerIO) {
        String line;
        String[] terms;
        while (managerIO.hasNextLine((TextReader) managerIO.getLexiconReader())) {
            line = managerIO.readLineFromFile((TextReader) managerIO.getLexiconReader());
            terms = line.split(" ");
            lexicon.addInformation(terms[0], Integer.parseInt(terms[1]), Integer.parseInt(terms[2]),
                    Integer.parseInt(terms[3]), Integer.parseInt(terms[4]), Integer.parseInt(terms[5]), Float.parseFloat(terms[6]));
        }
    }

    //function to load in main memory the document index from the disk
    public void obtainDocumentIndex(){
        int docId;
        int docNo;
        int size;
        for(int i = 0; i<statistics.getNDocs(); i++){
            //for the number of document it reads 3 integer from the disk file and add a new document information to the document index
            docId = managerIO.readFromFile(managerIO.getDocumentIndexReader());
            docNo = managerIO.readFromFile(managerIO.getDocumentIndexReader());
            size = managerIO.readFromFile(managerIO.getDocumentIndexReader());
            docIndex.addDocument(docId, docNo, size);
        }
    }

    //function to load in main memory the collection statistics from the disk.
    public void obtainCollectionStatistics(){
        String[] terms;
        terms = managerIO.readLineFromFile((TextReader) managerIO.getCollectionStatisticsReader()).split(" ");
        statistics = new Statistics(Integer.parseInt(terms[0]), Double.parseDouble(terms[1]),
                lexicon.getLexicon().size(), Integer.parseInt(terms[2]));
    }

    public Statistics getStatistics() {
        return statistics;
    }

    public DocIndex getDocIndex() {
        return docIndex;
    }

    public ManagerIO getManagerIO() {
        return managerIO;
    }

    public Lexicon getLexicon() {
        return lexicon;
    }

    public void setLexicon(Lexicon lexicon) {
        this.lexicon = lexicon;
    }

    //function to load the next block of a posting list.
    public HashMap<String, ArrayList<Posting>> loadNextBlock(String term, int docId){
        HashMap<String, ArrayList<Posting>> postingLists = new HashMap<>();
        int[] skipPointers = new int[3];
        int postingListLength;
        int postingToRead;
        int numberOfBlocks;
        int newFreq;
        int newDocId;
        searchNextBlock(skipPointers, term, docId);
        if (skipPointers[0] == 0) return postingLists;
        managerIO.goToOffset((RandomByteReader) managerIO.getDocIdsReader(), skipPointers[0]);
        managerIO.goToOffset((RandomByteReader) managerIO.getFreqReader(), skipPointers[1]);
        postingListLength = lexicon.getLexicon().get(term).getPostingListLength();
        //skiPointers[2] == 0 if the docId is not contained in the last block of the posting list, 1 otherwise.
        if(skipPointers[2] == 0){
            postingToRead = Math.min(postingListLength, postingListLength);
        }
        else {
            //if the posting containing the docId is the last block of the posting list it computes the right
            //length to read.
            numberOfBlocks = (lexicon.getLexicon().get(term).getPostingListLength() / postingListLength) + 1;
            postingToRead = postingListLength - (numberOfBlocks - 1) * postingListLength;
        }
        for(int i = 0; i<postingToRead; i++){
            //for the number of posting to read it reads docId and frequency from the relative files
            //and adds a posting to the relative posting list.
            newDocId = managerIO.readFromFile(managerIO.getDocIdsReader());
            newFreq = managerIO.readFromFile(managerIO.getFreqReader());
            addPosting(postingLists, term, newDocId, newFreq);
        }
        return postingLists;
    }

    //function to search the skipPointers for the next posting list block
    public void searchNextBlock(int[] skipPointers, String term, int docId){
        ArrayList<Integer> pointersDocIds = new ArrayList<>();
        ArrayList<Integer> pointerFreq = new ArrayList<>();
        ArrayList<Integer> docIds = new ArrayList<>();
        int offsetLastDocIds;
        int offsetSkipPointers;
        //it gets the number of blocks of the term's posting list
        int blockNumber = (lexicon.getLexicon().get(term).getPostingListLength() / postingListLength) + 1;
        offsetLastDocIds = lexicon.getLexicon().get(term).getOffsetLastDocIds();
        offsetSkipPointers = lexicon.getLexicon().get(term).getOffsetSkipPointers();
        managerIO.goToOffset((RandomByteReader) managerIO.getLastDocIdsReader(), offsetLastDocIds);
        managerIO.goToOffset((RandomByteReader) managerIO.getSkipPointersReader(), offsetSkipPointers);
        for(int i = 0; i<blockNumber; i++){
            //for the number of blocks it reads and add to the relative array the posting list block information.
            docIds.add(managerIO.readFromFile(managerIO.getLastDocIdsReader()));
            pointersDocIds.add(managerIO.readFromFile(managerIO.getSkipPointersReader()));
            pointerFreq.add(managerIO.readFromFile(managerIO.getSkipPointersReader()));
        }

        for(int i = 0; i<docIds.size(); i++){
            if (docId == docIds.get(i)){
                //if the docId passed belongs to the last block it returns 0 0 as skipPointers
                if (i == (docIds.size() - 1)) return;
                //if the next block is the last block
                if (i == (docIds.size() - 2)) skipPointers[2] = 1;
                skipPointers[0] = pointersDocIds.get(i+1);
                skipPointers[1] = pointerFreq.get(i+1);
            }
        }
    }
}
