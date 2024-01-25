package unipi.mircv;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Represents a document index
 */
public class DocIndex  {

    //the documentIndex is a hashmap  between an int (docId) and the info of the document
    public HashMap<Integer, DocInfo> docIndex;

    public DocIndex(){
        docIndex = new HashMap<>();
    }

    /**
     * Sets the document index with a new HashMap of document information.
     *
     * @param docIndex The new document index HashMap.
     */
    public void setDocIndex(HashMap<Integer, DocInfo> docIndex){ this.docIndex = docIndex;}
    public HashMap<Integer, DocInfo> getDocIndex(){ return this.docIndex; }


    /**
     * Adds a document to the document index with the specified document ID, document number, and document length.
     *
     * @param docId   The document ID.
     * @param docNo   The document number.
     * @param docLen  The document length.
     */
    public void addDocument(int docId, int docNo, int docLen ){
        docIndex.put(docId, new DocInfo(docNo, docLen));
    }

    /**
     * Returns a list of sorted integers that represent the document IDs in the document index.
     *
     * @return A sorted list of document IDs.
     */
    public ArrayList<Integer> sortDocIndex(){
        ArrayList<Integer> sortedDocIds = new ArrayList<>(docIndex.keySet());
        Collections.sort(sortedDocIds);
        return sortedDocIds;
    }


}
