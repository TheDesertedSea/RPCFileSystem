public class OpenHandler {
    private FDTable fdTable;

    public OpenHandler(FDTable fdTable) {
        this.fdTable = fdTable;
    }

    int open(String path, FileHandling.OpenOption option){
        int fd = fdTable.getFreeFd();
        if (fd < 0) {
            return ResCode.EMFILE;
        }

        Boolean write = option != FileHandling.OpenOption.READ;
        Boolean read = true;
        Boolean create = option == FileHandling.OpenOption.CREATE || option == FileHandling.OpenOption.CREATE_NEW;
        Boolean exclusive = option == FileHandling.OpenOption.CREATE_NEW;
        String normalizedPath = PathTools.normalizePath(path);

        FileOpenResult result = Proxy.getCache().checkAndOpen(path, read, write, create, exclusive);
        if (result.getResCode() < 0) {
            return result.getResCode();
        }

        fdTable.addOpenFile(fd, result.getOpenFile());
        return fd;
    }
}
