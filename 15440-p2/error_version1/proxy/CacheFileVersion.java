import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.rmi.RemoteException;
import java.util.UUID;

public class CacheFileVersion {
    private CacheFile cacheFile;
    private String relativePath;
    private UUID verId;
    private long refCount;
    private Boolean canRead;
    private Boolean canWrite;
    private Boolean isDeleted;
    private Boolean isModified;
    private long size;

    public CacheFileVersion(CacheFile cacheFile, String relativePath, UUID verId, Boolean canRead, Boolean canWrite,
            long initialRefCount, RandomAccessFile raf) {
        Logger.log("Creating CacheFileVersion for " + relativePath + " with verId " + verId);
        this.cacheFile = cacheFile;
        this.relativePath = relativePath;
        this.verId = verId;
        this.refCount = initialRefCount;
        Logger.LRUlog("Creating CacheFileVersion for " + relativePath + " with refCount " + this.refCount);
        this.canRead = canRead;
        this.canWrite = canWrite;
        this.isDeleted = false;
        this.isModified = false;
        this.size = 0;
        File file = new File(getCacheLocation());
        try {
            file.createNewFile();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if(raf != null){
            initFileContent(raf);
        }
    }

    public CacheFileVersion(CacheFile cacheFile, String relativePath, UUID verId, Boolean canRead, Boolean canWrite, long refCount, int serverFd, long size, byte[] firstChunk) {
        Logger.log("Creating CacheFileVersion for " + relativePath + " with verId " + verId);
        this.cacheFile = cacheFile;
        this.relativePath = relativePath;
        this.verId = verId;
        this.refCount = refCount;
        Logger.LRUlog("Creating CacheFileVersion for " + relativePath + " with refCount " + this.refCount);
        this.canRead = canRead;
        this.canWrite = canWrite;
        this.isDeleted = false;
        this.isModified = false;
        this.size = 0;
        File file = new File(getCacheLocation());
        if (file.exists()) {
            file.delete();
        }
        try {
            file.createNewFile();
        } catch (Exception e) {
            e.printStackTrace();
        }

        initFileContent(serverFd, size, firstChunk);
    }

    /**
     * uses the file
     */
    public synchronized Boolean use() {
        if (isDeleted) {
            return false;
        }
        refCount++;
        Logger.LRUlog("File: " + relativePath + " is used, refCount: " + refCount);
        return true;
    }

    /**
     * releases the file
     */
    public synchronized void release() {
        if (isDeleted) {
            return;
        }
        refCount--;
        if (refCount == 0) {
            Logger.LRUlog("File: " + relativePath + " is released");
            File file = new File(getCacheLocation());
            if (isModified) {
                Logger.log("File: " + relativePath + " is modified, uploading to server");
                Logger.log("size is: " + size + " or is it? " + file.length());
                isModified = false;
                updateCache();
                uploadToServer();
            }else{
                file.delete();
                Proxy.getCache().releaseSize(size);
                isDeleted = true;
            }
        }
        Logger.LRUlog("File after releasing: " + relativePath + " refCount: " + refCount);
    }

    /**
     * Open the file as a OpenFile object
     * Close OpenFile object to release the file, not CacheFileVersion
     * 
     * @param read
     * @param write
     * @return
     */
    public FileOpenResult open(Boolean read, Boolean write) {
        if (!use()) {
            return new FileOpenResult(ResCode.ENOENT, null);
        }
        String mode = "";
        if (read) {
            if (!canRead) {
                return new FileOpenResult(ResCode.EACCES, null);
            }
            mode += "r";
        }
        if (write) {
            if (!canWrite) {
                return new FileOpenResult(ResCode.EACCES, null);
            }
            mode += "w";
        }

        File file = new File(getCacheLocation());
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(file, mode);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return new FileOpenResult(ResCode.SUCCESS, new OpenFile(read, write, this, raf));
    }

    public CacheFileVersion getWriteCopy() {
        if (!use()) { // return null if file is deleted
            return null;
        }
        RandomAccessFile raf = getRAF();
        CacheFileVersion writeCopy = new CacheFileVersion(null, relativePath, UUID.randomUUID(), true, true, 0, raf);
        try {
            raf.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        release(); // release the original file
        return writeCopy;
    }

    public long getRefCount() {
        return refCount;
    }

    public long getSize() {
        if (isDeleted) {
            return 0;
        }
        return size;
    }

    public UUID getVerId() {
        if (isDeleted) {
            return null;
        }
        return verId;
    }

    public void setSize(long size) {
        isModified = true;
        if (size > this.size) {
            Proxy.getCache().requestSize(size - this.size);
            this.size = size;
        }
        // else if (size < this.size) {
        //     Proxy.getCache().releaseSize(this.size - size);
        // }
        
    }

    private String getCacheLocation() {
        return Proxy.getCache().getCacheDir() + relativePath.replace("/", "_") + "." + verId.toString();
    }

    /**
     * Uploads modfied file to server
     * 
     * @param data
     */
    private void uploadToServer() {
        RandomAccessFile raf = getRAF();
        int serverFd = -1;
        try {
            serverFd = Proxy.getServer().putFile(relativePath, verId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        long remaining = size;
        int uploadSize = 0;
        byte[] data = null;
        while (remaining > 0) {
            try {
                uploadSize = (int) Math.min(remaining, Server.CHUNK_SIZE);
                data = new byte[uploadSize];
                raf.read(data);
                Proxy.getServer().writeFile(serverFd, data);
                remaining -= uploadSize;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            Proxy.getServer().closeFile(serverFd);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        try {
            raf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initFileContent(RandomAccessFile raf) {
        RandomAccessFile thisFile = getRAF();
        try {
            this.size = raf.length();
            Proxy.getCache().requestSize(size);
            long remaining = size;
            int readSize = 0;
            byte[] data = null;
            while (remaining > 0) {
                readSize = (int) Math.min(remaining, Server.CHUNK_SIZE);
                data = new byte[readSize];
                raf.read(data);
                thisFile.write(data);
                remaining -= readSize;
            }
            thisFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initFileContent(int serverFd, long size, byte[] firstChunk){
        RandomAccessFile thisFile = getRAF();
        try {
            thisFile.write(firstChunk);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Proxy.getCache().requestSize(size);
        long totalRead = firstChunk.length;
        int readSize = 0;
        byte[] data = null;
        while(totalRead < size)
        {
            try {
                data = Proxy.getServer().readFile(serverFd);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            readSize = data.length;
            try {
                thisFile.write(data);
            } catch (IOException e) {
                e.printStackTrace();
            }
            totalRead += readSize;
        }
        try {
            thisFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.size = size;
    }

    /**
     * Updates cache with new data
     * 
     * @param data
     */
    private void updateCache() {
        RandomAccessFile raf = getRAF();
        Proxy.getCache().updateFile(this);
        try {
            raf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public RandomAccessFile getRAF() {
        try {
            return new RandomAccessFile(getCacheLocation(), "rw");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public void setCacheFile(CacheFile cacheFile) {
        this.cacheFile = cacheFile;
    }

    public void updateLRU(){
        if(cacheFile != null){
            cacheFile.updateLRU();
        }
    }
}
