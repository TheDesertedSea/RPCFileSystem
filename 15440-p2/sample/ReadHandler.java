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
        try {
            return file.getRandomAccessFile().read(buf) == -1 ? 0 : buf.length;
        } catch (IOException e) {
            System.out.println(e);
            System.exit(-1);
        }
        return 0;
    }

}
