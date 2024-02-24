import java.io.File;
import java.util.HashMap;
import java.util.UUID;

public class ServerFileTable {
    private String rootdir;
    private Server server;
    private HashMap<String, ServerFile> fileTable;

    public ServerFileTable(Server server) {
        this.server = server;
        this.rootdir = server.getRootdir();
        fileTable = new HashMap<String, ServerFile>();
    }

    public synchronized ServerFile getFile(String relativePath, Boolean read, UUID newVerId,
            Boolean createIfNotExist) {
        ServerFile serverFile = fileTable.get(relativePath);
        if (serverFile == null) {
            serverFile = manageFile(relativePath);
        }
        if (serverFile == null) {
            if (createIfNotExist) {
                serverFile = new ServerFile(this, relativePath, UUID.randomUUID()); // create a temp file
                return serverFile;
            } else {
                return null;
            }
        } else {
            return serverFile;
        }
    }

    /**
     * Remove a file from the file table.
     * Cannot be used with getFile() and giveBackFile().
     * 
     * @param relativePath
     * @return
     */
    public synchronized int removeFile(String relativePath) {
        ServerFile fileRemoved = fileTable.remove(relativePath);
        int res = ResCode.SUCCESS;
        if (fileRemoved == null) {
            File file = new File(rootdir + relativePath);
            if (!file.exists()) {
                res = ResCode.NOT_EXIST;
            } else {
                file.delete();
            }
        } else {
            fileRemoved.remove();
        }
        return res;
    }

    public synchronized void updateFile(String relativePath, File tempFile, UUID verId) {
        ServerFile serverFile = fileTable.get(relativePath);
        if (serverFile != null) {
            serverFile.update(tempFile, verId);
            return;
        }

        serverFile = new ServerFile(this, relativePath, verId);
        fileTable.put(relativePath, serverFile);
        serverFile.update(tempFile, verId);
    }

    private ServerFile manageFile(String relativePath) {
        File file = new File(rootdir + relativePath);
        if (!file.exists()) {
            return null;
        }
        ServerFile serverFile = new ServerFile(this, relativePath, UUID.randomUUID());
        fileTable.put(relativePath, serverFile);
        return serverFile;
    }

    public String getRootdir() {
        return rootdir;
    }
}
