package unipi.mircv;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Represents a document index, extending HashMap to map document IDs to document information.
 */
public class DocIndex  {

    public HashMap<Integer, DocInfo> docIndex;

    public DocIndex(){
        docIndex = new HashMap<>();
    }

    public void setDocIndex(HashMap<Integer, DocInfo> docIndex){ this.docIndex = docIndex;}
    public HashMap<Integer, DocInfo> getDocIndex(){ return this.docIndex; }


    //function that adds a document to the document index.
    public void addDocument(int docId, int docNo, int docLen ){
        docIndex.put(docId, new DocInfo(docNo, docLen));
    }

    //function that returns a list of sorted integers representing the docIds of the document index
    public ArrayList<Integer> sortDocIndex(){
        ArrayList<Integer> sortedDocIds = new ArrayList<>(docIndex.keySet());
        Collections.sort(sortedDocIds);
        return sortedDocIds;
    }


}
