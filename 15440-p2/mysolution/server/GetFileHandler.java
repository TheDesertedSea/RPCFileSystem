import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class GetFileHandler {

    private ServerFileTable fileTable;
    private String rootPath;
    private int componentCountRootPath;

    public GetFileHandler(ServerFileTable fileTable, String rootPath) {
        this.fileTable = fileTable;
        this.rootPath = rootPath;
        this.componentCountRootPath = this.rootPath.split("/").length;
    }

    public FileGetResult getFile(String requestPath, UUID proxyVersion) {
        Logger.log("Server: getFile(" + requestPath + ")");
        Path absolutePathObj = Paths.get(requestPath);
        if (!absolutePathObj.isAbsolute()) {
            absolutePathObj = Paths.get(rootPath, requestPath);
        }
        absolutePathObj = absolutePathObj.normalize();

        int res = checkPath(absolutePathObj.toString());
        if (res < 0) {
            Logger.log("Server: getFile(" + requestPath + ") = " + res);
            return new FileGetResult(res, null, null, false, false, null);
        }
        String normalizedPath = absolutePathObj.toString().replace(rootPath, "");
        Logger.log("Server: getFile(" + requestPath + "), normalizedPath = " + normalizedPath);

        /* Lock Version */
        UUID newestVersion = fileTable.startGet(normalizedPath);
        File file = absolutePathObj.toFile();
        Logger.logFileInfo(file);
        if (newestVersion == null) {
            if (file.isDirectory()) {
                return new FileGetResult(Server.IS_DIR, normalizedPath, null, false, false, null);
            }
            if(!file.exists()){
                return new FileGetResult(Server.NOT_EXIST, normalizedPath, null, false, false, null);
            }
            fileTable.addNewFileToManage(normalizedPath);
            newestVersion = fileTable.startGet(normalizedPath);
            if(newestVersion == null){
                return new FileGetResult(Server.NOT_EXIST, normalizedPath, null, false, false, null);
            }
        } else if (proxyVersion != null && proxyVersion.equals(newestVersion)) {
            fileTable.endGet(normalizedPath); // Unlock Version
            return new FileGetResult(Server.NO_UPDATE, normalizedPath, newestVersion, file.canRead(), file.canWrite(),
                    null);
        }

        byte[] data = new byte[(int) file.length()];
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            fileInputStream.read(data);
            fileInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        fileTable.endGet(normalizedPath); // Unlock Version
        return new FileGetResult(Server.NEW_VERSION, normalizedPath, newestVersion, file.canRead(), file.canWrite(),
                data);
    }

    private int checkPath(String absolutePath) {
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
