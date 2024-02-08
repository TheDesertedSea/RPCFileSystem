/**
 * ReadHandler.java
 * 
 * @author Cundao Yu <cundaoy@andrew.cmu.edu>
 */

import java.io.IOException;
import java.io.RandomAccessFile;

public class ReadHandler {
    
    private FDTable fdTable;

    public ReadHandler(FDTable fdTable) {
        this.fdTable = fdTable;
    }

    public long read(int fd, byte[] buf) {
        if(buf == null) {
            return FileHandling.Errors.EINVAL;
        }

        if(!fdTable.verifyFd(fd)) {
            return FileHandling.Errors.EBADF;
        }

        OpenFile file = fdTable.getOpenFile(fd);
        if(file.isDirectory()) {
            return FileHandling.Errors.EISDIR;
        }
        try {
            long readCount = file.read(buf);
            return readCount == -1 ? 0 : readCount;
        } catch (IOException e) {
            System.out.println(e);
            System.exit(-1);
        }
        return 0;
    }

}
