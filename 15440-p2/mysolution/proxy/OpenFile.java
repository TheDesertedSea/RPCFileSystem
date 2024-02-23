/**
 * OpenFile.java
 * 
 * @author Cundao Yu <cundaoy@andrew.cmu.edu>
 */

import java.io.IOException;
import java.io.RandomAccessFile;

public class OpenFile {
    private CacheFileVersion cacheFileVersion;
    private RandomAccessFile file;
    private Boolean isDirectory;

    public OpenFile(RandomAccessFile file, CacheFileVersion cacheFileVersion, Boolean isDirectory) {
        this.file = file;
        this.cacheFileVersion = cacheFileVersion;
        this.isDirectory = isDirectory;
    }

    public Boolean canRead() {
        return cacheFileVersion.canRead();
    }

    public Boolean canWrite() {
        return cacheFileVersion.canWrite();
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
        long size = buf.length;
        cacheFileVersion.needWrite(size);
        file.write(buf);
        cacheFileVersion.setModified(true);
    }

    public void lseek(long pos) throws IOException {
        file.seek(pos);
    }

    public void close() throws IOException {
        if(file != null) {
            file.close();
            cacheFileVersion.release();
        }
    }

    public Boolean isModified() {
        return cacheFileVersion.isModified();
    }
}
