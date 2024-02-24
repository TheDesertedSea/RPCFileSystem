import java.util.UUID;

public class CacheFile {
    private String relativePath;
    private CacheFileVersion newest;
    private CacheFile prev;
    private CacheFile next;

    public CacheFile(String relativePath, UUID verId, Boolean canRead, Boolean canWrite, byte[] data) {
        this.relativePath = relativePath;
        this.newest = new CacheFileVersion(relativePath, verId, canRead, canWrite, data, 1);
        this.prev = null;
        this.next = null;
    }

    public Boolean isNewestInUse() {
        return newest.getRefCount() > 1;
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

    public void update(UUID verId, Boolean canRead, Boolean canWrite, byte[] data) {
        newest.release();
        newest = new CacheFileVersion(relativePath, verId, canRead, canWrite, data, 1);
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
