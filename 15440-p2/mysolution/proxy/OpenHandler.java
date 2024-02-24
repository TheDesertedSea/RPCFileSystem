import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.nio.file.Paths;
import java.util.UUID;

public class OpenHandler {
    private FDTable fdTable;

    public OpenHandler(FDTable fdTable) {
        this.fdTable = fdTable;
    }

    int open(String path, FileHandling.OpenOption option) {
        Boolean bWrite = false;
        if (option != FileHandling.OpenOption.READ) {
            bWrite = true;
        }
        String normalizedPath = Paths.get(path).normalize().toString();
        if(normalizedPath.startsWith("./"))
        {
            normalizedPath = normalizedPath.substring(2);
        }
        CacheFileVersion newestVersion = Proxy.getCache().getNewestVersion(normalizedPath, bWrite);
        FileGetResult res = null;
        try {
            res = Proxy.getServer().getFile(path, newestVersion == null ? null : newestVersion.getVersionId());
        } catch (Exception e) {
            e.printStackTrace();
            if (newestVersion != null) {
                newestVersion.release();
            }
            return ResCode.EINVAL;
        }

        if (res.getResCode() < 0) {
            if (newestVersion != null) {
                newestVersion.release();
            }
            return res.getResCode();
        }

        if (res.getResCode() == Server.IS_DIR) {
            if (option == FileHandling.OpenOption.READ) {
                int fd = fdTable.getFreeFd();
                if (fd < 0) {
                    return ResCode.EMFILE;
                }
                OpenFile openFile = new OpenFile(null, null, true);
                fdTable.addOpenFile(fd, openFile);
                return fd;
            } else {
                return ResCode.EISDIR;
            }
        }

        Boolean exists = newestVersion != null;
        if (res.getResCode() == Server.NEW_VERSION) {
            if(newestVersion != null){
                newestVersion.release();
            }
            newestVersion = Proxy.getCache().updateNewestVersion(res.getRelativePath(), res.getVerId(), res.getCanRead(),
                    res.getCanWrite(), res.getData(), bWrite);
            if (newestVersion == null) {
                exists = false;
            }else{
                exists = true;
            }
        } else if (res.getResCode() == Server.NOT_EXIST) {
            if (newestVersion != null) {
                newestVersion.release();
            }
            Proxy.getCache().removeFile(res.getRelativePath());
            exists = false;
        }

        String mode = "";
        switch (option) {
            case READ:
                if (!exists) {
                    return ResCode.ENOENT;
                }
                if (!newestVersion.canRead()) {
                    newestVersion.release();
                    return ResCode.EACCES;
                }
                mode = "r";
                break;
            case WRITE:
                if (!exists) {
                    return ResCode.ENOENT;
                }
                if (!newestVersion.canWrite() || !newestVersion.canRead()) {
                    newestVersion.release();
                    return ResCode.EACCES;
                }
                mode = "rw";
                break;
            case CREATE:
                if (exists) {
                    if (!newestVersion.canWrite() || !newestVersion.canRead()) {
                        newestVersion.release();
                        return ResCode.EACCES;
                    }
                } else {
                    newestVersion = new CacheFileVersion(Proxy.getCache(), res.getRelativePath(), UUID.randomUUID(), true, true, 1, null);
                    newestVersion.use();
                }
                mode = "rw";
                break;
            case CREATE_NEW:
                if (exists) {
                    newestVersion.release();
                    return ResCode.EEXIST;
                }
                newestVersion = new CacheFileVersion(Proxy.getCache(), res.getRelativePath(), UUID.randomUUID(), true, true, 1, null);
                newestVersion.use();
                mode = "rw";
                break;
            default:
                newestVersion.release();
                return ResCode.EINVAL;
        }

        int fd = fdTable.getFreeFd();
        if (fd < 0) {
            newestVersion.release();
            return ResCode.EMFILE;
        }

        try {
            RandomAccessFile file = new RandomAccessFile(newestVersion.getActualPath(), mode);
            OpenFile openFile = new OpenFile(file, newestVersion, false);
            fdTable.addOpenFile(fd, openFile);
        } catch (FileNotFoundException e) {
            newestVersion.release();
            return ResCode.EACCES;
        }

        return fd;
    }
}
