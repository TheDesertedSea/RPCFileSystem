
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.UUID;

public interface ServerOperations extends Remote {
    
    FileCheckResult checkFile(String reqPathStr, UUID proxyVerId) throws RemoteException;

    void closeFile(int serverFd) throws RemoteException;

    byte[] readFile(int serverFd) throws RemoteException;

    int putFile(String relativePath, UUID verId) throws RemoteException;

    void writeFile(int serverFd, byte[] data) throws RemoteException;

    FileRemoveResult removeFile(String reqPathStr) throws RemoteException;
}
