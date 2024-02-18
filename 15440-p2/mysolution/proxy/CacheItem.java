public class CacheItem {
    private String serverPath;
    private long version;
    private Boolean isDirectory;
    private Boolean canRead;
    private Boolean canWrite;
    private String localPath;
    private long refCount;

    public CacheItem(String serverPath, long version, String localPath, Boolean isDirectory, Boolean canRead, Boolean canWrite) {
        this.serverPath = serverPath;
        this.version = version;
        this.localPath = localPath;
        this.isDirectory = isDirectory;
        this.canRead = canRead;
        this.canWrite = canWrite;
        this.refCount = 0;
    }

    public String getServerPath() {
        return serverPath;
    }

    public long getVersion() {
        return version;
    }

    public String getLocalPath() {
        return localPath;
    }

    public Boolean isDirectory() {
        return isDirectory;
    }

    public Boolean canRead() {
        return canRead;
    }

    public Boolean canWrite() {
        return canWrite;
    }

    public long getRefCount() {
        return refCount;
    }

    public void incRefCount() {
        refCount++;
    }

    public void decRefCount() {
        refCount--;
    }
}
