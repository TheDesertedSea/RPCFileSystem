import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class PathTools {

    /**
     * Normalize the path, also remove the "./" at the beginning of the path
     * @param pathStr
     * @return
     */
    public static String normalizePath(String pathStr){
        Path pathObj = Paths.get(pathStr);
        String normalizedPathStr = pathObj.normalize().toString();
        if (normalizedPathStr.startsWith("./")) {
            return normalizedPathStr.substring(2);
        }
        return normalizedPathStr;
    }

    /**
     * Get the absolute path, also normalize the path
     * @param pathStr
     * @param rootPathStr
     * @return
     */
    public static String getAbsolutePath(String pathStr, String rootPathStr){
        Path pathObj = Paths.get(pathStr);
        if (pathObj.isAbsolute()) {
            return pathObj.normalize().toString();
        } else {
            return Paths.get(rootPathStr, pathStr).normalize().toString();
        }
    }

    /**
     * Get the relative path, remove the root path components
     * @param absolutePathStr
     * @param rootPathStr
     * @return
     */
    public static String getRelativePath(String absolutePathStr, String rootPathStr){
        return absolutePathStr.replace(rootPathStr, "");
    }

    /**
     * Check if the component of the absolute path is valid, except the last component
     * @param absolutePathStr
     * @param rootPathStr
     * @return ResCode
     */
    public static int checkPath(String absolutePathStr, String rootPathStr){
        if (absolutePathStr == null || absolutePathStr.isEmpty()) {
            return ResCode.EINVAL;
        }

        if (!absolutePathStr.startsWith(rootPathStr)) {
            return ResCode.EPERM;
        }

        List<String> pathList = Arrays.asList(absolutePathStr.split("/"));
        String curPath = rootPathStr;
        for (int i = rootPathStr.split("/").length; i < pathList.size() - 1; i++) {
            if (pathList.get(i).isEmpty()) {
                continue;
            }

            curPath += pathList.get(i);
            File file = new File(curPath);
            if (!file.exists()) {
                return ResCode.ENOENT;
            }

            if (!file.isDirectory()) {
                return ResCode.ENOTDIR;
            }

            curPath += "/";
        }

        return ResCode.SUCCESS;
    }
}
