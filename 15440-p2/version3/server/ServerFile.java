
/**
 * ServerFile.java
 * 
 * @author Cundao Yu <cundaoy@andrew.cmu.edu>
 */
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.UUID;

/**
 * Abstraction of a file in the server
 */
public class ServerFile {
    /**
     * {@link String}
     * Root directory
     */
    private String rootdir;
    /**
     * {@link ServerFileTable}
     * File table of the server
     */
    private ServerFileTable fileTable;
    /**
     * {@link String}
     * Relative path
     */
    private String relativePath;
    /**
     * {@link UUID}
     * Version ID
     */
    private UUID verId;
    /**
     * {@link Boolean}
     * True if the file can be read
     */
    private Boolean canRead;
    /**
     * {@link Boolean}
     * True if the file can be written
     */
    private Boolean canWrite;

    /**
     * Constructor for an existing file
     * 
     * @param fileTable    {@link ServerFileTable} File table of the server
     * @param relativePath {@link String} Relative path
     * @param verId        {@link UUID} Version ID
     */
    public ServerFile(ServerFileTable fileTable, String relativePath, UUID verId) {
        this.rootdir = fileTable.getRootdir();
        this.fileTable = fileTable;
        this.relativePath = relativePath;
        this.verId = verId;
        File file = new File(rootdir + relativePath);
        this.canRead = file.canRead();
        this.canWrite = file.canWrite();
    }

    /**
     * Constructor for a new file
     * 
     * @param fileTable    {@link ServerFileTable} File table of the server
     * @param relativePath {@link String} Relative path
     * @param verId        {@link UUID} Version ID
     * @param canRead      {@link Boolean} True if the file can be read
     * @param canWrite     {@link Boolean} True if the file can be written
     */
    public ServerFile(ServerFileTable fileTable, String relativePath, UUID verId, Boolean canRead, Boolean canWrite) {
        this.rootdir = fileTable.getRootdir();
        this.fileTable = fileTable;
        this.relativePath = relativePath;
        this.verId = verId;
        this.canRead = canRead;
        this.canWrite = canWrite;
    }

    /**
     * Open the file
     * 
     * This creates a temporary copy for reading and writing by chunks.
     * By doing so, when transferring data by chunks, it won't block the file and
     * let other an access to the file.
     * 
     * @param read     {@link Boolean} True if the file is opened for only reading
     * @param newVerId {@link UUID} New version ID
     * @return {@link ServerTempFile} Opened file
     */
    public ServerTempFile open(Boolean read, UUID newVerId) {
        File originalFile = new File(rootdir + relativePath);
        File tempFile = new File(rootdir + relativePath + "." + UUID.randomUUID().toString()); // Temporary file
        try {
            tempFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (!originalFile.exists()) {
            return new ServerTempFile(this, tempFile, read ? verId : newVerId, read);
        }

        /* Copy the original file to the temporary file */
        try {
            RandomAccessFile originalFileRandomAccessFile = new RandomAccessFile(originalFile, "r");
            RandomAccessFile tempFileRandomAccessFile = new RandomAccessFile(tempFile, "rw");
            long remaining = originalFileRandomAccessFile.length();
            byte[] buffer = null;
            while (remaining > 0) {
                int readSize = (int) Math.min(remaining, Server.CHUNK_SIZE);
                buffer = new byte[readSize];
                originalFileRandomAccessFile.read(buffer); // Read by chunks
                tempFileRandomAccessFile.write(buffer); // Write by chunks
                remaining -= readSize;
            }
            originalFileRandomAccessFile.close();
            tempFileRandomAccessFile.close();

            if (read) {
                return new ServerTempFile(this, tempFile, verId, true);
            } else {
                return new ServerTempFile(this, tempFile, newVerId, false);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Update the file with the content of the temporary file
     * 
     * After writting by chunks, the temporary file will be used to update the file
     * 
     * @param tempFile {@link File} Temporary file
     * @param newVerId {@link UUID} New version ID
     */
    public void update(File tempFile, UUID newVerId) {
        File originalFile = new File(rootdir + relativePath);
        if (!originalFile.exists()) {
            try {
                originalFile.createNewFile();
                this.canRead = true;
                this.canWrite = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /* Copy the data of temporary file to the original file */
        try {
            RandomAccessFile originalFileRandomAccessFile = new RandomAccessFile(originalFile, "rw");
            RandomAccessFile tempFileRandomAccessFile = new RandomAccessFile(tempFile, "r");
            long remaining = tempFileRandomAccessFile.length();
            byte[] buffer = null;
            while (remaining > 0) {
                int readSize = (int) Math.min(remaining, Server.CHUNK_SIZE);
                buffer = new byte[readSize];
                tempFileRandomAccessFile.read(buffer); // Read by chunks
                originalFileRandomAccessFile.write(buffer); // Write by chunks
                remaining -= readSize;
            }
            originalFileRandomAccessFile.close();
            tempFileRandomAccessFile.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        verId = newVerId;
    }

    /**
     * Remove the file
     */
    public void remove() {
        File file = new File(rootdir + relativePath);
        file.delete();
    }

    public long getSize() {
        File file = new File(rootdir + relativePath);
        if (!file.exists()) {
            return 0;
        }
        return file.length();
    }

    public ServerFileTable getFileTable() {
        return fileTable;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public UUID getVerId() {
        return verId;
    }

    public boolean canRead() {
        return canRead;
    }

    public boolean canWrite() {
        return canWrite;
    }
}
