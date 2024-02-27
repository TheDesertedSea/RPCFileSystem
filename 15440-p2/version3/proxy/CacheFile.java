
/**
 * CacheFile.java
 * 
 * @author Cundao Yu <cundaoy@andrew.cmu.edu>
 */
import java.util.UUID;

/**
 * Abstraction of a file in the cache
 * 
 * It contains the newest version {@link CacheFileVersion} of the file
 * Also, it's a node of a doubly linked list(LRU list)
 */
public class CacheFile {
    /**
     * {@link String}
     * Relative path of the file
     */
    private String relativePath;
    /**
     * {@link CacheFileVersion}
     * The newest version of the file
     */
    private CacheFileVersion newest;
    /**
     * {@link CacheFile}
     * The more recent used file in the LRU list
     */
    private CacheFile prev;
    /**
     * {@link CacheFile}
     * The less recent used file in the LRU list
     */
    private CacheFile next;

    /**
     * Constructor using server data as file content source
     * 
     * @param relativePath {@link String} Relative path of the file
     * @param verId        {@link UUID} Version ID of the file
     * @param canRead      {@link Boolean} True if the file can be read
     * @param canWrite     {@link Boolean} True if the file can be written
     * @param serverFd     File descriptor of the file in the server
     * @param size         Size of the file
     * @param firstChunk   First chunk of the file content
     */
    public CacheFile(String relativePath, UUID verId, Boolean canRead, Boolean canWrite, int serverFd, long size,
            byte[] firstChunk) {
        this.relativePath = relativePath;
        this.newest = new CacheFileVersion(this, relativePath, verId, canRead, canWrite, 1, serverFd, size, firstChunk);
        this.prev = null;
        this.next = null;
    }

    /**
     * Constructor using a file version as newest version
     * 
     * @param relativePath {@link String} Relative path of the file
     * @param fileVersion  {@link CacheFileVersion} The newest version of the file
     */
    public CacheFile(String relativePath, CacheFileVersion fileVersion) {
        this.relativePath = relativePath;
        this.newest = fileVersion;
        this.prev = null;
        this.next = null;
    }

    /**
     * Open the file
     * 
     * @param read  {@link Boolean} True if the file is opened for reading
     * @param write {@link Boolean} True if the file is opened for writing
     * @return {@link FileOpenResult} The result of the file open operation
     */
    public FileOpenResult open(Boolean read, Boolean write) {
        if (write) {
            // If the file is opened for writing, clone the newest version to get a write
            // copy version
            CacheFileVersion writeCopy = newest.getWriteCopy();
            if (writeCopy == null) {
                return new FileOpenResult(ResCode.ENOENT, null);
            }
            return writeCopy.open(read, write);
        }
        return newest.open(read, write);
    }

    /**
     * Update the file with server data
     * 
     * @param verId      {@link UUID} Version ID of the file
     * @param canRead    {@link Boolean} True if the file can be read
     * @param canWrite   {@link Boolean} True if the file can be written
     * @param serverFd   File descriptor of the file in the server
     * @param size       Size of the file
     * @param firstChunk First chunk of the file content
     */
    public void update(UUID verId, Boolean canRead, Boolean canWrite, int serverFd, long size, byte[] firstChunk) {
        newest.release(); // Release the old newest version
        newest.setCacheFile(null); // Set the old newest version's cache file to null
        newest = new CacheFileVersion(this, relativePath, verId, canRead, canWrite, 1, serverFd, size, firstChunk);
    }

    /**
     * Update the file with a file version
     * 
     * @param fileVersion {@link CacheFileVersion} The new newest version of the
     *                    file
     */
    public void update(CacheFileVersion fileVersion) {
        newest.release(); // Release the old newest version
        newest.setCacheFile(null); // Set the old newest version's cache file to null
        fileVersion.use(); // Use the new newest version
        fileVersion.setCacheFile(this); // Set the new newest version's cache file to this
        newest = fileVersion;
    }

    /**
     * Update the LRU list to move the file to the most recent used position
     */
    public void updateLRU() {
        Proxy.getCache().updateLRU(this);
    }

    /**
     * Remove the file
     */
    public void remove() {
        newest.release(); // Release the newest version
        newest.setCacheFile(null); // Set the newest version's cache file to null
    }

    /**
     * Check if the newest version of the file is in use
     * 
     * @return {@link Boolean} True if the newest version of the file is in use
     */
    public Boolean isNewestInUse() {
        long refCount = newest.getRefCount();
        Logger.log("Check newest refCount: " + refCount + " for " + relativePath);
        return refCount > 1;
    }

    /**
     * Get the more recently used file in the LRU list
     * 
     * @return {@link CacheFile} The more recently used file in the LRU list
     */
    public CacheFile getPrev() {
        return prev;
    }

    /**
     * Get the less recently used file in the LRU list
     * 
     * @return {@link CacheFile} The less recently used file in the LRU list
     */
    public CacheFile getNext() {
        return next;
    }

    /**
     * Set the more recently used file in the LRU list
     * 
     * @param prev {@link CacheFile} The more recently used file in the LRU list
     */
    public void setPrev(CacheFile prev) {
        this.prev = prev;
    }

    /**
     * Set the less recently used file in the LRU list
     * 
     * @param next {@link CacheFile} The less recently used file in the LRU list
     */
    public void setNext(CacheFile next) {
        this.next = next;
    }

    /**
     * Get the relative path of the file
     * 
     * @return {@link String} Relative path of the file
     */
    public String getRelativePath() {
        return relativePath;
    }

    /**
     * Get the newest version of the file
     * 
     * @return {@link CacheFileVersion} The newest version of the file
     */
    public long getNewestSize() {
        return newest.getSize();
    }

    /**
     * Get the newest version ID of the file
     * 
     * @return {@link UUID} The newest version ID of the file
     */
    public UUID getNewestVerId() {
        return newest.getVerId();
    }
}
