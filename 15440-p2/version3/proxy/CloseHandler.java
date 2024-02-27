/**
 * CloseHandler.java
 * 
 * @author Cundao Yu <cundaoy@andrew.cmu.edu>
 */

/**
 * Handler for close operation
 */
public class CloseHandler {
    
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
    public CloseHandler(FDTable fdTable) {
        this.fdTable = fdTable;
    }

    /**
     * Close a file
     * 
     * @param fd {@link Integer} File descriptor
     * @return {@link Integer} 0 if success, otherwise a negative error code
     */
    public int close(int fd) {
        if(!fdTable.verifyFd(fd)) {
            return ResCode.EBADF;
        }

        OpenFile file = fdTable.getOpenFile(fd);
        file.close();
        fdTable.removeOpenFile(fd);
        return 0;
    }
}
