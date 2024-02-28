
/**
 * Cache.java
 * 
 * @author Cundao Yu <cundaoy@andrew.cmu.edu>
 */
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Cache class to manage the cache.
 * 
 * It has a HashMap {@link HashMap} to store the mapping from relativePath to
 * CacheFile.
 * It stores the head of LRU list(a doubly-linked list of CacheFile
 * {@link CacheFile} objects) as mostRecentUsed and the tail as
 * leastRecentUsed.
 * It has a tableLock {@link ReentrantLock} to protect the cache file HashMap.
 * It has a freeSizeLock {@link ReentrantLock} to protect the freeSize(availble
 * space in cache).
 */
public class Cache {
    /**
     * {@link String}
     * The directory of the cache
     */
    private String cacheDir;
    /**
     * The available space in cache
     */
    private long freeSize;
    /**
     * {@link HashMap}<{@link String}, {@link CacheFile}>
     * The mapping from relativePath to CacheFile
     */
    private HashMap<String, CacheFile> cacheFileTable;
    /**
     * {@link CacheFile}
     * The head of LRU list, the most recently used file
     */
    private CacheFile mostRecentUsed;
    /**
     * {@link CacheFile}
     * The tail of LRU list, the least recently used file
     */
    private CacheFile leastRecentUsed;
    /**
     * {@link ReentrantLock}
     * The lock to protect the cache file HashMap
     */
    private ReentrantLock tableLock;
    /**
     * {@link ReentrantLock}
     * The lock to protect the freeSize
     */
    private ReentrantLock freeSizeLock;

    /**
     * Constructor of Cache
     * 
     * @param cacheDir  {@link String} The directory of the cache
     * @param cacheSize The size of the cache
     */
    public Cache(String cacheDir, long cacheSize) {
        this.cacheDir = cacheDir;
        this.freeSize = cacheSize;
        this.cacheFileTable = new HashMap<String, CacheFile>();
        this.mostRecentUsed = null;
        this.leastRecentUsed = null;
        this.tableLock = new ReentrantLock();
        this.freeSizeLock = new ReentrantLock();
    }

