import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class RemoveFileHandler {
    private ServerFileTable fileTable;
    private String rootPath;
    private int componentCountRootPath;

    public RemoveFileHandler(ServerFileTable fileTable, String rootPath) {
        this.fileTable = fileTable;
        this.rootPath = rootPath;
        this.componentCountRootPath = this.rootPath.split("/").length;
    }

    public FileRemoveResult removeFile(String path) {
        Path absolutePathObj = Paths.get(path);
        if (!absolutePathObj.isAbsolute()) {
            absolutePathObj = Paths.get(rootPath, path);
        }
        absolutePathObj = absolutePathObj.normalize();

        int res = checkPath(absolutePathObj.toString());
        if (res < 0) {
            return new FileRemoveResult(res, null);
        }

        String normalizedPath = absolutePathObj.toString().replace(rootPath, "");
        int result = fileTable.remove(normalizedPath);
        return new FileRemoveResult(result, normalizedPath);
    }

    private int checkPath(String absolutePath) {
        Logger.log("Server: checkPath(" + absolutePath + ")");
        if (absolutePath == null || absolutePath.isEmpty()) {
            return ResCode.EINVAL;
        }

        if (!absolutePath.startsWith(rootPath)) {
            return ResCode.EPERM;
        }

        List<String> pathList = Arrays.asList(absolutePath.split("/"));
        String curPath = rootPath;
        for (int i = componentCountRootPath; i < pathList.size() - 1; i++) {
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

        return 0;
    }
}
