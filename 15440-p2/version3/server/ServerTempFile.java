/**
 * ServerTempFile.java
 * 
 * @author Cundao Yu <cundaoy@andrew.cmu.edu>
 */
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.UUID;

/**
 * Temp file class in the server
 * 
 * This is used to write/read data by chunks to/from a temporary file
 */
public class ServerTempFile {
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
     * {@link File}
     * Temporary file
     */
    private File tempFile;
    /**
     * {@link RandomAccessFile}
     * Random access file
     */
    private RandomAccessFile randomAccessFile;
    /**
     * Size of the file
     */
    private long size;
    /**
     * {@link UUID}
     * Version ID
     */
    private UUID verId;
    /**
     * {@link Boolean}
     * True if the file can be read
     */
    private Boolean read;

    /**
     * Constructor
     * 
     * @param serverFile {@link ServerFile} Server file
     * @param tempFile   {@link File} Temporary file that this open file is associated
     * @param verId      {@link UUID} Version ID
     * @param read       {@link Boolean} True if the file can be read
     */
    public ServerTempFile(ServerFile serverFile, File tempFile, UUID verId, Boolean read){
        this.fileTable = serverFile.getFileTable();
        this.relativePath = serverFile.getRelativePath();
        this.tempFile = tempFile;
        try {
            this.randomAccessFile = new RandomAccessFile(tempFile, "rw");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        this.size = tempFile.length();
        this.verId = verId;
        this.read = read;
    }

    /**
     * Read data from the file
     * 
     * @return {@link byte[]} Data
     */
    public byte[] read(){
        long remaining = 0;
        try {
            remaining = size - randomAccessFile.getFilePointer();
        } catch (IOException e) {
            e.printStackTrace();
        }
        int readSize = (int) Math.min(remaining, Server.CHUNK_SIZE);
        byte[] buffer = new byte[readSize];
        try {
            randomAccessFile.read(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return buffer;
    }

    /**
     * Write data to the file
     * 
     * @param data {@link byte[]} Data
     */
    public void write(byte[] data){
        try {
            randomAccessFile.write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Close the file
     */
    public void close(){
        try {
            randomAccessFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(!read){
            fileTable.updateFile(relativePath, tempFile, verId);
            
        }
        tempFile.delete();
    }
}
