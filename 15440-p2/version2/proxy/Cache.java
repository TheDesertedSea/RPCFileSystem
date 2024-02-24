import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Cache {
    private String cacheDir;
    private long freeSize;
    private HashMap<String, CacheFile> cacheFileTable;
    private CacheFile mostRecentUsed; // head of LRU
    private CacheFile leastRecentUsed; // tail of LRU
    private ReentrantLock tableLock = new ReentrantLock();
    private ReentrantReadWriteLock freeSizeLock = new ReentrantReadWriteLock();

    public Cache(String cacheDir, long cacheSize) {
        this.cacheDir = cacheDir;
        this.freeSize = cacheSize;
        this.cacheFileTable = new HashMap<String, CacheFile>();
        this.mostRecentUsed = null;
        this.leastRecentUsed = null;
    }

    /**
     * request size from cache, this lock freeSize write lock.
     * This means, only one thread can request size at a time.
     * 
     * @param size
     */
    public void requestSize(long size) {
        freeSizeLock.writeLock().lock();
        evictToSize(size);
        freeSize -= size;
        freeSizeLock.writeLock().unlock();
    }

    /**
     * release size to cache, this lock freeSize read lock.
     * This means, multiple threads can release size at a time.
     * 
     * @param size
     */
    public void releaseSize(long size) {
        freeSizeLock.readLock().lock();
        freeSize += size;
        freeSizeLock.readLock().unlock();
    }

    public String getCacheDir() {
        return cacheDir;
    }

    private FileOpenResult open(String relativePath, Boolean read, Boolean write, Boolean create, Boolean exclusive) {
        tableLock.lock();
        CacheFile file = cacheFileTable.get(relativePath);
        FileOpenResult result = null;
        if (file == null) {
            if (!create) {
                return new FileOpenResult(ResCode.ENOENT, null);
            }
            file = cacheFileTable.get(relativePath);
            if (file == null) {
                file = new CacheFile(relativePath, UUID.randomUUID(), read, write, new byte[0]);
                cacheFileTable.put(relativePath, file);
                insertToLRU(file);
            } else {
                if (exclusive) {
                    tableLock.unlock();
                    return new FileOpenResult(ResCode.EEXIST, null);
                }
            }
            updateLRU(file);
            result = file.open(read, write);
            tableLock.unlock();
        } else {
            updateLRU(file);
            result = file.open(read, write);
            tableLock.unlock();
            if (exclusive) {
                return new FileOpenResult(ResCode.EEXIST, null);
            }
        }
        return result;
    }

    /**
     * Update the file in cache. Also check path on the server.
     * @param relativePath
     * @return
     */
    public FileOpenResult checkAndOpen(String relativePath, Boolean read, Boolean write, Boolean create, Boolean exclusive){
        tableLock.lock();
        CacheFile file = cacheFileTable.get(relativePath);
        UUID verId = null;
        if(file != null){
            verId = file.getNewestVerId();
        } 
        tableLock.unlock();

        FileGetResult result = null;
        /* Get new version from server */
        try {
            result = Proxy.getServer().getFile(relativePath, verId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        if(result.getResCode() < 0){
            return new FileOpenResult(result.getResCode(), null);
        }

        switch(result.getResCode()){
            case ResCode.NO_UPDATE:
                return open(relativePath, read, write, create, exclusive);
            case ResCode.NEW_VERSION:
                if(exclusive){
                    return new FileOpenResult(ResCode.EEXIST, null);
                }
                return updateAndOpen(result, read, write);
            case ResCode.IS_DIR:
                if(write || create || exclusive){
                    return new FileOpenResult(ResCode.EISDIR, null);
                }
                return new FileOpenResult(ResCode.IS_DIR, new OpenFile(true));
            case ResCode.NOT_EXIST:
                removeFile(relativePath);
                return open(relativePath, read, write, create, exclusive);
            default:
                throw new RuntimeException("Error: Unknown result code");
        }
    }

    private FileOpenResult updateAndOpen(FileGetResult result, Boolean read, Boolean write){
        tableLock.lock();
        CacheFile file = cacheFileTable.get(result.getRelativePath());
        FileOpenResult openResult = null;
        if(file == null){
            file = new CacheFile(result.getRelativePath(), result.getVerId(), result.getCanRead(), result.getCanWrite(), result.getData());
            cacheFileTable.put(result.getRelativePath(), file);
            insertToLRU(file);
            openResult = file.open(read, write);
        } else {
            file.update(result.getVerId(), result.getCanRead(), result.getCanWrite(), result.getData());
            updateLRU(file);
            openResult = file.open(read, write);
        }
        tableLock.unlock();
        return openResult;
    }

    public void updateFile(String relativePath, byte[] data, UUID verId){
        tableLock.lock();
        CacheFile file = cacheFileTable.get(relativePath);
        if(file != null){
            file.update(verId, true, true, data);
            updateLRU(file);
        } else {
            file = new CacheFile(relativePath, verId, true, true, data);
            cacheFileTable.put(relativePath, file);
            insertToLRU(file);
        }
        tableLock.unlock();
    }

    public void removeFile(String relativePath){
        tableLock.lock();
        CacheFile file = cacheFileTable.get(relativePath);
        if(file != null){
            cacheFileTable.remove(relativePath);
            removeFromLRU(file);
            file.remove();
        }
        tableLock.unlock();
    }

    /**
     * evict cache to size required.
     * This can only be called when freeSize write lock is held.
     * This will lock table write lock.
     * 
     * @param sizeRequired
     */
    private void evictToSize(long sizeRequired) {
        tableLock.lock();
        CacheFile file = leastRecentUsed;
        while (freeSize < sizeRequired) {
            if (file == null) {
                Logger.log("Error: Cannot evict to required size");
                break;
            }
            if (file.isNewestInUse()) {
                file = file.getPrev();
                continue;
            }
            cacheFileTable.remove(file.getRelativePath());
            removeFromLRU(file);
            file.remove();
            file = file.getPrev();
        }
        tableLock.unlock();
    }

    /**
     * insert a new file to LRU
     * This can only be called when table write lock is held.
     * 
     * @param file
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
    }

    /**
     * remove a file from LRU
     * This can only be called when table write lock is held.
     * 
     * @param file
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
    }

    /**
     * move a file to the head of LRU
     * This can only be called when table write lock is held.
     * 
     * @param file
     */
    private void updateLRU(CacheFile file) {
        removeFromLRU(file);
        insertToLRU(file);
    }
}
