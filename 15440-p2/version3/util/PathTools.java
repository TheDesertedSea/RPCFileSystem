import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * Path tools
 * 
 * Used to check if the path is valid or change the path to other forms
 */
public class PathTools {

    /**
     * Normalize the path, also remove the "./" at the beginning of the path
     * 
     * @param {String} path to be normalized
     * @return {String} normalized path
     */
    public static String normalizePath(String pathStr) {
        Path pathObj = Paths.get(pathStr);
        String normalizedPathStr = pathObj.normalize().toString();
        if (normalizedPathStr.startsWith("./")) {
            return normalizedPathStr.substring(2);
        }
        return normalizedPathStr;
    }

    /**
     * Get the absolute path, also normalize the path
     * 
     * @param pathStr     {@link String} path to be converted to absolute path
     * @param rootPathStr {@link String} root path
     * @return {@link String} absolute path
     */
    public static String getAbsolutePath(String pathStr, String rootPathStr) {
        Path pathObj = Paths.get(pathStr);
        if (pathObj.isAbsolute()) {
            return pathObj.normalize().toString();
        } else {
            return Paths.get(rootPathStr, pathStr).normalize().toString();
        }
    }

    /**
     * Get the relative path, remove the root path components from the absolute path
     * 
     * @param absolutePathStr {@link String} absolute path
     * @param rootPathStr     {@link String} root path
     * @return {@link String} relative path
     */
    public static String getRelativePath(String absolutePathStr, String rootPathStr) {
        return absolutePathStr.replace(rootPathStr, "");
    }

    /**
     * Check if the component of the absolute path is valid, except the last
     * component
     * 
     * @param absolutePathStr {@link String} absolute path
     * @param rootPathStr     {@link String} root path
     * @return result code
     */
    public static int checkPath(String absolutePathStr, String rootPathStr) {
        if (absolutePathStr == null || absolutePathStr.isEmpty()) {
            return ResCode.EINVAL; // Invalid absolute path
        }

        if (!absolutePathStr.startsWith(rootPathStr)) {
            return ResCode.EPERM; // This path is not within the root directory
        }

        /* Check every component of this path, except the last one */
        List<String> pathList = Arrays.asList(absolutePathStr.split("/"));
        String curPath = rootPathStr;
        for (int i = rootPathStr.split("/").length; i < pathList.size() - 1; i++) {
            if (pathList.get(i).isEmpty()) {
                continue;
            }

            curPath += pathList.get(i);
            File file = new File(curPath);
            if (!file.exists()) {
                // This component does not exist
                return ResCode.ENOENT;
            }

            if (!file.isDirectory()) {
                // This component is not a directory
                return ResCode.ENOTDIR;
            }

            curPath += "/";
        }

        return ResCode.SUCCESS;
    }
}
