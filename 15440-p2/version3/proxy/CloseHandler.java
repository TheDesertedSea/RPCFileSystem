/**
 * CloseHandler.java
 * 
 * @author Cundao Yu <cundaoy@andrew.cmu.edu>
 */

public class CloseHandler {
    
    private FDTable fdTable;  

    public CloseHandler(FDTable fdTable) {
        this.fdTable = fdTable;
    }

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
