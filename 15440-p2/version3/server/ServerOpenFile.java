import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.UUID;

public class ServerOpenFile {
    private ServerFileTable fileTable;
    private String relativePath;
    private File tempFile;
    private RandomAccessFile randomAccessFile;
    private long size;
    private UUID verId;
    private Boolean read;

    public ServerOpenFile(ServerFile serverFile, File tempFile, UUID verId, Boolean read){
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

    public void write(byte[] data){
        try {
            randomAccessFile.write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close(){
        try {
            randomAccessFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(!read){
            Logger.log("File: " + relativePath + " is being updated");
            fileTable.updateFile(relativePath, tempFile, verId);
        }
        Boolean deleteRes = tempFile.delete();
        Logger.log("Temp file: " + tempFile.getAbsolutePath() + " is deleted: " + deleteRes);
    }
}
