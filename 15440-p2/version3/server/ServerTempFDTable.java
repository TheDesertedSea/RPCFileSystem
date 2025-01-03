
/**
 * ServerTempFDTable.java
 * 
 * @author Cundao Yu <cundaoy@andrew.cmu.edu>
 */
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * File descriptor table for temp files on server
 */
public class ServerTempFDTable {
    /**
     * Max size of the table
     */
    private static final int MAX_SIZE = 10240;

    /**
     * {@link List}<{@link ServerTempFile}>
     * Open files table, index is the file descriptor
     */
    private List<ServerTempFile> openFiles;
    /**
     * {@link ReentrantReadWriteLock}
     * Lock for the table
     */
    private ReentrantReadWriteLock lock;

    /**
     * Constructor
     */
    public ServerTempFDTable() {
        openFiles = new ArrayList<ServerTempFile>();
        lock = new ReentrantReadWriteLock();
    }

    /**
     * Verify if the file descriptor is valid
     * 
     * @param fd File descriptor
     * @return {@link Boolean} True if the file descriptor is valid
     */
    public Boolean verifyFd(int fd) {
        lock.readLock().lock();
        if (fd < 0 || fd >= openFiles.size()) {
            lock.readLock().unlock();
            return false;
        }

        if (openFiles.get(fd) == null) {
            lock.readLock().unlock();
            return false;
        }

        lock.readLock().unlock();
        return true;
    }

    /**
     * Get a free file descriptor
     * 
     * @return File descriptor
     */
    private int getFreeFd() {
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
     * @param file {@link ServerTempFile} Open file
     * @return File descriptor
     */
    public int addOpenFile(ServerTempFile file) {
        lock.writeLock().lock();
        int fd = getFreeFd();
        openFiles.set(fd, file);
        lock.writeLock().unlock();
        return fd;
    }

    /**
     * Get the open file from the table
     * 
     * @param fd File descriptor
     * @return {@link ServerTempFile} Open file
     */
    public ServerTempFile getOpenFile(int fd) {
        lock.readLock().lock();
        ServerTempFile file = openFiles.get(fd);
        lock.readLock().unlock();
        return file;
    }

    /**
     * Remove an open file from the table
     * 
     * @param fd File descriptor
     */
    public synchronized void removeOpenFile(int fd) {
        lock.writeLock().lock();
        openFiles.set(fd, null);
        lock.writeLock().unlock();
    }
}
