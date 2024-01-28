package unipi.mircv;

public class Merger {

    //readers used during the merging phase
    TextReader[] lexiconScanners;
    TextReader[] docIdsTextScanners;
    TextReader[] freqTextScanners;
    TextReader[] documentIndexTextScanners;
    ByteReader[] docIdByteScanners;
    ByteReader[] freqByteScanners;
    ByteReader[] documentIndexByteScanners;

    //writers used to write to blocks during indexing and then for merging.
    public TextWriter lexiconWriter;
    public TextWriter docIdsTextWriter;
    public TextWriter freqTextWriter;
    public TextWriter docIndexTextWriter;
    public TextWriter lastDocIdsTextWriter;
    public TextWriter skipPointersTextWriter;

    public ByteWriter docIdsByteWriter;
    public ByteWriter freqByteWriter;
    public ByteWriter docIndexByteWriter;
    public ByteWriter lastDocIdsByteWriter;
    public ByteWriter skipPointersByteWriter;


    //function that implements the merging phase of the SPIMI algorithm.
    public void mergeByteBlocks(int blockCounter, String encodingType, Statistics statistics) {

        int localPostingListLength;
        int postingListLength;
        int offsetDocIds = 0;
        int offsetFreq = 0;
        int offsetLastDocIds = 0;
        int offsetSkipPointers = 0;
        int docId = 0;
        float termUpperBound;
        float maxTermFrequency;
        float localTermFrequency;
        float tf;
        float idf;
        String minTerm;

        String[][] terms = new String[blockCounter][]; //terms read from the current pointer in the lexicon
        boolean[] scannerToRead = new boolean[blockCounter]; //array of boolean to indicate if a scanner to a file need to be read.
        boolean[] scannerFinished = new boolean[blockCounter]; //array of boolean to indicate if a scanner has scanned al the block file.

        int postingBlockCounter;
        for (int i = 0; i < blockCounter; i++) {
            scannerToRead[i] = true;
            scannerFinished[i] = false;
        }

        openScanners(blockCounter, encodingType); //open the scanners of the block files

        openMergeFiles(encodingType); //open the final marge files

        //in this for we do the merging of the document index first. It reads a number to check if the file is ended.
        //Every row of the document index is saved as 3 integers.
        for (int i = 0; i < blockCounter; i++) {
            int number = documentIndexByteScanners[i].read(); //read the first integer
            while (number != -1) { //continue until the file is not ended
                docIndexByteWriter.write(number); //write on the final file the read integer
                for (int j = 0; j < 2; j++) // reads other 2 times an integer from the current block file and writes it to the final file
                {
                    docIndexByteWriter.write(documentIndexByteScanners[i].read());
                }
                number = documentIndexByteScanners[i].read();
            }
        }
        //here the merging loop of the lexicon and of the documentIds and frequencies files is performed.
        while (true) {
            //it read from the lexicon files that needs to be read.
            advancePointers(lexiconScanners, scannerToRead, terms, scannerFinished,blockCounter);
            //it checks if the merging phase is finished.
            if (!continueMerging(scannerFinished,blockCounter)) {
                break;
            }
            //it gets the minimum term.
            minTerm = minTerm(terms, scannerFinished,blockCounter);
            postingListLength = 0;
            postingBlockCounter = 0;
            maxTermFrequency = 0;
            //it writes the term information to the lexicon in text format.
            lexiconWriter.write(minTerm + " "
                    + offsetDocIds + " " + offsetFreq + " " + offsetLastDocIds + " " + offsetSkipPointers + " ");
            //for every block if the current pointed term is the minimum term we perform merging.
            for (int i = 0; i < blockCounter; i++) {
                if (terms[i][0].equals(minTerm)) {
                    scannerToRead[i] = true; //we are using the information so the next time we need to read new information.
                    //obtain the posting list length of the current block
                    localPostingListLength = Integer.parseInt(terms[i][5]);
                    //update the global posting list length
                    postingListLength += localPostingListLength;
                    localTermFrequency = Float.parseFloat(terms[i][6]);
                    if (localTermFrequency > maxTermFrequency) maxTermFrequency = localTermFrequency;
                    for (int j = 0; j < localPostingListLength; j++) {
                        //if it is at the start of the posting list block it saves the skip pointers for the block
                        if (postingBlockCounter == 0) {
                            //it saves in the skiPointers file 2 integers: one for the docId offset and one for the frequency offset.
                            offsetSkipPointers += skipPointersByteWriter.write(offsetDocIds);
                            offsetSkipPointers += skipPointersByteWriter.write(offsetFreq);
                        }

                        docId = docIdByteScanners[i].read();
                        //it saves in the final files the information arriving from the block files.
                        offsetDocIds += docIdsByteWriter.write(docId);
                        offsetFreq += freqByteWriter.write(freqByteScanners[i].read());

                        postingBlockCounter += 1;
                        //if we are at the end of the posting list block we save the current docId in the lastDocId file.
                        if (postingBlockCounter == postingListLength) {
                            offsetLastDocIds += lastDocIdsByteWriter.write(docId);
                            postingBlockCounter = 0;
                        }
                    }
                } else {
                    //if the current term of the lexicon pointer is not the min term the next time we do not need to read
                    //from the scanner again
                    scannerToRead[i] = false;
                }
            }
            //at the end of the merging for a specific term we save the docId of the last posting of the posting list of that term.
            if (postingBlockCounter != postingListLength) {
                offsetLastDocIds += lastDocIdsByteWriter.write(docId);
            }
            //we conclude the lexicon merging adding the global posting list length and the term upper bound information.
            tf = (float) (1 + Math.log(maxTermFrequency));
            idf = (float) Math.log((double) statistics.getNDocs() / postingListLength);
            termUpperBound = tf * idf;
            lexiconWriter.write(postingListLength + " "
                    + termUpperBound + "\n");
        }

        // close byte scanners
        for (int i = 0; i < lexiconScanners.length; i++) {
                lexiconScanners[i].close();
                docIdByteScanners[i].close();
                freqByteScanners[i].close();
                documentIndexByteScanners[i].close();
            }

        //close merged files writers
            docIdsByteWriter.close();
            freqByteWriter.close();
            lexiconWriter.close();
            docIndexByteWriter.close();
            lastDocIdsByteWriter.close();
            skipPointersByteWriter.close();
    }

