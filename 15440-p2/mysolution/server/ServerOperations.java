
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.UUID;

public interface ServerOperations extends Remote {
    FileGetResult getFile(String requestPath, UUID proxyVersion) throws RemoteException;

    void putFile(String path, byte[] data) throws RemoteException;

    void removeFile(String path) throws RemoteException;
}
