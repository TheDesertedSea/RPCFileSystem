/**
 * OpenFile.java
 * 
 * @author Cundao Yu <cundaoy@andrew.cmu.edu>
 */

import java.io.IOException;
import java.io.RandomAccessFile;

public class OpenFile {
    private Boolean canRead;
    private Boolean canWrite;
    private Boolean isDirectory;
    private RandomAccessFile file;

    public OpenFile(RandomAccessFile file, Boolean canRead, Boolean canWrite, Boolean isDirectory) {
        this.canRead = canRead;
        this.canWrite = canWrite;
        this.isDirectory = isDirectory;
        this.file = file;
    }

    public Boolean canRead() {
        return canRead;
    }

    public Boolean canWrite() {
        return canWrite;
    }

    public Boolean isDirectory() {
        return isDirectory;
    }

    public long getFilePointer() throws IOException {
        return file.getFilePointer();
    }

    public long getLength() throws IOException {
        return file.length();
    }

    public long read(byte[] buf) throws IOException {
        return file.read(buf);
    }

    public void write(byte[] buf) throws IOException {
        file.write(buf);
    }

    public void lseek(long pos) throws IOException {
        file.seek(pos);
    }

    public void close() throws IOException {
        if(file != null) {
            file.close();
        }
    }
}
