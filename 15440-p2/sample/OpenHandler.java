
/**
 * OpenHandler.java
 * 
 * @author Cundao Yu <cundaoy@andrew.cmu.edu>
 */

import java.io.File;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class OpenHandler {

    private FDTable fdTable;

    public OpenHandler(FDTable fdTable) {
        this.fdTable = fdTable;
    }

    public int open(String path, FileHandling.OpenOption o) {
        int res = checkPath(path);
        if (res < 0) {
            return res;
        }

        File file = new File(path);
        Logger.logFileInfo(file);
        String mode = "";
        Boolean canRead = false;
        Boolean canWrite = false;
        Boolean isDirectory = false;
        switch (o) {
            case CREATE:
                if (path.endsWith("/")) {
                    return FileHandling.Errors.EISDIR;
                }
                if (!file.exists()) {
                    try {
                        file.createNewFile();
                    } catch (SecurityException e) {
                        return Errno.EACCES;
                    } catch (Exception e) {
                        System.out.println(e);
                        System.exit(-1);
                    }
                } else if (file.isDirectory()) {
                    return FileHandling.Errors.EISDIR;
                }
                mode = "rw";
                canRead = true;
                canWrite = true;
                break;
            case CREATE_NEW:
                if (path.endsWith("/")) {
                    return FileHandling.Errors.EISDIR;
                }
                if (file.exists()) {
                    return FileHandling.Errors.EEXIST;
                }
                try {
                    file.createNewFile();
                } catch (SecurityException e) {
                    return Errno.EACCES;
                } catch (Exception e) {
                    System.out.println(e);
                    System.exit(-1);
                }
                mode = "rw";
                canRead = true;
                canWrite = true;
                break;
            case READ:
                if (path.endsWith("/")) {
                    File tempFile = new File(path.substring(0, path.length() - 1));
                    if (tempFile.exists() && !tempFile.isDirectory()) {
                        return FileHandling.Errors.ENOTDIR;
                    }
                }
                if (!file.exists()) {
                    return FileHandling.Errors.ENOENT;
                }
                mode = "r";
                canRead = true;
                canWrite = false;
                isDirectory = file.isDirectory();
                break;
            case WRITE:
                if (path.endsWith("/")) {
                    return FileHandling.Errors.EISDIR;
                }
                if (!file.exists()) {
                    return FileHandling.Errors.ENOENT;
                }
                if (file.isDirectory()) {
                    return FileHandling.Errors.EISDIR;
                }
                mode = "rw";
                canRead = true;
                canWrite = true;
                break;
        }

        int fd = fdTable.getFreeFd();
        if (fd < 0) {
            return FileHandling.Errors.EMFILE;
        }

        try {
            RandomAccessFile randomAccessFile = null;
            if (!isDirectory) {
                randomAccessFile = new RandomAccessFile(file, mode);
            }
            OpenFile openFile = new OpenFile(randomAccessFile, canRead, canWrite, isDirectory);
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
        if (path == null || path.isEmpty()) {
            return FileHandling.Errors.EINVAL;
        }

        List<String> pathList = Arrays.asList(path.split("/"));
        String curPath;
        if (path.startsWith("/")) {
            curPath = "/";
        } else {
            curPath = "";
        }
        for (int i = 0; i < pathList.size() - 1; i++) {
            if (pathList.get(i).isEmpty()) {
                continue;
            }

            curPath += pathList.get(i);
            File file = new File(curPath);
            if (!file.exists()) {
                return FileHandling.Errors.ENOENT;
            }

            if (!file.isDirectory()) {
                return FileHandling.Errors.ENOTDIR;
            }

            curPath += "/";
        }

        return 0;
    }
}
