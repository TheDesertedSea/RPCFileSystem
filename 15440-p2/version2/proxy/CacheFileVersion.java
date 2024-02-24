import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.rmi.RemoteException;
import java.util.UUID;

public class CacheFileVersion {
    private String relativePath;
    private UUID verId;
    private long refCount;
    private Boolean canRead;
    private Boolean canWrite;
    private Boolean isDeleted;
    private Boolean isModified;
    private long size;

    public CacheFileVersion(String relativePath, UUID verId, Boolean canRead, Boolean canWrite, byte[] data,
            long initialRefCount) {
        this.relativePath = relativePath;
        this.verId = verId;
        this.refCount = initialRefCount;
        this.canRead = canRead;
        this.canWrite = canWrite;
        this.isDeleted = false;
        this.isModified = false;
        initialFile(data);
        this.size = data.length;
    }

    /**
     * uses the file
     */
    private synchronized Boolean use() {
        if (isDeleted) {
            return false;
        }
        refCount++;
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
            File file = new File(getCacheLocation());
            if (isModified) {
                byte[] data = getData(file);
                file.delete();
                isDeleted = true;
                Proxy.getCache().releaseSize(size);
                updateCache(data);
                uploadToServer(data);
            }else{
                file.delete();
                isDeleted = true;
                Proxy.getCache().releaseSize(size);
            }
        }
    }

    /**
     * Open the file as a OpenFile object
     * Close OpenFile object to release the file, not CacheFileVersion
     * @param read
     * @param write
     * @return
     */
    public FileOpenResult open(Boolean read, Boolean write) {
        if(!use()){
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
        if (raf == null) {
            return null;
        }
        return new FileOpenResult(ResCode.SUCCESS, new OpenFile(read, write, this, raf));
    }

    public CacheFileVersion getWriteCopy() {
        if(!use()){ // return null if file is deleted
            return null;
        }
        byte[] data = getData(new File(getCacheLocation()));
        CacheFileVersion writeCopy = new CacheFileVersion(relativePath, UUID.randomUUID(), true, true, data, 0);
        release(); // release the original file
        return writeCopy;
    }

    public long getRefCount() {
        return refCount;
    }

    public long getSize() {
        if(isDeleted){
            return 0;
        }
        return size;
    }

    public UUID getVerId() {
        if(isDeleted){
            return null;
        }
        return verId;
    }

    public void setSize(long size) {
        isModified = true;
        if (size > this.size) {
            Proxy.getCache().requestSize(size - this.size);
        } else if (size < this.size) {
            Proxy.getCache().releaseSize(this.size - size);
        }
    }

    private String getCacheLocation() {
        return Proxy.getCache().getCacheDir() + relativePath.replace("/", "_") + "." + verId.toString();
    }

    /**
     * Uploads modfied file to server
     * 
     * @param data
     */
    private void uploadToServer(byte[] data) {
        try {
            Proxy.getServer().putFile(relativePath, data, verId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * Reads data from file
     * 
     * @param file
     * @return
     */
    private byte[] getData(File file) {
        byte[] data = new byte[(int) file.length()];
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            fileInputStream.read(data);
            fileInputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return data;
    }

    /**
     * Updates cache with new data
     * 
     * @param data
     */
    private void updateCache(byte[] data) {
        Proxy.getCache().updateFile(relativePath, data, verId);
    }

    private void initialFile(byte[] data) {
        if (data == null) {
            data = new byte[0];
        }
        Proxy.getCache().requestSize(data.length); // request size from cache
        File file = new File(getCacheLocation());
        try {
            file.createNewFile();
            java.io.FileOutputStream fileOutputStream = new java.io.FileOutputStream(file);
            fileOutputStream.write(data);
            fileOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
