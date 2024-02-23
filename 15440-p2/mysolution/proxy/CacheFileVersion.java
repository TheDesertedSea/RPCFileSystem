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

    public CacheFileVersion(Cache cache, String path, UUID versionId, Boolean canRead, Boolean canWrite,
            long writeCopyId, byte[] data) {
        this.cache = cache;
        this.path = path;
        this.size = data == null ? 0 : data.length;
        this.versionId = versionId;
        this.canRead = canRead;
        this.canWrite = canWrite;
        this.modified = false;
        this.refCount = 0;
        this.isDeleted = false;
        this.writeCopyId = writeCopyId;

        File file = new File(getActualPath());
        try {
            file.createNewFile();
            if (data != null) {
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(data);
                fos.close();
            }
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
        Logger.log("CacheFileVersion: use: " + path + " refCount: " + refCount);
        return true;
    }

    public synchronized void release() {
        refCount--;
        if (refCount == 0) {
            isDeleted = true;
            File file = new File(getActualPath());
            if (modified) {
                Logger.log("upload file: " + path);
                byte[] data = new byte[(int) size];
                try {
                    FileInputStream fis = new FileInputStream(file);
                    fis.read(data);
                    fis.close();
                    file.delete();
                    cache.giveBackSpace(size);
                    CacheFileVersion newest = Proxy.getCache().updateNewestVersion(path, versionId, canRead, canWrite, data, false);
                    if(newest != null)
                    {
                        newest.release();
                    }
                    Proxy.getServer().putFile(path, data, versionId);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                file.delete();
                cache.giveBackSpace(size);
            }
        }
        Logger.log("CacheFileVersion: release: " + path + " refCount: " + refCount);
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
        CacheFileVersion writeCopy = new CacheFileVersion(cache, this.path, UUID.randomUUID(), this.canRead, this.canWrite,
                this.writeCopyId + 1,
                data);
        writeCopy.use();
        this.release();
        return writeCopy;
    }

    public String getActualPath() {
        String name = path.replace("/", "_");
        return cache.getCacheDir() + name + (versionId != null ? "." + versionId.toString() : "") + "." + writeCopyId;
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

    public void writeMore(long size) {
        cache.takeSpace(size);
        this.size += size;
    }
}