    public void mergeTextBlocks(int blockCounter, String encodingType, Statistics statistics) {

        int localPostingListLength;
        int postingListLength;
        int offsetDocIds = 0;
        int offsetFreq = 0;
        int offsetLastDocIds = 0;
        int offsetSkipPointers = 0;
        int docId = 0;
        float termUpperBound;
        float maxTermFrequency;
        float localTermFrequency;
        float tf;
        float idf;
        String minTerm;

        String[][] terms = new String[blockCounter][]; //terms read from the current pointer in the lexicon
        boolean[] scannerToRead = new boolean[blockCounter]; //array of boolean to indicate if a scanner to a file need to be read.
        boolean[] scannerFinished = new boolean[blockCounter]; //array of boolean to indicate if a scanner has scanned al the block file.

        int postingBlockCounter;
        for (int i = 0; i < blockCounter; i++) {
            scannerToRead[i] = true;
            scannerFinished[i] = false;
        }

        openScanners(blockCounter, encodingType); //open the scanners of the block files

        openMergeFiles(encodingType); //open the final marge files

        //in this for we do the merging of the document index first. It reads a number to check if the file is ended.
        //Every row of the document index is saved as 3 integers.
        for (int i = 0; i < blockCounter; i++) {
            int number = documentIndexTextScanners[i].read(); //read the first integer
            while (number != -1) { //continue until the file is not ended
                docIndexTextWriter.write(number); //write on the final file the read integer
                for (int j = 0; j < 2; j++) // reads other 2 times an integer from the current block file and writes it to the final file
                {
                    docIndexTextWriter.write(documentIndexTextScanners[i].read());
                }
                number = documentIndexTextScanners[i].read();
            }
        }
        //here the merging loop of the lexicon and of the documentIds and frequencies files is performed.
        while (true) {
            //it read from the lexicon files that needs to be read.
            advancePointers(lexiconScanners, scannerToRead, terms, scannerFinished,blockCounter);
            //it checks if the merging phase is finished.
            if (!continueMerging(scannerFinished,blockCounter)) {
                break;
            }
            //it gets the minimum term.
            minTerm = minTerm(terms, scannerFinished,blockCounter);
            postingListLength = 0;
            postingBlockCounter = 0;
            maxTermFrequency = 0;
            //it writes the term information to the lexicon in text format.
            lexiconWriter.write(minTerm + " "
                    + offsetDocIds + " " + offsetFreq + " " + offsetLastDocIds + " " + offsetSkipPointers + " ");
            //for every block if the current pointed term is the minimum term we perform merging.
            for (int i = 0; i < blockCounter; i++) {
                if (terms[i][0].equals(minTerm)) {
                    scannerToRead[i] = true; //we are using the information so the next time we need to read new information.
                    //obtain the posting list length of the current block
                    localPostingListLength = Integer.parseInt(terms[i][5]);
                    //update the global posting list length
                    postingListLength += localPostingListLength;
                    localTermFrequency = Float.parseFloat(terms[i][6]);
                    if (localTermFrequency > maxTermFrequency) maxTermFrequency = localTermFrequency;
                    for (int j = 0; j < localPostingListLength; j++) {
                        //if it is at the start of the posting list block it saves the skip pointers for the block
                        if (postingBlockCounter == 0) {
                            //it saves in the skiPointers file 2 integers: one for the docId offset and one for the frequency offset.
                            offsetSkipPointers += skipPointersTextWriter.write(offsetDocIds);
                            offsetSkipPointers += skipPointersTextWriter.write(offsetFreq);
                        }

                        docId = docIdsTextScanners[i].read();
                        //it saves in the final files the information arriving from the block files.
                        offsetDocIds += docIdsTextWriter.write(docId);
                        offsetFreq += freqTextWriter.write(freqTextScanners[i].read());

                        postingBlockCounter += 1;
                        //if we are at the end of the posting list block we save the current docId in the lastDocId file.
                        if (postingBlockCounter == postingListLength) {
                            offsetLastDocIds += lastDocIdsTextWriter.write(docId);
                            postingBlockCounter = 0;
                        }
                    }
                } else {
                    //if the current term of the lexicon pointer is not the min term the next time we do not need to read
                    //from the scanner again
                    scannerToRead[i] = false;
                }
            }
            //at the end of the merging for a specific term we save the docId of the last posting of the posting list of that term.
            if (postingBlockCounter != postingListLength) {
                offsetLastDocIds += lastDocIdsTextWriter.write(docId);
            }
            //we conclude the lexicon merging adding the global posting list length and the term upper bound information.
            tf = (float) (1 + Math.log(maxTermFrequency));
            idf = (float) Math.log((double) statistics.getNDocs() / postingListLength);
            termUpperBound = tf * idf;
            lexiconWriter.write(postingListLength + " "
                    + termUpperBound + "\n");
        }

        // close byte scanners
        for (int i = 0; i < lexiconScanners.length; i++) {
            lexiconScanners[i].close();
            docIdsTextScanners[i].close();
            freqTextScanners[i].close();
            documentIndexTextScanners[i].close();
        }

        //close merged files writers
        docIdsTextWriter.close();
        freqTextWriter.close();
        lexiconWriter.close();
        docIndexTextWriter.close();
        lastDocIdsTextWriter.close();
        skipPointersTextWriter.close();
    }


