
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Operations extends Remote {
    CheckResult check(String path, CheckOption option) throws RemoteException;
    byte[] getFile(String path) throws RemoteException;
    int putFile(String path, byte[] data) throws RemoteException;
}
