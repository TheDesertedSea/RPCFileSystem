import java.util.concurrent.ConcurrentHashMap;

public class FileRefCountsMap {
    private static final ConcurrentHashMap<String, Integer> fileRefCounts = new ConcurrentHashMap<String, Integer>();

    public static int getRefCount(String path) {
        Integer refCount = fileRefCounts.get(path);
        if(refCount == null) {
            return 0;
        }
        return refCount;
    }

    public static void incrementRefCount(String path) {
        Integer refCount = fileRefCounts.get(path);
        if(refCount == null) {
            fileRefCounts.put(path, 1);
            return;
        }

        fileRefCounts.put(path, refCount + 1);
    }

    public static void decrementRefCount(String path) {
        Integer refCount = fileRefCounts.get(path);
        if (refCount == 1) {
            fileRefCounts.remove(path);
            return;
        }

        fileRefCounts.put(path, refCount - 1);
    }

}
