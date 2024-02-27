/**
 * LseekHandler.java
 * 
 * @author Cundao Yu <cundaoy@andrew.cmu.edu>
 */
import java.io.IOException;

/**
 * Handler for lseek operation
 */
public class LseekHandler {

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
    public LseekHandler(FDTable fdTable) {
        this.fdTable = fdTable;
    }

    /**
     * Change the file offset
     * 
     * @param fd  File descriptor
     * @param pos Offset
     * @param option Open option
     * @return 0 if success, otherwise a negative error code
     */
    public long lseek(int fd, long pos, FileHandling.LseekOption option) {
        if (!fdTable.verifyFd(fd)) {
            return ResCode.EBADF;
        }

        OpenFile file = fdTable.getOpenFile(fd);
        if (file.isDirectory()) {
            return ResCode.EBADF;
        }
        try {
            switch (option) {
                case FROM_START:
                    file.lseek(pos);
                    break;
                case FROM_CURRENT:
                    file.lseek(file.getFilePointer() + pos);
                    break;
                case FROM_END:
                    file.lseek(file.getLength() + pos);
                    break;
                default:
                    return ResCode.EINVAL;
            }
        } catch (IOException e) {
            System.out.println(e);
            System.exit(-1);
        }

        long cur = 0;
        try {
            cur = file.getFilePointer();
        } catch (IOException e) {
            System.out.println(e);
            System.exit(-1);
        }

        return cur;
    }

}