    /**
     * Check file on server and open it.
     * 
     * @param relativePath {@link String} The relative path of the file
     * @param read         {@link Boolean} True if to read
     * @param write        {@link Boolean} True if to write
     * @param create       {@link Boolean} True if to create
     * @param exclusive    {@link Boolean} True if to create exclusively(return
     *                     EEXIST if file exists)
     * @return {@link FileOpenResult} The result of opening the file
     */
    public FileOpenResult checkAndOpen(String relativePath, Boolean read, Boolean write, Boolean create,
            Boolean exclusive) {
        /* First, check in the file map to get current version */
        tableLock.lock(); // Lock table lock
        CacheFile file = cacheFileTable.get(relativePath);
        UUID verId = null;
        if (file != null) {
            verId = file.getNewestVerId();
        }
        tableLock.unlock(); // Unlock table lock

        FileCheckResult result = null;
        /* Check file on the server */
        try {
            result = Proxy.getServer().checkFile(relativePath, verId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        if (result.getResCode() < 0) {
            return new FileOpenResult(result.getResCode(), null);
        }

        /* Handle the check result */
        switch (result.getResCode()) {
            case ResCode.NO_UPDATE:
                // File is already newest version, open it
                return open(relativePath, read, write, create, exclusive);
            case ResCode.NEW_VERSION:
                // New version of file, update the cache and open it
                if (exclusive) {
                    return new FileOpenResult(ResCode.EEXIST, null);
                }
                FileOpenResult openResult = updateAndOpen(result, read, write); // Update cache and open the file
                try {
                    Proxy.getServer().closeFile(result.getServerFd()); // Close the openfile on server
                } catch (RemoteException e) {

                    e.printStackTrace();
                }
                return openResult;
            case ResCode.IS_DIR:
                // File is a directory, Only read mode is allowed
                if (write || create || exclusive) {
                    return new FileOpenResult(ResCode.EISDIR, null);
                }
                return new FileOpenResult(ResCode.IS_DIR, new OpenFile(true));
            case ResCode.NOT_EXIST:
                // File not exists on server, remove it from cache if in cache
                removeFile(relativePath);
                return open(relativePath, read, write, create, exclusive);
            default:
                throw new RuntimeException("Error: Unknown result code");
        }
    }

    /**
     * Open a file in cache.
     * 
     * @param relativePath {@link String} The relative path of the file
     * @param read         {@link Boolean} True if to read
     * @param write        {@link Boolean} True if to write
     * @param create       {@link Boolean} True if to create
     * @param exclusive    {@link Boolean} True if to create exclusively(return
     *                     EEXIST if file
     *                     exists)
     * @return {@link FileOpenResult} The result of opening the file
     */
    private FileOpenResult open(String relativePath, Boolean read, Boolean write, Boolean create, Boolean exclusive) {
        tableLock.lock(); // Lock table lock
        CacheFile file = cacheFileTable.get(relativePath);
        FileOpenResult result = null;
        if (file == null) {
            // File not in cache
            if (!create) {
                tableLock.unlock();
                return new FileOpenResult(ResCode.ENOENT, null);
            }
            CacheFileVersion fileVersion = new CacheFileVersion(null, relativePath, UUID.randomUUID(), true, true, 0,
                    null); // Create a temporary file version for writing, after writing, it will be added
                           // to cache map
            result = fileVersion.open(true, true);
            tableLock.unlock(); // Unlock table lock
        } else {
            // File is in cache
            if (exclusive) {
                tableLock.unlock(); // Unlock table lock
                return new FileOpenResult(ResCode.EEXIST, null);
            }
            result = file.open(read, write);
            tableLock.unlock(); // Unlock table lock
        }
        return result;
    }

    /**
     * Update cache and open the file.
     * 
     * @param result {@link FileCheckResult} The result of checking the file on
     *               server
     * @param read   {@link Boolean} True if to read
     * @param write  {@link Boolean} True if to write
     * @return {@link FileOpenResult} The result of opening the file
     */
    private FileOpenResult updateAndOpen(FileCheckResult result, Boolean read, Boolean write) {
        tableLock.lock(); // Lock table lock
        CacheFile file = cacheFileTable.get(result.getRelativePath());
        FileOpenResult openResult = null;
        if (file == null) {
            // File not in cache, create a new CacheFile and open it
            file = new CacheFile(result.getRelativePath(), result.getVerId(), result.getCanRead(), result.getCanWrite(),
                    result.getServerFd(), result.getSize(), result.getFirstChunk());
            cacheFileTable.put(result.getRelativePath(), file);
            insertToLRU(file);
            openResult = file.open(read, write);
        } else if (!file.getNewestVerId().equals(result.getVerId())) {
            // New version of file, update the CacheFile and open it
            file.update(result.getVerId(), result.getCanRead(), result.getCanWrite(), result.getServerFd(),
                    result.getSize(), result.getFirstChunk());
            openResult = file.open(read, write);
        } else {
            // File is already newest version, open it
            openResult = file.open(read, write);
        }
        tableLock.unlock(); // Unlock table lock
        return openResult;
    }

    /**
     * Update file in cache with new version.
     * 
     * @param fileVersion {@link CacheFileVersion} The file version to update
     */
    public void updateFile(CacheFileVersion fileVersion) {
        tableLock.lock(); // Lock table lock
        CacheFile file = cacheFileTable.get(fileVersion.getRelativePath());
        if (file != null) {
            // File in cache, update it
            file.update(fileVersion);
        } else {
            // File not in cache, create a new CacheFile and update it
            file = new CacheFile(fileVersion.getRelativePath(), fileVersion);
            cacheFileTable.put(fileVersion.getRelativePath(), file);
            insertToLRU(file);
        }
        tableLock.unlock(); // Unlock table lock
    }

    /**
     * Remove file from cache.
     * 
     * @param relativePath {@link String} The relative path of the file
     */
    public void removeFile(String relativePath) {
        tableLock.lock(); // Lock table lock
        CacheFile file = cacheFileTable.get(relativePath);
        if (file != null) {
            cacheFileTable.remove(relativePath);
            removeFromLRU(file);
            file.remove();
        }
        tableLock.unlock(); // Unlock table lock
    }

    /**
     * Request size from cache.
     * 
     * @param size The size to request
     */
    public void requestSize(long size) {
        freeSizeLock.lock(); // Lock freeSize lock
        evictToSize(size); // Evict cache to size required
        freeSize -= size; // Decrease free size
        Logger.log("Request size: " + size + " free size: " + freeSize);
        freeSizeLock.unlock(); // Unlock freeSize lock
    }

    /**
     * Release size to cache.
     * 
     * @param size The size to release
     */
    public void releaseSize(long size) {
        freeSizeLock.lock(); // Lock freeSize lock
        freeSize += size; // Increase free size
        Logger.log("Release size: " + size + " free size: " + freeSize);
        freeSizeLock.unlock(); // Unlock freeSize lock
    }

    /**
     * Evict cache to size required.
     * 
     * This can only be called when freeSize lock is held.
     * This will lock table write lock.
     * 
     * @param sizeRequired
     */
    private void evictToSize(long sizeRequired) {
        if (freeSize >= sizeRequired) {
            Logger.log("Free size is enough, no need to evict");
            return;
        }
        Logger.log("Current free size: " + freeSize + " required size: " + sizeRequired);
        tableLock.lock(); // Lock table lock

        /* Traverse the LRU list to evict */
        CacheFile file = leastRecentUsed;
        while (freeSize < sizeRequired) {
            if (file == null) {
                if (leastRecentUsed == null) {
                    Logger.log("Exception: Required size is larger than cache size");
                    tableLock.unlock(); // Unlock table lock
                    return;
                }
                Logger.log("No file could be evicted now, sleep and try again");
                // sleep and try again
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                file = leastRecentUsed;
            }
            Logger.log("Try to evict file: " + file.getRelativePath());
            if (file.isNewestInUse()) {
                // This file is in use, skip
                file = file.getPrev();
                Logger.log("File is in use, skip");
                continue;
            }
            // Evict this file
            cacheFileTable.remove(file.getRelativePath());
            CacheFile prev = file.getPrev();
            removeFromLRU(file);
            file.remove();
            file = prev;
            Logger.log("Evicted file: " + file.getRelativePath());
        }
        Logger.log(getLRUStatus());
        tableLock.unlock();
    }

    /**
     * insert a new file to LRU
     * 
     * This can only be called when table lock is held.
     * 
     * @param file {@link CacheFile} The file to insert
     */
    private void insertToLRU(CacheFile file) {
        if (mostRecentUsed == null) {
            mostRecentUsed = file;
            leastRecentUsed = file;
        } else {
            file.setNext(mostRecentUsed);
            mostRecentUsed.setPrev(file);
            mostRecentUsed = file;
        }
        Logger.log(getLRUStatus());
    }

    /**
     * remove a file from LRU
     * This can only be called when table lock is held.
     * 
     * @param file {@link CacheFile} The file to remove
     */
    private void removeFromLRU(CacheFile file) {
        if (file.getPrev() != null) {
            file.getPrev().setNext(file.getNext());
        } else {
            mostRecentUsed = file.getNext();
        }
        if (file.getNext() != null) {
            file.getNext().setPrev(file.getPrev());
        } else {
            leastRecentUsed = file.getPrev();
        }
        file.setNext(null);
        file.setPrev(null);
    }

    /**
     * Move a file to the head of LRU
     * 
     * This can only be called when table lock is held.
     * 
     * @param file {@link CacheFile} The file to move
     */
    public void updateLRU(CacheFile file) {
        tableLock.lock();
        removeFromLRU(file);
        insertToLRU(file);
        tableLock.unlock();
    }

    /**
     * Get the status of LRU list
     * 
     * @return {@link String} The file sequence in LRU list
     */
    private String getLRUStatus() {
        String status = "LRU status: ";
        CacheFile file = mostRecentUsed;
        while (file != null) {
            status += file.getRelativePath() + " ";
            file = file.getNext();
        }
        return status;
    }

    /**
     * Get the cache directory
     * 
     * @return {@link String} The cache directory
     */
    public String getCacheDir() {
        return cacheDir;
    }
}