    //function to check if we need to continue the merging phase.
    public boolean continueMerging(boolean[] scannerFinished,int blockCounter) {
        boolean continueMerging;
        continueMerging = false;
        for (int i = 0; i < blockCounter; i++) {
            if (!scannerFinished[i]) {
                continueMerging = true;
                break;
            }
        }
        return continueMerging;
    }

    //function that advance the right pointers during the merge phase.
    public void advancePointers(TextReader[] lexiconScanners, boolean[] scannerToRead, String[][] terms,
                                boolean[] scannerFinished,int blockCounter) {
        for (int i = 0; i < blockCounter; i++) {
            if (scannerToRead[i]) {
                if (lexiconScanners[i].hasNextLine()) {
                    terms[i] = lexiconScanners[i].readLine().split(" ");
                } else {
                    scannerFinished[i] = true;
                }
            }
        }
    }

    //function that given the terms currently pointed during the merging phase returns the minimum term.
    public String minTerm(String[][] terms, boolean[] scannerFinished,int blockCounter) {
        String minTerm = "}";
        for (int i = 0; i < blockCounter; i++) {
            if (terms[i][0].compareTo(minTerm) < 0 && !scannerFinished[i]) {
                minTerm = terms[i][0];
            }
        }
        return minTerm;
    }

