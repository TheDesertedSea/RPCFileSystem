import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class CheckHandler {

    private String rootPath;
    private int componentCountRootPath;

    public CheckHandler(String rootPath) {
        this.rootPath = rootPath;
        this.componentCountRootPath = rootPath.split("/").length;
    }

    CheckResult check(String path, CheckOption option) {
        Path absolutePath = Paths.get(path);
        if(!absolutePath.isAbsolute())
        {
            absolutePath = Paths.get(rootPath, path);
        }
        absolutePath = absolutePath.normalize();
        
        int res = checkPath(absolutePath.toString());
        if (res < 0) {
            return new CheckResult(res, null, false, 0, false, false, false, -1);
        }

        if (path.endsWith("/"))
        {
            if(option != CheckOption.READ)
            {
                return new CheckResult(Errno.EISDIR, null, false, 0, true, false, false, -1);
            }
        }
        
        File file = absolutePath.toFile();
        Logger.logFileInfo(file);
        Logger.log("CheckHandler: " + option + " " + absolutePath.toString());
        if(file.isDirectory())
        {
            if(option != CheckOption.READ)
            {
                return new CheckResult(Errno.EISDIR, null, false, 0, true, false, false, -1);
            }else if(!file.exists())
            {
                return new CheckResult(Errno.ENOENT, null, false, 0, false, false, false, -1);
            }else if(!file.canRead())
            {
                return new CheckResult(Errno.EACCES, absolutePath.toString(), true, 0, true, false, false, -1);
            }else{
                return new CheckResult(0, absolutePath.toString(), true, 0, true, true, false, -1);
            }
        }

        switch(option)
        {
            case CREATE:
                if(!file.exists())
                {
                    return new CheckResult(0, absolutePath.toString(), false, 0, false, false, false, -1);
                }else if(!file.canRead() || !file.canWrite())
                {
                    return new CheckResult(Errno.EACCES, absolutePath.toString(), true, 0, false, file.canRead(), file.canWrite(), -1);
                }
                break;
            case CREATE_NEW:
                if(file.exists())
                {
                    return new CheckResult(Errno.EEXIST, absolutePath.toString(), true, 0, false, file.canRead(), file.canWrite(), -1);
                }else if(!file.canRead() || !file.canWrite())
                {
                    return new CheckResult(Errno.EACCES, absolutePath.toString(), true, 0, false, file.canRead(), file.canWrite(), -1);
                }
                break;
            case READ:
                if(!file.exists())
                {
                    return new CheckResult(Errno.ENOENT, absolutePath.toString(), false, 0, false, false, false, -1);
                }else if(!file.canRead())
                {
                    return new CheckResult(Errno.EACCES, absolutePath.toString(), true, 0, false, file.canRead(), file.canWrite(), -1);
                }
                break;
            case WRITE:
                if(!file.exists())
                {
                    return new CheckResult(Errno.ENOENT, absolutePath.toString(), false, 0, false, false, false, -1);
                }else if(!file.canRead() || !file.canWrite())
                {
                    return new CheckResult(Errno.EACCES, absolutePath.toString(), true, 0, false, file.canRead(), file.canWrite(), -1);
                }
                break;
        }
        
        return new CheckResult(0, absolutePath.toString(), true, 0, false, file.canRead(), file.canWrite(), -1);    
    }

    public int checkPath(String absolutePath) {
        if (absolutePath == null || absolutePath.isEmpty()) {
            return Errno.EINVAL;
        }

        if(!absolutePath.startsWith(rootPath))
        {
            return Errno.EPERM;
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
