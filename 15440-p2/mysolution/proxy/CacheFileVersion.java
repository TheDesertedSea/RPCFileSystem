import java.util.UUID;

public class CacheFileVersion {
    private UUID versionId;
    private String path;
    private long size;
    private Boolean modified;
    private Boolean canRead;
    private Boolean canWrite;
    private long refCount;
    private Boolean isDeleted;

    public CacheFileVersion(UUID versionId, Boolean canRead, Boolean canWrite, long size){
        this.versionId = versionId;
        this.size = size;
        this.canRead = canRead;
        this.canWrite = canWrite;
        this.modified = false;
        this.refCount = 0;
    }

    public UUID getVersionId(){
        return versionId;
    }

    public synchronized Boolean use(){
        if (isDeleted){
            return false;
        }
        refCount++;
        return true;
    }

    public synchronized void release(){
        
    }
}
