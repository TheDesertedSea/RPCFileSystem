/**
 * OpenHandler.java
 * 
 * @author Cundao Yu <cundaoy@andrew.cmu.edu>
 */

/**
 * Handler for open operation
 */
public class OpenHandler {
    /**
     * {@link FDTable}
     * File descriptor table
     */
    private FDTable fdTable;

    /**
     * Constructor
     * 
     * @param fdTable {@link FDTable} File descriptor table
     */
    public OpenHandler(FDTable fdTable) {
        this.fdTable = fdTable;
    }

    /**
     * Open a file
     * 
     * @param path   File path
     * @param option Open option
     * @return File descriptor if success, otherwise a negative error code
     */
    int open(String path, FileHandling.OpenOption option) {
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
