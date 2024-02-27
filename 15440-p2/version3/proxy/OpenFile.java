
/**
 * OpenFile.java
 * 
 * @author Cundao Yu <cundaoy@andrew.cmu.edu>
 */
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Open file class
 * 
 * Emulate the open file in Unix
 * RandomAccessFile is the core to operate file
 */
public class OpenFile {
    /**
     * True if the file is readable
     */
    private Boolean read;
    /**
     * True if the file is writable
     */
    private Boolean write;
    /**
     * True if the file is a directory
     */
    private Boolean isDirectory;
    /**
     * {@link CacheFileVersion}
     * File version
     */
    private CacheFileVersion version;
    /**
     * {@link RandomAccessFile}
     * Random access file
     */
    private RandomAccessFile raf;

    /**
     * Constructor
     * 
     * @param read    True if the file is readable
     * @param write   True if the file is writable
     * @param version {@link CacheFileVersion} Corresponding file version
     * @param raf     {@link RandomAccessFile} Random access file
     */
    public OpenFile(Boolean read, Boolean write, CacheFileVersion version, RandomAccessFile raf) {
        this.read = read;
        this.write = write;
        this.version = version;
        this.raf = raf;
        this.isDirectory = false;
    }

    /**
     * Constructor for directory
     * 
     * @param isDirectory True if the file is a directory
     */
    public OpenFile(Boolean isDirectory) {
        this.read = true;
        this.write = false;
        this.isDirectory = isDirectory;
        this.version = null;
        this.raf = null;
    }

    /**
     * Check if the file is readable
     * 
     * @return True if the file is readable
     */
    public Boolean canRead() {
        return read;
    }

    /**
     * Check if the file is writable
     * 
     * @return True if the file is writable
     */
    public Boolean canWrite() {
        return write;
    }

    /**
     * Check if the file is a directory
     * 
     * @return True if the file is a directory
     */
    public Boolean isDirectory() {
        return isDirectory;
    }

    /**
     * Read from the file
     * 
     * @param buf Buffer to store the read content
     * @return Number of bytes read
     * @throws IOException
     */
    public long read(byte[] buf) throws IOException {
        return raf.read(buf);
    }

    /**
     * Write to the file
     * 
     * @param buf Buffer to write
     * @throws IOException
     */
    public void write(byte[] buf) throws IOException {
        if (buf == null || buf.length == 0) {
            return;
        }
        version.setSize(raf.getFilePointer() + buf.length); // Update the size of the file
        raf.write(buf);
    }

    /**
     * Seek to a position in the file
     * 
     * @param pos Position to seek
     * @throws IOException
     */
    public void lseek(long pos) throws IOException {
        raf.seek(pos);
    }

    /**
     * Close the file
     */
    public void close() {
        if (isDirectory) {
            return;
        }

        Logger.log("Close file: " + version.getRelativePath());

        try {
            raf.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        version.updateLRU();
        version.release();
    }

    /**
     * Get the current position in the file
     * 
     * @return Current position in the file
     * @throws IOException
     */
    public long getFilePointer() throws IOException {
        return raf.getFilePointer();
    }

    /**
     * Get the length of the file
     * 
     * @return Length of the file
     * @throws IOException
     */
    public long getLength() throws IOException {
        return version.getSize();
    }
}
