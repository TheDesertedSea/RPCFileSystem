/**
 * UnlinkHandler.java
 * 
 * @author Cundao Yu <cundaoy@andrew.cmu.edu>
 */

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.List;

public class UnlinkHandler{
    private String cacheDir;
    private int componentCountCachePath;
    public UnlinkHandler(String cacheDir) {
        this.cacheDir = cacheDir;
        this.componentCountCachePath = cacheDir.split("/").length;
    }

    public int unlink(String path) {
        Path absolutePathObj = Paths.get(path);
        if (!absolutePathObj.isAbsolute()) {
            absolutePathObj = Paths.get(cacheDir, path);
        }
        absolutePathObj = absolutePathObj.normalize();
        int res = checkPath(absolutePathObj.toString());
        if (res < 0) {
            return res;
        }
        String normalizedPath = absolutePathObj.relativize(Paths.get(cacheDir)).toString();
        Proxy.getCache().removeFile(normalizedPath);
        try {
            Proxy.getServer().removeFile(normalizedPath);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private int checkPath(String absolutePath) {
        if (absolutePath == null || absolutePath.isEmpty()) {
            return Errno.EINVAL;
        }

        if (!absolutePath.startsWith(cacheDir)) {
            return Errno.EPERM;
        }

        List<String> pathList = Arrays.asList(absolutePath.split("/"));
        String curPath = cacheDir;
        for (int i = componentCountCachePath; i < pathList.size() - 1; i++) {
            if (pathList.get(i).isEmpty()) {
                continue;
            }

            curPath += pathList.get(i);
            File file = new File(curPath);
            if (!file.exists()) {
                return Errno.ENOENT;
            }

            if (!file.isDirectory()) {
                return Errno.ENOTDIR;
            }

            curPath += "/";
        }

        return 0;
    }
}