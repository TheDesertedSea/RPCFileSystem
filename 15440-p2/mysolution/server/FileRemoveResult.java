public class FileRemoveResult implements java.io.Serializable{
    int rescode;
    String normalizedPath;

    public FileRemoveResult(int rescode, String normalizedPath) {
        this.rescode = rescode;
        this.normalizedPath = normalizedPath;
    }

    public int getRescode() {
        return rescode;
    }

    public String getNormalizedPath() {
        return normalizedPath;
    }

    public void setRescode(int rescode) {
        this.rescode = rescode;
    }

    public void setNormalizedPath(String normalizedPath) {
        this.normalizedPath = normalizedPath;
    }

    public String toString() {
        return "-------------------\n" +
                "rescode: " + rescode + "\n" +
                "normalizedPath: " + (normalizedPath == null ? "" : normalizedPath) + "\n" +
                "-------------------\n";
    }
}
