/**
 * CloseHandler.java
 * 
 * @author Cundao Yu <cundaoy@andrew.cmu.edu>
 */

import java.io.IOException;
import java.io.RandomAccessFile;

public class CloseHandler {
    
    private FDTable fdTable;  

    public CloseHandler(FDTable fdTable) {
        this.fdTable = fdTable;
    }

    public int close(int fd) {
        if(!fdTable.verifyFd(fd)) {
            return FileHandling.Errors.EBADF;
        }

        OpenFile file = fdTable.getOpenFile(fd);
        try {
            file.close();
        } catch (IOException e) {
            System.out.println(e);
            System.exit(-1);
        }

        fdTable.removeOpenFile(fd);
        return 0;
    }
}
