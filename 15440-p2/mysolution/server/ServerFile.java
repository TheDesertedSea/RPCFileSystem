import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ServerFile {
    private UUID version;
    private ReentrantReadWriteLock readWriteLock;
    private ReentrantReadWriteLock removeLock;

    public ServerFile(){
        version = UUID.randomUUID();
        readWriteLock = new ReentrantReadWriteLock();
        removeLock = new ReentrantReadWriteLock();
    }

    public UUID startGet(){
        readWriteLock.readLock().lock();
        return version;
    }

    public void endGet(){
        readWriteLock.readLock().unlock();
    }

    public void startPut(){
        readWriteLock.writeLock().lock();
        version = UUID.randomUUID();
    }

    public void endPut(){
        readWriteLock.writeLock().unlock();
    }

    public void startRemove(){
        removeLock.writeLock().lock();
    }

    public void endRemove(){
        removeLock.writeLock().unlock();
    }

    public void freezeRemove(){
        removeLock.readLock().lock();
    }

    public void unfreezeRemove(){
        removeLock.readLock().unlock();
    }
}
