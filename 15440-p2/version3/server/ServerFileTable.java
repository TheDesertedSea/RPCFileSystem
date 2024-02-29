
/**
 * ServerFileTable.java
 * 
 * @author Cundao Yu <cundaoy@andrew.cmu.edu>
 */
import java.io.File;
import java.util.HashMap;
import java.util.UUID;

/**
 * File table of the server
 */
public class ServerFileTable {
    /**
     * {@link String}
     * Root directory
     */
    private String rootdir;
    /**
     * {@link HashMap}
     * File table
     */
    private HashMap<String, ServerFile> fileTable;

    /**
     * Constructor
     * 
     * @param server {@link Server} Server
     */
    public ServerFileTable(Server server) {
        this.rootdir = server.getRootdir();
        fileTable = new HashMap<String, ServerFile>();
    }

    /**
     * Get a file from the file table
     * 
     * This method is synchronized, since it may also add a file that's not in the
     * table but actually exists in the real file system to the table.
     * 
     * @param relativePath     {@link String} Relative path
     * @param read             {@link Boolean} True if the file can be read
     * @param newVerId         {@link UUID} New version ID
     * @param createIfNotExist {@link Boolean} True if the file should be created if
     *                         not exist
     * @return {@link ServerFile} File
     */
    public synchronized ServerFile getFile(String relativePath, Boolean read, UUID newVerId,
            Boolean createIfNotExist) {
        ServerFile serverFile = fileTable.get(relativePath);
        if (serverFile == null) {
            serverFile = manageFile(relativePath); // Try to find the file in the real file system
        }
        if (serverFile == null) {
            if (createIfNotExist) {
                serverFile = new ServerFile(this, relativePath, UUID.randomUUID(), true, true);
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
     * 
     * This method is synchronized, since it may remove a file from the table
     * 
     * @param relativePath {@link String} Relative path
     * @return Result code
     */
    public synchronized int removeFile(String relativePath) {
        ServerFile fileRemoved = fileTable.remove(relativePath);
        int res = ResCode.SUCCESS;
        if (fileRemoved == null) {
            File file = new File(rootdir + relativePath);
            if (!file.exists()) {
                res = ResCode.ENOENT;
            } else {
                file.delete();
            }
        } else {
            fileRemoved.remove();
        }
        return res;
    }

    /**
     * Update a file in the file table using a temporary file
     * 
     * Create a new file if the file does not exist
     * 
     * @param relativePath {@link String} Relative path
     * @param tempFile     {@link File} Temporary file
     * @param verId        {@link UUID} Version ID
     */
    public synchronized void updateFile(String relativePath, File tempFile, UUID verId) {
        ServerFile serverFile = fileTable.get(relativePath);
        if (serverFile != null) {
            // ServerFile object already exists, update it
            serverFile.update(tempFile, verId);
            return;
        }

        // ServerFile object does not exist, create a new one
        serverFile = new ServerFile(this, relativePath, verId);
        fileTable.put(relativePath, serverFile);
        serverFile.update(tempFile, verId);
    }

    /**
     * Try to find the file in the real file system and add it to the file table
     * 
     * @param relativePath {@link String} Relative path
     * @return {@link ServerFile} Server file, or null if the file does not exist
     */
    private ServerFile manageFile(String relativePath) {
        File file = new File(rootdir + relativePath);
        if (!file.exists()) {
            return null;
        }
        ServerFile serverFile = new ServerFile(this, relativePath, UUID.randomUUID());
        fileTable.put(relativePath, serverFile);
        return serverFile;
    }

    /**
     * Get the root directory
     * 
     * @return {@link String} Root directory
     */
    public String getRootdir() {
        return rootdir;
    }
}
