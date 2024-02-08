import java.io.RandomAccessFile;

public class OpenFile {
    private Boolean canRead;
    private Boolean canWrite;
    private RandomAccessFile file;

    public OpenFile(RandomAccessFile file, Boolean canRead, Boolean canWrite) {
        this.canRead = canRead;
        this.canWrite = canWrite;
        this.file = file;
    }

    public Boolean canRead() {
        return canRead;
    }

    public Boolean canWrite() {
        return canWrite;
    }

    public RandomAccessFile getRandomAccessFile() {
        return file;
    }
}
