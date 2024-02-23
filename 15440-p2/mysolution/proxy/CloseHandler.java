/**
 * CloseHandler.java
 * 
 * @author Cundao Yu <cundaoy@andrew.cmu.edu>
 */

import java.io.IOException;

public class CloseHandler {
    
    private FDTable fdTable;  

    public CloseHandler(FDTable fdTable) {
        this.fdTable = fdTable;
    }

    public int close(int fd) {
        if(!fdTable.verifyFd(fd)) {
            return Errno.EBADF;
        }

        OpenFile file = fdTable.getOpenFile(fd);
        try {
            file.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        fdTable.removeOpenFile(fd);
        return 0;
    }
}
