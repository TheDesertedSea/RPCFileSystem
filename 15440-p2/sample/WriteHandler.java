import java.io.IOException;
import java.io.RandomAccessFile;

public class WriteHandler {
    
    private FDTable fdTable;

    public WriteHandler(FDTable fdTable) {
        this.fdTable = fdTable;
    }

    public long write(int fd, byte[] buf) {
        if(buf == null) {
            return FileHandling.Errors.EINVAL;
        }

        if(!fdTable.verifyFd(fd)) {
            return FileHandling.Errors.EBADF;
        }

        OpenFile file = fdTable.getOpenFile(fd);
        if(!file.canWrite()) {
            return FileHandling.Errors.EPERM;
        }

        try {
            file.getRandomAccessFile().write(buf);
            return buf.length;
        } catch (IOException e) {
            System.out.println(e);
            System.exit(-1);
        }
        return 0;
    }

}
