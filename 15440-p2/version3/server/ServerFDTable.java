import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ServerFDTable {
    private static final int MAX_SIZE = 10240;

    private List<ServerOpenFile> openFiles;
    private ReentrantReadWriteLock lock;

    public ServerFDTable() {
        openFiles = new ArrayList<ServerOpenFile>();
        lock = new ReentrantReadWriteLock();
    }

    public Boolean verifyFd(int fd) {
        lock.readLock().lock();
        if(fd < 0 || fd >= openFiles.size()) {
            return false;
        }

        if(openFiles.get(fd) == null) {
            return false;
        }

        lock.readLock().unlock();
        return true;
    }

    private int getFreeFd() {
        for(int i = 0; i < openFiles.size(); i++) {
            if(openFiles.get(i) == null) {
                return i;
            }
        }

        if(openFiles.size() < MAX_SIZE) {
            openFiles.add(null);
            return openFiles.size() - 1;
        }

        return -1;
    }

    public int addOpenFile(ServerOpenFile file) {
        lock.writeLock().lock();
        int fd = getFreeFd();
        openFiles.set(fd, file);
        lock.writeLock().unlock();
        return fd;
    }

    public ServerOpenFile getOpenFile(int fd) {
        lock.readLock().lock();
        ServerOpenFile file = openFiles.get(fd);
        lock.readLock().unlock();
        return file;
    }

    public synchronized void removeOpenFile(int fd) {
        lock.writeLock().lock();
        openFiles.set(fd, null);
        lock.writeLock().unlock();
    }
}
