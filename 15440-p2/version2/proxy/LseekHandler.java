
/**
 * LseekHandler.java
 * 
 * @author Cundao Yu <cundaoy@andrew.cmu.edu>
 */

import java.io.IOException;

public class LseekHandler {

    private FDTable fdTable;

    public LseekHandler(FDTable fdTable) {
        this.fdTable = fdTable;
    }

    public long lseek(int fd, long pos, FileHandling.LseekOption o) {
        if (!fdTable.verifyFd(fd)) {
            return ResCode.EBADF;
        }

        OpenFile file = fdTable.getOpenFile(fd);
        if (file.isDirectory()) {
            return ResCode.EBADF;
        }
        try {
            switch (o) {
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
