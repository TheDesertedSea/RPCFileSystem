
/**
 * WriteHandler.java
 * 
 * @author Cundao Yu <cundaoy@andrew.cmu.edu>
 */

import java.io.IOException;

/**
 * Handler for write operation
 */
public class WriteHandler {
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
    public WriteHandler(FDTable fdTable) {
        this.fdTable = fdTable;
    }

    /**
     * Write to a file
     * 
     * @param fd  File descriptor
     * @param buf Buffer to store the content
     * @return Number of bytes written if success, otherwise a negative error code
     */
    public long write(int fd, byte[] buf) {
        if (buf == null) {
            return ResCode.EINVAL;
        }

        if (!fdTable.verifyFd(fd)) {
            return ResCode.EBADF;
        }

        OpenFile file = fdTable.getOpenFile(fd);
        if (file.isDirectory()) {
            return ResCode.EBADF;
        }
        if (!file.canWrite()) {
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
