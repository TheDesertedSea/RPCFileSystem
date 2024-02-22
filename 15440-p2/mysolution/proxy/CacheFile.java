public class CacheFile {
    private String path;
    private CacheFileVersion newestVersion;
    private CacheFile prev;
    private CacheFile next;

    public CacheFile(String path, CacheFileVersion newestVersion) {
        this.path = path;
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

    public void setNewestVersion(CacheFileVersion newestVersion) {
        newestVersion.use();
        this.newestVersion = newestVersion;
    }
    
    public CacheFile getNext() {
        return null;
    }

    public void setNext(CacheFile next) {
        return;
    }

    public CacheFile getPrev() {
        return null;
    }

    public void setPrev(CacheFile prev) {
        return;
    }


}
