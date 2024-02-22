import java.io.Serializable;
import java.util.UUID;

public class FileGetResult implements Serializable{
    private int resCode;
    private String path;
    private UUID version;
    private Boolean canRead;
    private Boolean canWrite;
    private byte[] data;

    public FileGetResult(int resCode, String path, UUID version, Boolean canRead, Boolean canWrite, byte[] data) {
        this.resCode = resCode;
        this.path = path;
        this.version = version;
        this.canRead = canRead;
        this.canWrite = canWrite;
        this.data = data;
    }

    public int getResCode() {
        return resCode;
    }

    public String getPath() {
        return path;
    }

    public UUID getVersion() {
        return version;
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

    public void setResCode(int resCode) {
        this.resCode = resCode;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setVersion(UUID version) {
        this.version = version;
    }

    public void setCanRead(Boolean canRead) {
        this.canRead = canRead;
    }

    public void setCanWrite(Boolean canWrite) {
        this.canWrite = canWrite;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}
