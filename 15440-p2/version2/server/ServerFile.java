import java.io.File;
import java.io.FileInputStream;
import java.io.RandomAccessFile;
import java.util.UUID;

public class ServerFile {
    private ServerFileTable fileTable;
    private String relativePath;
    private UUID verId;
    private Boolean canRead;
    private Boolean canWrite;
    
    public ServerFile(ServerFileTable fileTable, String relativePath, UUID verId){
        this.fileTable = fileTable;
        this.relativePath = relativePath;
        this.verId = verId;
        File file = new File(relativePath);
        this.canRead = file.canRead();
        this.canWrite = file.canWrite();
    }

    public UUID getVerId(){
        return verId;
    }

    public boolean canRead(){
        return canRead;
    }

    public boolean canWrite(){
        return canWrite;
    }

    public byte[] getData(long offset, int length){
        File file = new File(relativePath);
        byte[] data = new byte[length];
        int readCount = 0;
        try {
            RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");
            randomAccessFile.seek(offset);
            readCount = randomAccessFile.read(data);
            randomAccessFile.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        byte[] result = new byte[readCount];
        for(int i = 0; i < readCount; i++){
            result[i] = data[i];
        }
        return result;
    }

    public void putData(byte[] data, UUID verId, long offset){
        File file = new File(relativePath);
        try {
            RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
            randomAccessFile.seek(offset);
            randomAccessFile.write(data);
            randomAccessFile.close();
            setVerId(verId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void remove(){
        File file = new File(relativePath);
        file.delete();
    }

    private void setVerId(UUID verId){
        this.verId = verId;
    }
}
