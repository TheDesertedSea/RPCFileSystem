import java.io.IOException;

public class LseekHandler {
    
    private FDTable fdTable;

    public LseekHandler(FDTable fdTable) {
        this.fdTable = fdTable;
    }

    public long lseek(int fd, long pos, FileHandling.LseekOption o) {
        if(!fdTable.verifyFd(fd)) {
            return FileHandling.Errors.EBADF;
        }

        OpenFile file = fdTable.getOpenFile(fd);
        try {
            switch(o) {
                case FROM_START:
                    file.getRandomAccessFile().seek(pos);
                    break;
                case FROM_CURRENT:
                    file.getRandomAccessFile().seek(file.getRandomAccessFile().getFilePointer() + pos);
                    break;
                case FROM_END:
                    file.getRandomAccessFile().seek(file.getRandomAccessFile().length() + pos);
                    break;
                default:
                    return FileHandling.Errors.EINVAL;
            }
        } catch (IOException e) {
            System.out.println(e);
            System.exit(-1);
        }
        

        long cur = 0;
        try {
            cur = file.getRandomAccessFile().getFilePointer();
        } catch (IOException e) {
            System.out.println(e);
            System.exit(-1);
        }

        return cur;
    }

}
