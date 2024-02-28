import java.io.Serializable;

public class FileRemoveResult implements Serializable{
    int resCode;
    String relativePath;

    public FileRemoveResult(int resCode, String relativePath) {
        this.resCode = resCode;
        this.relativePath = relativePath;
    }

    public int getResCode() {
        return resCode;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public void setResCode(int rescode) {
        this.resCode = rescode;
    }

    public void setRelativePath(String normalizedPath) {
        this.relativePath = normalizedPath;
    }

    public String toString() {
        return "-------------------\n" +
                "rescode: " + resCode + "\n" +
                "normalizedPath: " + (relativePath == null ? "" : relativePath) + "\n" +
                "-------------------\n";
    }
}
