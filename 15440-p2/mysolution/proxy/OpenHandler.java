
public class OpenHandler {
    private FDTable fdTable;  

    public OpenHandler(FDTable fdTable) {
        this.fdTable = fdTable;
    }

    int open(String path, FileHandling.OpenOption option){
        Boolean bWrite = false;
        if (option != FileHandling.OpenOption.READ)
        {
            bWrite = true;
        }
        CacheFileVersion newestVersion = Proxy.getCache().getNewestVersion(path, bWrite);
        FileGetResult res = Proxy.getServer().getFile(path, newestVersion == null ? null : newestVersion.getVersionId());
        if(res.getResCode() < 0)
        {
            return res.getResCode();
        }

        if(res.getResCode() == Server.IS_DIR)
        {
            if(option == FileHandling.OpenOption.READ)
            {
                int fd = fdTable.getFreeFd();
                if(fd < 0)
                {
                    return Errno.EMFILE;
                }
                OpenFile openFile = new OpenFile(null, null, true);
                fdTable.addOpenFile(fd, openFile);
                return fd;
            }else{
                return Errno.EISDIR;
            }
        }

        if(res.getResCode() == Server.NEW_VERSION)
        {
            newestVersion = Proxy.getCache().updateNewestVersion(path, res.getVersionId(), res.getCanRead(), res.getCanWrite(), res.getData().length, res.getData(), bWrite);
        }else{}

        if(newestVersion == null)
        {
            return Errno.ENOENT;
        }

        
    }
}
