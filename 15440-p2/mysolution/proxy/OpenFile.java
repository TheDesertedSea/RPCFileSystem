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
    private Boolean hasModified;
    private RandomAccessFile file;
    private String localPath;
    private String serverPath;

    public OpenFile(RandomAccessFile file, Boolean canRead, Boolean canWrite, Boolean isDirectory, String localPath, String serverPath) {
        this.canRead = canRead;
        this.canWrite = canWrite;
        this.isDirectory = isDirectory;
        this.file = file;
        this.hasModified = false;
        this.localPath = localPath;
        this.serverPath = serverPath;
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
        hasModified = true;
    }

    public void lseek(long pos) throws IOException {
        file.seek(pos);
    }

    public void close() throws IOException {
        if(file != null) {
            file.close();
        }
    }

    public Boolean hasModified() {
        return hasModified;
    }

    public String getLocalPath() {
        return localPath;
    }

    public String getServerPath() {
        return serverPath;
    }
}
