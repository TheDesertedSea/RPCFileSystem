/**
 * FileRemoveResult.java
 * 
 * @author Cundao Yu <cundaoy@andrew.cmu.edu>
 */
import java.io.Serializable;

/**
 * Result of file remove
 */
public class FileRemoveResult implements Serializable{
    /**
     * Result code
     */
    int resCode;
    /**
     * {@link String}
     * Relative path
     */
    String relativePath;

    /**
     * Constructor
     * 
     * @param resCode      Result code
     * @param relativePath {@link String} Relative path
     */
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
