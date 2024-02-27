
/**
 * ReadHandler.java
 * 
 * @author Cundao Yu <cundaoy@andrew.cmu.edu>
 */
import java.io.IOException;

/**
 * Handler for read operation
 */
public class ReadHandler {
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
    public ReadHandler(FDTable fdTable) {
        this.fdTable = fdTable;
    }

    /**
     * Read from a file
     * 
     * @param fd  File descriptor
     * @param buf Buffer to store the content
     * @return Number of bytes read if success, otherwise a negative error code
     */
    public long read(int fd, byte[] buf) {
        if (buf == null) {
            return ResCode.EINVAL;
        }

        if (!fdTable.verifyFd(fd)) {
            return ResCode.EBADF;
        }

        OpenFile file = fdTable.getOpenFile(fd);
        if (file.isDirectory()) {
            return ResCode.EISDIR;
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
