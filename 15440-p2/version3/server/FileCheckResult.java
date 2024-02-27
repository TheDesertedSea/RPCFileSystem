
/**
 * FileCheckResult.java
 * 
 * @author Cundao Yu <cundaoy@andrew.cmu.edu>
 */
import java.io.Serializable;
import java.util.UUID;

/**
 * Result of file check on server
 */
public class FileCheckResult implements Serializable {
    /**
     * Result code
     */
    private int resCode;
    /**
     * {@link String}
     * Relative path
     */
    private String relativePath;
    /**
     * {@link UUID}
     * Version ID
     */
    private UUID verId;
    /**
     * {@link Boolean}
     * True if the file can be read
     */
    private Boolean canRead;
    /**
     * {@link Boolean}
     * True if the file can be written
     */
    private Boolean canWrite;
    /**
     * File descriptor of the file in the server
     */
    private int serverFd;
    /**
     * Size of the file
     */
    private long size;
    /**
     * First chunk of the file content
     */
    private byte[] firstChunk;

    /**
     * Constructor
     * 
     * @param resCode      Result code
     * @param relativePath {@link String} Relative path
     * @param verId        {@link UUID} Version ID
     * @param canRead      {@link Boolean} True if the file can be read
     * @param canWrite     {@link Boolean} True if the file can be written
     * @param serverFd     File descriptor of the file in the server
     * @param size         Size of the file
     * @param firstChunk   First chunk of the file content
     */
    public FileCheckResult(int resCode, String relativePath, UUID verId, Boolean canRead, Boolean canWrite,
            int serverFd, long size,
            byte[] firstChunk) {
        this.resCode = resCode;
        this.relativePath = relativePath;
        this.verId = verId;
        this.canRead = canRead;
        this.canWrite = canWrite;
        this.serverFd = serverFd;
        this.size = size;
        this.firstChunk = firstChunk;
    }

    public int getResCode() {
        return resCode;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public UUID getVerId() {
        return verId;
    }

    public Boolean getCanRead() {
        return canRead;
    }

    public Boolean getCanWrite() {
        return canWrite;
    }

    public int getServerFd() {
        return serverFd;
    }

    public long getSize() {
        return size;
    }

    public byte[] getFirstChunk() {
        return firstChunk;
    }

    public String toString() {
        return "----------FileCheckResult---------\n" +
                "resCode: " + resCode + "\n" +
                "path: " + (relativePath == null ? "" : relativePath) + "\n" +
                "version: " + (verId == null ? "" : verId.toString()) + "\n" +
                "canRead: " + canRead + "\n" +
                "canWrite: " + canWrite + "\n" +
                "serverFd: " + serverFd + "\n" +
                "size: " + size + "\n" +
                "-------------------------------\n";
    }
}
