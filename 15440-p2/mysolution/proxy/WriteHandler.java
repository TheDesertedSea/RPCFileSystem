/**
 * WriteHandler.java
 * 
 * @author Cundao Yu <cundaoy@andrew.cmu.edu>
 */

import java.io.IOException;
import java.io.RandomAccessFile;

public class WriteHandler {
    
    private FDTable fdTable;

    public WriteHandler(FDTable fdTable) {
        this.fdTable = fdTable;
    }

    public long write(int fd, byte[] buf) {
        if(buf == null) {
            return Errno.EINVAL;
        }

        if(!fdTable.verifyFd(fd)) {
            return Errno.EBADF;
        }

        OpenFile file = fdTable.getOpenFile(fd);
        if(file.isDirectory()) {
            return Errno.EBADF;
        }
        if(!file.canWrite()) {
            return Errno.EBADF;
        }

        try {
            file.write(buf);
            return buf.length;
        } catch (IOException e) {
            System.out.println(e);
            System.exit(-1);
        }
        return 0;
    }

}
