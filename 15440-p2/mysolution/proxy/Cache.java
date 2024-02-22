import java.util.HashMap;
import java.util.UUID;

public class Cache {
    private String cacheDir;
    private long cacheSize;
    private HashMap<String, CacheFile> cacheFileTable;
    private CacheFile mostRecentUsed;
    private CacheFile leastRecentUsed;

    public Cache(String cacheDir, long cacheSize) {
        this.cacheDir = cacheDir;
        this.cacheSize = cacheSize;
        this.cacheFileTable = new HashMap<String, CacheFile>();
        this.mostRecentUsed = null;
        this.leastRecentUsed = null;
    }

    public synchronized CacheFileVersion getNewestVersion(String path) {
        CacheFile cacheFile = cacheFileTable.get(path);
        if (cacheFile == null) {
            return null;
        }
        updateLRU(cacheFile);
        return cacheFile.getNewestVersion();
    }

    public synchronized CacheFileVersion updateNewestVersion(String path, UUID versionId, Boolean canRead, Boolean canWrite,
            long size) {
        CacheFileVersion newestVersion;
        if (cacheFileTable.containsKey(path)) {
            CacheFile cacheFile = cacheFileTable.get(path);
            newestVersion = cacheFile.getNewestVersion();
            if (newestVersion == null || newestVersion.getVersionId().equals(versionId)) {
                return newestVersion;
            }
            newestVersion = new CacheFileVersion(versionId, canRead, canWrite, size);
            cacheFile.setNewestVersion(newestVersion);
            updateLRU(cacheFile);
        } else {
            newestVersion = new CacheFileVersion(versionId, canRead, canWrite, size);
            CacheFile newCacheFile = new CacheFile(path, newestVersion);
            insertLRU(newCacheFile);
            cacheFileTable.put(path, newCacheFile);
        }
        return newestVersion;
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

}
