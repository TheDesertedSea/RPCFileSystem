import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.UUID;

public class ServerFile {
    private String rootdir;
    private ServerFileTable fileTable;
    private String relativePath;
    private UUID verId;
    private Boolean canRead;
    private Boolean canWrite;

    public ServerFile(ServerFileTable fileTable, String relativePath, UUID verId) {
        this.rootdir = fileTable.getRootdir();
        this.fileTable = fileTable;
        this.relativePath = relativePath;
        this.verId = verId;
        File file = new File(rootdir + relativePath);
        Logger.log("File: " + file.getAbsolutePath() + " is created with verId: " + verId);
        this.canRead = file.canRead();
        this.canWrite = file.canWrite();
    }

    public ServerFile(ServerFileTable fileTable, String relativePath, UUID verId, Boolean canRead, Boolean canWrite) {
        this.rootdir = fileTable.getRootdir();
        this.fileTable = fileTable;
        this.relativePath = relativePath;
        this.verId = verId;
        this.canRead = canRead;
        this.canWrite = canWrite;
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

    public void remove() {
        File file = new File(rootdir + relativePath);
        file.delete();
    }

    public ServerOpenFile open(Boolean read, UUID newVerId) {
        Logger.log("Open file: " + relativePath + " read: " + read + " newVerId: " + newVerId);
        File originalFile = new File(rootdir + relativePath);
        File tempFile = new File(rootdir + relativePath + "." + UUID.randomUUID().toString());
        try {
            tempFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (!originalFile.exists()) {
            Logger.log("File: " + originalFile.getAbsolutePath() + " does not exist");
            return new ServerOpenFile(this, tempFile, read ? verId : newVerId, read);
        }
        try {
            RandomAccessFile originalFileRandomAccessFile = new RandomAccessFile(originalFile, "r");
            RandomAccessFile tempFileRandomAccessFile = new RandomAccessFile(tempFile, "rw");
            long remaining = originalFileRandomAccessFile.length();
            byte[] buffer = null;
            while (remaining > 0) {
                int readSize = (int) Math.min(remaining, Server.CHUNK_SIZE);
                buffer = new byte[readSize];
                originalFileRandomAccessFile.read(buffer);
                tempFileRandomAccessFile.write(buffer);
                remaining -= readSize;
            }
            originalFileRandomAccessFile.close();
            tempFileRandomAccessFile.close();

            if (read) {
                return new ServerOpenFile(this, tempFile, verId, true);
            } else {
                return new ServerOpenFile(this, tempFile, newVerId, false);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public ServerFileTable getFileTable() {
        return fileTable;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public void update(File tempFile, UUID newVerId) {
        File originalFile = new File(rootdir + relativePath);
        if (!originalFile.exists()) {
            Logger.log("File: " + originalFile.getAbsolutePath() + " does not exist");
            try {
                originalFile.createNewFile();
                this.canRead = true;
                this.canWrite = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Logger.log("File: " + originalFile.getAbsolutePath() + " is being updated with temp file: "
                + tempFile.getAbsolutePath());
        try {
            RandomAccessFile originalFileRandomAccessFile = new RandomAccessFile(originalFile, "rw");
            RandomAccessFile tempFileRandomAccessFile = new RandomAccessFile(tempFile, "r");
            long remaining = tempFileRandomAccessFile.length();
            byte[] buffer = null;
            while (remaining > 0) {
                int readSize = (int) Math.min(remaining, Server.CHUNK_SIZE);
                buffer = new byte[readSize];
                tempFileRandomAccessFile.read(buffer);
                originalFileRandomAccessFile.write(buffer);
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
        Logger.log("File:" + relativePath + " canRead: " + canRead + " canWrite: " + canWrite + " updated to verId: "
                + verId);
    }

    public long getSize() {
        File file = new File(rootdir + relativePath);
        if (!file.exists()) {
            Logger.log("Try to get size of file: " + file.getAbsolutePath() + " does not exist");
            return 0;
        }
        Logger.log("Get size of file: " + file.getAbsolutePath() + " is " + file.length());
        return file.length();
    }
}