    //function that opens the block scanners during the merging phase.
    //depending on the encoding used it opens the right files.
    public void openScanners(int blockCounter, String encodingType) {
        lexiconScanners = new TextReader[blockCounter];
        for (int i = 0; i < blockCounter; i++) {
            lexiconScanners[i] = new TextReader("Output/Lexicon/lexicon" + i + ".txt");
        }
        if (encodingType.equals("text")) {
            docIdsTextScanners = new TextReader[blockCounter];
            freqTextScanners = new TextReader[blockCounter];
            documentIndexTextScanners = new TextReader[blockCounter];
            for (int i = 0; i < blockCounter; i++) {
                docIdsTextScanners[i] = new TextReader("Output/DocIds/docIds" + i + ".txt");
                freqTextScanners[i] = new TextReader("Output/Frequencies/freq" + i + ".txt");
                documentIndexTextScanners[i] = new TextReader("Output/DocumentIndex/documentIndex" + i + ".txt");
            }
        } else {
            Compressor compressor = new Compressor();
            docIdByteScanners = new ByteReader[blockCounter];
            freqByteScanners = new ByteReader[blockCounter];
            documentIndexByteScanners = new ByteReader[blockCounter];
            for (int i = 0; i < blockCounter; i++) {
                docIdByteScanners[i] = new ByteReader("Output/DocIds/docIds" + i + ".dat", compressor);
                freqByteScanners[i] = new ByteReader("Output/Frequencies/freq" + i + ".dat", compressor);
                documentIndexByteScanners[i] = new ByteReader("Output/DocumentIndex/documentIndex" + i + ".dat", compressor);
            }
        }
    }

    //function that opens the final files for the merge phase.
    //Depending on the encoding it opens the right files.
    public void openMergeFiles(String encodingType) {
        lexiconWriter = new TextWriter("Output/Lexicon/lexicon.txt");
        if (encodingType.equals("text")) {
            docIdsTextWriter = new TextWriter("Output/DocIds/docIds.txt");
            freqTextWriter = new TextWriter("Output/Frequencies/freq.txt");
            docIndexTextWriter = new TextWriter("Output/DocumentIndex/documentIndex.txt");
            lastDocIdsTextWriter= new TextWriter("Output/Skipping/lastDocIds.txt");
            skipPointersTextWriter = new TextWriter("Output/Skipping/skipPointers.txt");
        } else {
            Compressor compressor = new Compressor();
            docIdsByteWriter = new ByteWriter("Output/DocIds/docIds.dat", compressor);
            freqByteWriter = new ByteWriter("Output/Frequencies/freq.dat", compressor);
            docIndexByteWriter = new ByteWriter("Output/DocumentIndex/documentIndex.dat", compressor);
            lastDocIdsByteWriter = new ByteWriter("Output/Skipping/lastDocIds.dat", compressor);
            skipPointersByteWriter = new ByteWriter("Output/Skipping/skipPointers.dat", compressor);
        }
    }

}

