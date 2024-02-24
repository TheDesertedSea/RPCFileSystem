import java.io.RandomAccessFile;
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
        Logger.log("Request size: " + size + " free size: " + freeSize);
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
        Logger.log("Release size: " + size + " free size: " + freeSize);
        freeSizeLock.readLock().unlock();
    }

    public String getCacheDir() {
        return cacheDir;
    }

    private FileOpenResult open(String relativePath, Boolean read, Boolean write, Boolean create, Boolean exclusive) {
        Logger.log("Open file: " + relativePath + " read: " + read + " write: " + write + " create: " + create + " exclusive: " + exclusive);
        tableLock.lock();
        CacheFile file = cacheFileTable.get(relativePath);
        FileOpenResult result = null;
        if (file == null) {
            if (!create) {
                tableLock.unlock();
                return new FileOpenResult(ResCode.ENOENT, null);
            }
            CacheFileVersion fileVersion = new CacheFileVersion(relativePath, UUID.randomUUID(), true, true, 0, null);
            result = fileVersion.open(true, true);
            tableLock.unlock();
        } else {
            if (exclusive) {
                tableLock.unlock();
                return new FileOpenResult(ResCode.EEXIST, null);
            }
            updateLRU(file);
            result = file.open(read, write);
            tableLock.unlock(); 
        }
        return result;
    }

    /**
     * Update the file in cache. Also check path on the server.
     * @param relativePath
     * @return
     */
    public FileOpenResult checkAndOpen(String relativePath, Boolean read, Boolean write, Boolean create, Boolean exclusive){
        Logger.log("Check and open file: " + relativePath + " read: " + read + " write: " + write + " create: " + create + " exclusive: " + exclusive);
        tableLock.lock();
        Logger.log("Just test");
        CacheFile file = cacheFileTable.get(relativePath);
        UUID verId = null;
        if(file != null){
            verId = file.getNewestVerId();
        } 
        tableLock.unlock();

        FileCheckResult result = null;
        /* Get new version from server */
        try {
            result = Proxy.getServer().checkFile(relativePath, verId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        Logger.log("Check file result:" + result.toString());

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
                FileOpenResult openResult = updateAndOpen(result, read, write);
                try {
                    Proxy.getServer().closeFile(result.getServerFd());
                } catch (RemoteException e) {

                    e.printStackTrace();
                }
                return openResult;
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

    private FileOpenResult updateAndOpen(FileCheckResult result, Boolean read, Boolean write){
        Logger.log("Update and open file: " + result.getRelativePath() + " read: " + read + " write: " + write);
        tableLock.lock();
        CacheFile file = cacheFileTable.get(result.getRelativePath());
        FileOpenResult openResult = null;
        if(file == null){
            file = new CacheFile(result.getRelativePath(), result.getVerId(), result.getCanRead(), result.getCanWrite(), result.getServerFd(), result.getSize());
            cacheFileTable.put(result.getRelativePath(), file);
            insertToLRU(file);
            openResult = file.open(read, write);
        } else {
            file.update(result.getVerId(), result.getCanRead(), result.getCanWrite(), result.getServerFd(), result.getSize());
            updateLRU(file);
            openResult = file.open(read, write);
        }
        tableLock.unlock();
        return openResult;
    }

    public void updateFile(String relativePath, RandomAccessFile raf, UUID verId){
        tableLock.lock();
        CacheFile file = cacheFileTable.get(relativePath);
        if(file != null){
            Logger.log("Update file in table: " + relativePath + " with verId: " + verId.toString());
            Logger.log("old verId: " + file.getNewestVerId().toString());
            file.update(verId, true, true, raf);
            updateLRU(file);
        } else {
            Logger.log("Update and add new file to table: " + relativePath + " with verId: " + verId.toString());
            file = new CacheFile(relativePath, verId, true, true, raf);
            cacheFileTable.put(relativePath, file);
            insertToLRU(file);
        }
        tableLock.unlock();
    }

    public void updateFile(CacheFileVersion fileVersion){
        Logger.log("Update file in cache: " + fileVersion.getRelativePath() + " with verId: " + fileVersion.getVerId().toString());
        tableLock.lock();
        CacheFile file = cacheFileTable.get(fileVersion.getRelativePath());
        if(file != null){
            file.update(fileVersion);
            updateLRU(file);
        } else {
            file = new CacheFile(fileVersion.getRelativePath(), fileVersion);
            cacheFileTable.put(fileVersion.getRelativePath(), file);
            insertToLRU(file);
        }
        tableLock.unlock();
    }

    public void removeFile(String relativePath){
        Logger.log("Remove file from cache: " + relativePath);
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
            Logger.log("Evict file: " + file.getRelativePath());
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
