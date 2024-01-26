package unipi.mircv;

import java.io.BufferedWriter;
import java.util.Scanner;

public class ManagerIO {

    //writers used to write to blocks during indexing and then for merging.
    public Writer myWriterLexicon;
    public Writer myWriterDocIds;
    public Writer myWriterFreq;

    public Writer myWriterDocumentIndex;
    public Writer myWriterLastDocIds;
    public Writer myWriterSkipPointers;

    public Writer getMyWriterLexicon() {
        return myWriterLexicon;
    }

    public void setMyWriterLexicon(Writer myWriterLexicon) {
        this.myWriterLexicon = myWriterLexicon;
    }

    public Writer getMyWriterDocIds() {
        return myWriterDocIds;
    }

    public void setMyWriterDocIds(Writer myWriterDocIds) {
        this.myWriterDocIds = myWriterDocIds;
    }

    public Writer getMyWriterFreq() {
        return myWriterFreq;
    }

    public void setMyWriterFreq(Writer myWriterFreq) {
        this.myWriterFreq = myWriterFreq;
    }

    public Writer getMyWriterDocumentIndex() {
        return myWriterDocumentIndex;
    }

    public void setMyWriterDocumentIndex(Writer myWriterDocumentIndex) {
        this.myWriterDocumentIndex = myWriterDocumentIndex;
    }

    public Writer getMyWriterLastDocIds() {
        return myWriterLastDocIds;
    }

    public void setMyWriterLastDocIds(Writer myWriterLastDocIds) {
        this.myWriterLastDocIds = myWriterLastDocIds;
    }

    public Writer getMyWriterSkipPointers() {
        return myWriterSkipPointers;
    }

    public void setMyWriterSkipPointers(Writer myWriterSkipPointers) {
        this.myWriterSkipPointers = myWriterSkipPointers;
    }



    //readers used during the merging phase
        TextReader[] lexiconScanners;
        Reader[] docIdsScanners;
        Reader[] freqScanners;
        Reader[] documentIndexScanners;

        //readers used during the query processing lookup phase
        Reader lexiconReader;
        Reader docIdsReader;
        Reader freqReader;
        Reader documentIndexReader;
        Reader collectionStatisticsReader;
        Reader lastDocIdsReader;
        Reader skipPointersReader;


        public TextReader[] getLexiconScanners() {
            return lexiconScanners;
        }


        public Reader[] getDocIdsScanners() {
            return docIdsScanners;
        }


        public Reader[] getFreqScanners() {
            return freqScanners;
        }


        public Reader[] getDocumentIndexScanners() {
            return documentIndexScanners;
        }


        //function that opens the right block files during indexing
        public void openBlockFiles(int blockCounter, String encodingType) {
            TextWriter myWriterLexicon = new TextWriter("Output/Lexicon/lexicon" + blockCounter + ".txt");
            if (encodingType.equals("text")) {
                TextWriter myWriterDocIds = new TextWriter("Output/DocIds/docIds" + blockCounter + ".txt");
                TextWriter myWriterFreq = new TextWriter("Output/Frequencies/freq" + blockCounter + ".txt");
                TextWriter myWriterDocumentIndex = new TextWriter("Output/DocumentIndex/documentIndex" + blockCounter + ".txt");
            } else {
                Compressor compressor = new Compressor();
                ByteWriter myWriterDocIds = new ByteWriter("Output/DocIds/docIds" + blockCounter + ".dat", compressor);
                myWriterFreq = new ByteWriter("Output/Frequencies/freq" + blockCounter + ".dat", compressor);
                myWriterDocumentIndex = new ByteWriter("Output/DocumentIndex/documentIndex" + blockCounter + ".dat", compressor);
            }
        }

    //function that opens the final files for the merge phase.
    //Depending on the encoding it opens the right files.
    public void openMergeFiles(String encodingType) {
        myWriterLexicon = new TextWriter("Output/Lexicon/lexicon.txt");
        if (encodingType.equals("text")) {
            myWriterDocIds = new TextWriter("Output/DocIds/docIds.txt");
            myWriterFreq = new TextWriter("Output/Frequencies/freq.txt");
            myWriterDocumentIndex = new TextWriter("Output/DocumentIndex/documentIndex.txt");
            myWriterLastDocIds = new TextWriter("Output/Skipping/lastDocIds.txt");
            myWriterSkipPointers = new TextWriter("Output/Skipping/skipPointers.txt");
        } else {
            Compressor compressor = new Compressor();
            myWriterDocIds = new ByteWriter("Output/DocIds/docIds.dat", compressor);
            myWriterFreq = new ByteWriter("Output/Frequencies/freq.dat", compressor);
            myWriterDocumentIndex = new ByteWriter("Output/DocumentIndex/documentIndex.dat", compressor);
            myWriterLastDocIds = new ByteWriter("Output/Skipping/lastDocIds.dat", compressor);
            myWriterSkipPointers = new ByteWriter("Output/Skipping/skipPointers.dat", compressor);
        }
    }

