import java.io.Serializable;
import java.util.UUID;

public class FileCheckResult implements Serializable{
    private int resCode;
    private String relativePath;
    private UUID verId;
    private Boolean canRead;
    private Boolean canWrite;
    private int serverFd;
    private long size;
    private byte[] firstChunk;

    public FileCheckResult(int resCode, String relativePath, UUID verId, Boolean canRead, Boolean canWrite, int serverFd, long size, 
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

    public String toString(){
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
