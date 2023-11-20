package indexing;

public class DocInfo {

    private String docNo;
    private int docLen;
    private String url;
    private float pr_score;

    public DocInfo(String docNo, int docLen, String url, float pr_score) {
        this.docNo = docNo;
        this.docLen = docLen;
        this.url = url;
        this.pr_score = pr_score;
    }

    public String getDocNo() {
        return docNo;
    }

    public int getDocLen() {
        return docLen;
    }

    public String getUrl() {
        return url;
    }

    public float getPr_score() {
        return pr_score;
    }
}