        //function that closes block files during indexing
        public void closeBlockFiles() {
            myWriterDocIds.close();
            myWriterFreq.close();
            myWriterLexicon.close();
            myWriterDocumentIndex.close();
        }

        //function used to write to a file an int. The function accept a writer so it is independent of the encoding type
        public int writeOnFile(Writer writer, int number) {
            return writer.write(number);
        }

        //function used to specifically write a string to a text file.
        public void writeLineOnFile(TextWriter writer, String line) {
            writer.write(line);
        }

        //function used to read an int from a provided reader.
        public int readFromFile(Reader reader) {
            return reader.read();
        }

        //function used to specifically read a text line from a text file.
        public String readLineFromFile(TextReader reader) {
            return reader.readLine();
        }

        //function used to skip to a specific offset passed as int of the specified file.
        public void goToOffset(RandomByteReader file, int offset) {
            file.goToOffset(offset);
        }

        //function that opens the block scanners during the merging phase.
        //depending on the encoding used it opens the right files.
        public void openScanners(int blockCounter, String encodingType) {
            lexiconScanners = new TextReader[blockCounter];
            for (int i = 0; i < blockCounter; i++) {
                lexiconScanners[i] = new TextReader("Output/Lexicon/lexicon" + i + ".txt");
            }
            if (encodingType.equals("text")) {
                docIdsScanners = new TextReader[blockCounter];
                freqScanners = new TextReader[blockCounter];
                documentIndexScanners = new TextReader[blockCounter];
                for (int i = 0; i < blockCounter; i++) {
                    docIdsScanners[i] = new TextReader("Output/DocIds/docIds" + i + ".txt");
                    freqScanners[i] = new TextReader("Output/Frequencies/freq" + i + ".txt");
                    documentIndexScanners[i] = new TextReader("Output/DocumentIndex/documentIndex" + i + ".txt");
                }
            } else {
                Compressor compressor = new Compressor();
                docIdsScanners = new ByteReader[blockCounter];
                freqScanners = new ByteReader[blockCounter];
                documentIndexScanners = new ByteReader[blockCounter];
                for (int i = 0; i < blockCounter; i++) {
                    docIdsScanners[i] = new ByteReader("Output/DocIds/docIds" + i + ".dat", compressor);
                    freqScanners[i] = new ByteReader("Output/Frequencies/freq" + i + ".dat", compressor);
                    documentIndexScanners[i] = new ByteReader("Output/DocumentIndex/documentIndex" + i + ".dat", compressor);
                }
            }
        }

        //function that closes the scanners used during merging.
        public void closeScanners() {
            for (int i = 0; i < lexiconScanners.length; i++) {
                lexiconScanners[i].close();
                docIdsScanners[i].close();
                freqScanners[i].close();
                documentIndexScanners[i].close();
            }
        }



        //function that closes the merge files.
        public void closeMergeFiles() {
            myWriterDocIds.close();
            myWriterFreq.close();
            myWriterLexicon.close();
            myWriterDocumentIndex.close();
            myWriterLastDocIds.close();
            myWriterSkipPointers.close();
        }

        //function that opens the lookup files for the lookup phase.
        public void openLookupFiles() {
            Compressor compressor = new Compressor();
            docIdsReader = new RandomByteReader("Output/DocIds/docIds.dat", compressor);
            freqReader = new RandomByteReader("Output/Frequencies/freq.dat", compressor);
            lastDocIdsReader = new RandomByteReader("Output/Skipping/lastDocIds.dat", compressor);
            skipPointersReader = new RandomByteReader("Output/Skipping/skipPointers.dat", compressor);
        }

        //function that closes the lookup files.
        public void closeLookupFiles() {
            docIdsReader.close();
            freqReader.close();
            lastDocIdsReader.close();
            skipPointersReader.close();
        }

        public void openObtainFiles() {
            Compressor compressor = new Compressor();
            lexiconReader = new TextReader("Output/Lexicon/lexicon.txt");
            collectionStatisticsReader = new TextReader("Output/CollectionStatistics/collectionStatistics.txt");
            documentIndexReader = new ByteReader("Output/DocumentIndex/documentIndex.dat", compressor);
        }

        public void closeObtainFiles() {
            lexiconReader.close();
            collectionStatisticsReader.close();
            documentIndexReader.close();
        }

        //function that checks if a text file has a next line.
        public boolean hasNextLine(TextReader reader) {
            return reader.hasNextLine();
        }
}