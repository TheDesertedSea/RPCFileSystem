import java.io.Serializable;
import java.util.UUID;

public class FileGetResult implements Serializable{
    private int resCode;
    private String relativePath;
    private UUID verId;
    private Boolean canRead;
    private Boolean canWrite;
    private byte[] data;

    public FileGetResult(int resCode, String relativePath, UUID verId, Boolean canRead, Boolean canWrite, byte[] data) {
        this.resCode = resCode;
        this.relativePath = relativePath;
        this.verId = verId;
        this.canRead = canRead;
        this.canWrite = canWrite;
        this.data = data;
    }

    public int getResCode() {
        return resCode;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public UUID getVerId() {
        return verId;
    }

    public Boolean getCanRead() {
        return canRead;
    }

    public Boolean getCanWrite() {
        return canWrite;
    }

    public byte[] getData() {
        return data;
    }

    public String toString(){
        return "----------FileGetResult---------\n" + 
                "resCode: " + resCode + "\n" +
                "path: " + (relativePath == null ? "" : relativePath) + "\n" +
                "version: " + (verId == null ? "" : verId.toString()) + "\n" +
                "canRead: " + canRead + "\n" +
                "canWrite: " + canWrite + "\n" +
                "data length: " + (data == null ? "0" : data.length) + "\n" +
                "-------------------------------\n";
    }
}
