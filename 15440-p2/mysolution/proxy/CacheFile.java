public class CacheFile {
    private String path;
    private CacheFileVersion newestVersion;
    private CacheFile prev;
    private CacheFile next;

    public CacheFile(String path, CacheFileVersion newestVersion) {
        this.path = path;
        newestVersion.use();
        this.newestVersion = newestVersion;
        this.prev = null;
        this.next = null;
    }

    public CacheFileVersion getNewestVersion() {
        if (newestVersion == null) {
            return null;
        }

        if(!newestVersion.use()){
            return null;
        }
        return newestVersion;
    }

    public void removeNewestVersion() {
        if (newestVersion != null) {
            newestVersion.release();
        }
        newestVersion = null;
    }

    public void setNewestVersion(CacheFileVersion newestVersion) {
        newestVersion.use();
        this.newestVersion = newestVersion;
    }

    public void remove(){
        if (newestVersion != null) {
            newestVersion.release();
        }
        newestVersion = null;
    }
    
    public CacheFile getNext() {
        return next;
    }

    public void setNext(CacheFile next) {
        this.next = next;
    }

    public CacheFile getPrev() {
        return prev;
    }

    public void setPrev(CacheFile prev) {
        this.prev = prev;
    }

    public String getPath() {
        return path;
    }
}
