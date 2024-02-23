import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

public class CacheFileVersion {
    private Cache cache;
    private UUID versionId;
    private String path;
    private long size;
    private Boolean modified;
    private Boolean canRead;
    private Boolean canWrite;
    private long refCount;
    private Boolean isDeleted;
    private long writeCopyId;

    public CacheFileVersion(Cache cache, UUID versionId, Boolean canRead, Boolean canWrite, long size,
            long writeCopyId, byte[] data) {
        this.cache = cache;
        this.versionId = versionId;
        this.size = size;
        this.canRead = canRead;
        this.canWrite = canWrite;
        this.modified = false;
        this.refCount = 0;
        this.isDeleted = false;
        this.writeCopyId = writeCopyId;
        
        File file = new File(getActualPath());
        try {
            file.createNewFile();
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(data);
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public UUID getVersionId() {
        return versionId;
    }

    public synchronized Boolean use() {
        if (isDeleted) {
            return false;
        }
        refCount++;
        return true;
    }

    public synchronized void release() {
        refCount--;
        if (refCount == 0) {
            isDeleted = true;
            File file = new File(getActualPath());
            if(modified) {
                byte[] data = new byte[(int) size];
                try {
                    FileInputStream fis = new FileInputStream(file);
                    fis.read(data);
                    fis.close();
                    Proxy.getServer().putFile(path, data);
                } catch (IOException e) {
                    e.printStackTrace();
                } 
            }
            file.delete();
            cache.giveBackSpace(size);
        }
    }

    public CacheFileVersion cloneWriteCopy() {
        byte[] data = new byte[(int) size];
        File file = new File(getActualPath());
        try {
            FileInputStream fis = new FileInputStream(file);
            fis.read(data);
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new CacheFileVersion(cache, this.versionId, this.canRead, this.canWrite, this.size, this.writeCopyId + 1, data);
    }

    public String getActualPath() {
        String name = path.replace("/", "_");
        return cache.getCacheDir() + name + "." + versionId.toString() + "." + writeCopyId;
    }

    public Boolean canRead() {
        return canRead;
    }

    public Boolean canWrite() {
        return canWrite;
    }

    public long getSize() {
        return size;
    }

    public Boolean isDeleted() {
        return isDeleted;
    }

    public Boolean isModified() {
        return modified;
    }

    public void setModified(Boolean modified) {
        this.modified = modified;
    }

    public void needWrite(long size) {
        cache.takeSpace(size);
    }
}
