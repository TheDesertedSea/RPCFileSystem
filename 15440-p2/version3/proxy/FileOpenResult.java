/**
 * FileOpenResult.java
 * 
 * @author Cundao Yu <cundaoy@andrew.cmu.edu>
 */

/**
 * Result of open file operation
 */
public class FileOpenResult {
    /**
     * Result code
     */
    private int resCode;
    /**
     * {@link OpenFile}
     * Open file
     */
    private OpenFile openFile;

    /**
     * Constructor
     * 
     * @param resCode Result code
     * @param openFile {@link OpenFile} Open file
     */
    public FileOpenResult(int resCode, OpenFile openFile) {
        this.resCode = resCode;
        this.openFile = openFile;
    }

    /**
     * Get result code
     * 
     * @return Result code
     */
    public int getResCode() {
        return resCode;
    }

    /**
     * Get open file
     * 
     * @return {@link OpenFile} Open file
     */
    public OpenFile getOpenFile() {
        return openFile;
    }

    public String toString(){
        return "----------FileOpenResult---------\n" + 
                "resCode: " + resCode + "\n" +
                "openFile: " + (openFile == null ? "" : openFile.toString()) + "\n" +
                "-------------------------------\n";
    }
}
