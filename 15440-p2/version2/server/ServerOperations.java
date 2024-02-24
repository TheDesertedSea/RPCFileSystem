
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.UUID;

public interface ServerOperations extends Remote {
    FileGetResult getFile(String reqPathStr, UUID proxyVerId, long offset) throws RemoteException;

    void putFile(String relativePath, byte[] data, UUID verId, long offset) throws RemoteException;

    FileRemoveResult removeFile(String reqPathStr) throws RemoteException;
}
