import java.util.concurrent.ConcurrentHashMap;

public class Cache {
    private String cacheDir;
    private long cacheSize;

    private ConcurrentHashMap<String, CacheItem> cache;

    public Cache(String cacheDir, long cacheSize) {
        this.cacheDir = cacheDir;
        this.cacheSize = cacheSize;
        this.cache = new ConcurrentHashMap<String, CacheItem>();
    }

    public String getLocalFilePath(String serverPath){
        if(cache.containsKey(serverPath)){
            return cache.get(serverPath).getLocalPath();
        }
        return null;
    }

    public void addCacheItem(String serverPath, String localPath, long version, Boolean isDirectory, Boolean canRead, Boolean canWrite){
        CacheItem item = new CacheItem(serverPath, version, localPath, isDirectory, canRead, canWrite);
        cache.put(serverPath, item);
    }

    public String generateLocalPath(String serverPath){
        return cacheDir + serverPath.replace("/", "_");
    }
}
