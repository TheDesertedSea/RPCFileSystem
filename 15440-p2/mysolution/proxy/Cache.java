import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Cache {
    private String cacheDir;
    private long freeSpace;
    private ReentrantReadWriteLock freeSpaceLock = new ReentrantReadWriteLock();
    private HashMap<String, CacheFile> cacheFileTable;
    private CacheFile mostRecentUsed;
    private CacheFile leastRecentUsed;

    public Cache(String cacheDir, long cacheSize) {
        this.cacheDir = cacheDir;
        this.freeSpace = cacheSize;
        this.cacheFileTable = new HashMap<String, CacheFile>();
        this.mostRecentUsed = null;
        this.leastRecentUsed = null;
    }

    public synchronized CacheFileVersion getNewestVersion(String path, Boolean bWrite) {
        CacheFile cacheFile = cacheFileTable.get(path);
        if (cacheFile == null) {
            return null;
        }
        updateLRU(cacheFile);
        CacheFileVersion newestVersion = cacheFile.getNewestVersion();
        if (newestVersion == null) {
            return null;
        }
        if (bWrite) {
            newestVersion.release();
            evictUntilEnoughSpace(newestVersion.getSize());
            CacheFileVersion writeCopy = newestVersion.cloneWriteCopy();
            return writeCopy;
        }
        return newestVersion;
    }

    public synchronized CacheFileVersion updateNewestVersion(String path, UUID versionId, Boolean canRead,
            Boolean canWrite,
            byte[] data, Boolean bWrite) {
        int size = data == null ? 0 : data.length;
        CacheFileVersion newestVersion;
        if (cacheFileTable.containsKey(path)) {
            CacheFile cacheFile = cacheFileTable.get(path);
            newestVersion = cacheFile.getNewestVersion();
            if (newestVersion != null && newestVersion.getVersionId().equals(versionId)) {
                if (bWrite) {
                    if (newestVersion != null) {
                        newestVersion.release();
                    }
                    evictUntilEnoughSpace(size);
                    CacheFileVersion writeCopy = newestVersion.cloneWriteCopy();
                    return writeCopy;
                }
                return newestVersion;
            }
            if (newestVersion != null) {
                newestVersion.release();
            }
            cacheFile.removeNewestVersion();
            evictUntilEnoughSpace(size);
            newestVersion = new CacheFileVersion(this, path, versionId, canRead, canWrite, 0, data);
            cacheFile.setNewestVersion(newestVersion);
            updateLRU(cacheFile);
        } else {
            evictUntilEnoughSpace(size);
            newestVersion = new CacheFileVersion(this, path, versionId, canRead, canWrite, 0, data);
            CacheFile newCacheFile = new CacheFile(path, newestVersion);
            insertLRU(newCacheFile);
            cacheFileTable.put(path, newCacheFile);
        }
        if (bWrite) {
            evictUntilEnoughSpace(size);
            CacheFileVersion writeCopy = newestVersion.cloneWriteCopy();
            return writeCopy;
        }
        newestVersion.use();
        return newestVersion;
    }

    public synchronized void removeFile(String path) {
        CacheFile cacheFile = cacheFileTable.get(path);
        if (cacheFile == null) {
            Logger.log("Cache: removeFile: " + path + " not found");
            return;
        }
        cacheFileTable.remove(path);
        removeFromLRU(cacheFile);
        cacheFile.remove();
    }

    private void evictUntilEnoughSpace(long requiredSpace) {
        freeSpaceLock.writeLock().lock();
        while (freeSpace < requiredSpace) {
            if (leastRecentUsed == null) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            CacheFile cacheFile = leastRecentUsed;
            leastRecentUsed = leastRecentUsed.getPrev();
            leastRecentUsed.setNext(null);
            cacheFileTable.remove(cacheFile.getPath());
            cacheFile.remove();
        }
        freeSpace -= requiredSpace;
        freeSpaceLock.writeLock().unlock();
    }

    private void updateLRU(CacheFile cacheFile) {
        if (cacheFile == null) {
            return;
        }

        if (mostRecentUsed == null) {
            return;
        }

        if (mostRecentUsed == cacheFile) {
            return;
        }

        if (leastRecentUsed == cacheFile) {
            leastRecentUsed = leastRecentUsed.getPrev();
            leastRecentUsed.setNext(null);
        } else {
            cacheFile.getPrev().setNext(cacheFile.getNext());
            cacheFile.getNext().setPrev(cacheFile.getPrev());
        }

        cacheFile.setNext(mostRecentUsed);
        mostRecentUsed.setPrev(cacheFile);
        mostRecentUsed = cacheFile;
    }

    private void insertLRU(CacheFile newCacheFile) {
        if (newCacheFile == null) {
            return;
        }

        if (mostRecentUsed == null) {
            mostRecentUsed = newCacheFile;
            leastRecentUsed = newCacheFile;
            return;
        }

        newCacheFile.setNext(mostRecentUsed);
        mostRecentUsed.setPrev(newCacheFile);
        mostRecentUsed = newCacheFile;
    }

    private void removeFromLRU(CacheFile cacheFile) {
        if (cacheFile == null) {
            return;
        }

        if (mostRecentUsed == cacheFile) {
            mostRecentUsed = cacheFile.getNext();
            if (mostRecentUsed != null) {
                mostRecentUsed.setPrev(null);
            }
        } else if (leastRecentUsed == cacheFile) {
            leastRecentUsed = cacheFile.getPrev();
            if (leastRecentUsed != null) {
                leastRecentUsed.setNext(null);
            }
        } else {
            cacheFile.getPrev().setNext(cacheFile.getNext());
            cacheFile.getNext().setPrev(cacheFile.getPrev());
        }
    }

    public void giveBackSpace(long space) {
        freeSpaceLock.readLock().lock();
        freeSpace += space;
        freeSpaceLock.readLock().unlock();
    }

    public synchronized void takeSpace(long space) {
        evictUntilEnoughSpace(space);
    }

    public String getCacheDir() {
        return cacheDir;
    }
}
