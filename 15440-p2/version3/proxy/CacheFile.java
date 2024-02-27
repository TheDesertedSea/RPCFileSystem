import java.io.RandomAccessFile;
import java.util.UUID;

public class CacheFile {
    private String relativePath;
    private CacheFileVersion newest;
    private CacheFile prev;
    private CacheFile next;

    public CacheFile(String relativePath, UUID verId, Boolean canRead, Boolean canWrite, RandomAccessFile raf) {
        this.relativePath = relativePath;
        this.newest = new CacheFileVersion(relativePath, verId, canRead, canWrite, 1, raf);
        this.prev = null;
        this.next = null;
    }

    public CacheFile(String relativePath, UUID verId, Boolean canRead, Boolean canWrite, int serverFd, long size, byte[] firstChunk) {
        this.relativePath = relativePath;
        this.newest = new CacheFileVersion(relativePath, verId, canRead, canWrite, 1, serverFd, size, firstChunk);
        this.prev = null;
        this.next = null;
    }

    public CacheFile(String relativePath, CacheFileVersion fileVersion) {
        this.relativePath = relativePath;
        this.newest = fileVersion;
        this.prev = null;
        this.next = null;
    }

    public Boolean isNewestInUse() {
        long refCount = newest.getRefCount();
        Logger.LRUlog("Check newest refCount: " + refCount);
        return refCount > 1;
    }

    public CacheFile getPrev() {
        return prev;
    }

    public CacheFile getNext() {
        return next;
    }

    public void setPrev(CacheFile prev) {
        this.prev = prev;
    }

    public void setNext(CacheFile next) {
        this.next = next;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public void remove() {
        newest.release();
    }

    public long getNewestSize() {
        return newest.getSize();
    }

    public UUID getNewestVerId() {
        return newest.getVerId();
    }

    public void update(UUID verId, Boolean canRead, Boolean canWrite, RandomAccessFile raf) {
        newest.release();
        newest = new CacheFileVersion(relativePath, verId, canRead, canWrite, 1, raf);
    }

    public void update(UUID verId, Boolean canRead, Boolean canWrite, int serverFd, long size, byte[] firstChunk) {
        newest.release();
        newest = new CacheFileVersion(relativePath, verId, canRead, canWrite, 1, serverFd, size, firstChunk);
    }

    public void update(CacheFileVersion fileVersion) {
        newest.release();
        fileVersion.use();
        newest = fileVersion;
    }

    public FileOpenResult open(Boolean read, Boolean write) {
        if (write) {
            CacheFileVersion writeCopy = newest.getWriteCopy();
            if (writeCopy == null) {
                return new FileOpenResult(ResCode.ENOENT, null);
            }
            return writeCopy.open(read, write);
        }
        return newest.open(read, write);
    }
}
