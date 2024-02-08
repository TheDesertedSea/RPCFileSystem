import java.io.File;
import java.util.Arrays;
import java.util.List;

public class UnlinkHandler{
    private FDTable fdTable;
    public UnlinkHandler(FDTable fdTable) {
        this.fdTable = fdTable;
    }

    public int unlink(String path) {
        int res = checkPath(path);
        if(res < 0) {
            return res;
        }
        File file = new File(path);
        if(!file.exists()) {
            return FileHandling.Errors.ENOENT;
        }
        
        try{
            file.delete();
        } catch (SecurityException e) {
            return Errno.EACCES;
        } catch (Exception e) {
            System.out.println(e);
            System.exit(-1);
        }
        return 0;
    }

    public int checkPath(String path) {
        if(path == null || path.isEmpty()) {
            return FileHandling.Errors.EINVAL;
        }

        List<String> pathList = Arrays.asList(path.split("/"));

        String curPath;
        if(path.startsWith("/")) {
            curPath = "/";
        } else {
            curPath = "";
        }
        for(int i = 0; i < pathList.size() - 1; i++) {
            if(pathList.get(i).isEmpty()) {
                continue;
            }

            curPath += pathList.get(i);
            File file = new File(curPath);
            if(!file.exists()) {
                return FileHandling.Errors.ENOENT;
            }

            if(!file.isDirectory()) {
                return FileHandling.Errors.ENOTDIR;
            }

            curPath += "/";
        }

        File file = new File(path);
        if(file.isDirectory()) {
            return FileHandling.Errors.EISDIR;
        }

        return 0;
    }
}