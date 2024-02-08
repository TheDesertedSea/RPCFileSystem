import java.io.File;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class OpenHandler{

    private FDTable fdTable;   

    public OpenHandler(FDTable fdTable) {
        this.fdTable = fdTable;
    }

    public int open(String path, FileHandling.OpenOption o) {
        int res = checkPath(path);
        if(res < 0) {
            return res;
        }

        File file = new File(path);
        String mode = "";
        Boolean canRead = false;
        Boolean canWrite = false;
        switch(o)
        {
            case CREATE:
                if(!file.exists())
                {
                    try {
                        file.createNewFile();
                    } catch (SecurityException e) {
                        return Errno.EACCES;
                    } catch (Exception e) {
                        System.out.println(e);
                        System.exit(-1);
                    }
                }
                mode = "rw";
                canRead = true;
                canWrite = true;
                break;
            case CREATE_NEW:
                if(file.exists()) {
                    return FileHandling.Errors.EEXIST;
                }
                mode = "rw";
                canRead = true;
                canWrite = true;
                break;
            case READ:
                if(!file.exists()) {
                    return FileHandling.Errors.ENOENT;
                }
                mode = "r";
                canRead = true;
                canWrite = false;
                break;
            case WRITE:
                if(!file.exists()) {
                    return FileHandling.Errors.ENOENT;
                }
                mode = "rw";
                canRead = true;
                canWrite = true;
                break;
        }

        int fd = fdTable.getFreeFd();
        if(fd < 0) {
            return FileHandling.Errors.EMFILE;
        }

        try {
            RandomAccessFile randomAccessFile = new RandomAccessFile(file, mode);
            OpenFile openFile = new OpenFile(randomAccessFile, canRead, canWrite);
            fdTable.addOpenFile(fd, openFile);
        } catch (SecurityException e) {
            return Errno.EACCES;
        } catch (Exception e) {
            System.out.println(e);
            System.exit(-1);
        }
        
        return fd;
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
