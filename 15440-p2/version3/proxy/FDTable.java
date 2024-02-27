
/**
 * FDTable.java
 * 
 * @author Cundao Yu <cundaoy@andrew.cmu.edu>
 */
import java.util.ArrayList;
import java.util.List;

/**
 * File descriptor table
 */
public class FDTable {
    /**
     * Max size of the table
     */
    private static final int MAX_SIZE = 10240;

    /**
     * {@link List}<{@link OpenFile}>
     * Open files table, index is the file descriptor
     */
    private List<OpenFile> openFiles;

    /**
     * Constructor
     */
    public FDTable() {
        openFiles = new ArrayList<OpenFile>();
    }

    /**
     * Verify if the file descriptor is valid
     * 
     * @param fd File descriptor
     * @return {@link Boolean} True if the file descriptor is valid
     */
    public Boolean verifyFd(int fd) {
        if (fd < 0 || fd >= openFiles.size()) {
            return false;
        }

        if (openFiles.get(fd) == null) {
            return false;
        }

        return true;
    }

    /**
     * Get a free file descriptor
     * 
     * @return File descriptor
     */
    public int getFreeFd() {
        for (int i = 0; i < openFiles.size(); i++) {
            if (openFiles.get(i) == null) {
                return i;
            }
        }

        if (openFiles.size() < MAX_SIZE) {
            openFiles.add(null);
            return openFiles.size() - 1;
        }

        return -1;
    }

    /**
     * Add an open file to the table
     * 
     * @param fd   File descriptor
     * @param file {@link OpenFile} Open file
     */
    public void addOpenFile(int fd, OpenFile file) {
        openFiles.set(fd, file);
    }

    /**
     * Get the open file from the table
     * 
     * @param fd File descriptor
     * @return {@link OpenFile} Open file
     */
    public OpenFile getOpenFile(int fd) {
        return openFiles.get(fd);
    }

    /**
     * remove the file from the table
     * 
     * This just removes the open file from the table, does not close the file
     * 
     * @param fd File descriptor
     */
    public void removeOpenFile(int fd) {
        openFiles.set(fd, null);
    }
}
