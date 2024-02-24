/**
 * WriteHandler.java
 * 
 * @author Cundao Yu <cundaoy@andrew.cmu.edu>
 */

import java.io.IOException;

public class WriteHandler {
    
    private FDTable fdTable;

    public WriteHandler(FDTable fdTable) {
        this.fdTable = fdTable;
    }

    public long write(int fd, byte[] buf) {
        if(buf == null) {
            return ResCode.EINVAL;
        }

        if(!fdTable.verifyFd(fd)) {
            return ResCode.EBADF;
        }

        OpenFile file = fdTable.getOpenFile(fd);
        if(file.isDirectory()) {
            return ResCode.EBADF;
        }
        if(!file.canWrite()) {
            return ResCode.EBADF;
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
