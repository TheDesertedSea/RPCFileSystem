import java.io.IOException;
import java.io.RandomAccessFile;

public class OpenFile {
    private Boolean read;
    private Boolean write;
    private Boolean isDirectory;
    private CacheFileVersion version;
    private RandomAccessFile raf;

    public OpenFile(Boolean read, Boolean write, CacheFileVersion version, RandomAccessFile raf) {
        this.read = read;
        this.write = write;
        this.version = version;
        this.raf = raf;
    }

    public OpenFile(Boolean isDirectory) {
        this.read = true;
        this.write = false;
        this.isDirectory = isDirectory;
        this.version = null;
        this.raf = null;
    }

    public Boolean canRead() {
        return read;
    }

    public Boolean canWrite() {
        return write;
    }

    public Boolean isDirectory() {
        return isDirectory;
    }

    public long read(byte[] buf) throws IOException {
        return raf.read(buf);
    }

    public void write(byte[] buf) throws IOException {
        if(buf == null || buf.length == 0){
            return;
        }
        version.setSize(raf.getFilePointer() + buf.length);
        raf.write(buf);
    }

    public void lseek(long pos) throws IOException {
        raf.seek(pos);
    }

    public long getFilePointer() throws IOException {
        return raf.getFilePointer();
    }

    public long getLength() throws IOException {
        return version.getSize();
    }

    public void close() {
        if (isDirectory) {
            return;
        }
        try {
            raf.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        version.release();
    }
}
