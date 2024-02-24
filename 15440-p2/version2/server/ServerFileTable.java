import java.io.File;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ServerFileTable {
    private Server server;
    private HashMap<String, ServerFile> fileTable;
    private ReentrantReadWriteLock tableLock;

    public ServerFileTable(Server server) {
        this.server = server;
        fileTable = new HashMap<String, ServerFile>();
        tableLock = new ReentrantReadWriteLock();
    }

    /**
     * Get a file from the file table.
     * This will lock the file table.
     * Must call giveBackFile() after using the file.
     * 
     * @param relativePath
     * @return
     */
    public ServerFile getFile(String relativePath, Boolean createIfNotExist, Boolean readOnly) {
        if(readOnly){
            tableLock.readLock().lock();
        } else {
            tableLock.writeLock().lock();
        }
        ServerFile serverFile = fileTable.get(relativePath);
        if (serverFile == null) {
            serverFile = manageFile(relativePath, createIfNotExist);
        }
        return serverFile;
    }

    /**
     * Give back the file to the file table.
     * This will unlock the file table.
     * 
     * @param serverFile
     */
    public void giveBackFile(ServerFile serverFile, Boolean readOnly) {
        if(readOnly){
            tableLock.readLock().unlock();
        } else {
            tableLock.writeLock().unlock();
        }
    }

    /**
     * Remove a file from the file table.
     * Cannot be used with getFile() and giveBackFile().
     * 
     * @param relativePath
     * @return
     */
    public int removeFile(String relativePath) {
        tableLock.writeLock().lock();
        ServerFile fileRemoved = fileTable.remove(relativePath);
        int res = ResCode.SUCCESS;
        if (fileRemoved == null) {
            File file = new File(relativePath);
            if (!file.exists()) {
                res = ResCode.NOT_EXIST;
            } else {
                file.delete();
            }
        } else {
            fileRemoved.remove();
        }
        tableLock.writeLock().unlock();
        return res;
    }

    private ServerFile manageFile(String relativePath, Boolean createIfNotExist) {
        File file = new File(relativePath);
        if (!file.exists()) {
            if (!createIfNotExist) {
                return null;
            }
            try {
                file.createNewFile();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        ServerFile serverFile = new ServerFile(this, relativePath, UUID.randomUUID());
        fileTable.put(relativePath, serverFile);
        return serverFile;
    }
}
