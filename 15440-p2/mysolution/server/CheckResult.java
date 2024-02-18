import java.io.Serializable;

public class CheckResult implements Serializable {
    private int result;
    private String normalizedPath;
    private Boolean exists;
    private long version;
    private Boolean isDirectory;
    private Boolean canRead;
    private Boolean canWrite;
    private int serverFd;

    public CheckResult(int result, String normalizedPath, Boolean exists, long version, Boolean isDirectory, Boolean canRead, Boolean canWrite, int serverFd) {
        this.result = result;
        this.normalizedPath = normalizedPath;
        this.exists = exists;
        this.version = version;
        this.isDirectory = isDirectory;
        this.canRead = canRead;
        this.canWrite = canWrite;
        this.serverFd = serverFd;
    }

    public int getResult() {
        return result;
    }

    public String getNormalizedPath() {
        return normalizedPath;
    }

    public Boolean getExists() {
        return exists;
    }

    public long getVersion() {
        return version;
    }

    public Boolean getIsDirectory() {
        return isDirectory;
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

    public void setResult(int result) {
        this.result = result;
    }

    public void setNormalizedPath(String normalizedPath) {
        this.normalizedPath = normalizedPath;
    }

    public void setExists(Boolean exists) {
        this.exists = exists;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public void setIsDirectory(Boolean isDirectory) {
        this.isDirectory = isDirectory;
    }

    public void setCanRead(Boolean canRead) {
        this.canRead = canRead;
    }

    public void setCanWrite(Boolean canWrite) {
        this.canWrite = canWrite;
    }

    public void setServerFd(int serverFd) {
        this.serverFd = serverFd;
    }
}
