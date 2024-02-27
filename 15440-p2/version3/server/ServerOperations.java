
/**
 * ServerOperations.java
 * 
 * @author Cundao Yu <cundaoy@andrew.cmu.edu>
 */
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.UUID;

/**
 * Server operations interface
 */
public interface ServerOperations extends Remote {

    /**
     * Check file on the server
     * 
     * @param reqPathStr {@link String} Requested path
     * @param proxyVerId {@link UUID} Version ID of the proxy
     * @return {@link FileCheckResult} Result of the file check
     * @throws RemoteException
     */
    FileCheckResult checkFile(String reqPathStr, UUID proxyVerId) throws RemoteException;

    /**
     * Close a temporary file on the server
     * 
     * @param serverFd File descriptor of the file in the server
     * @throws RemoteException
     */
    void closeFile(int serverFd) throws RemoteException;

    /**
     * Read from a temporary file on the server
     * 
     * @param serverFd File descriptor of the file in the server
     * @return Data read from the file
     * @throws RemoteException
     */
    byte[] readFile(int serverFd) throws RemoteException;

    /**
     * Request to put a file on the server.
     * 
     * Server will create a temporary file for the file to be put.
     * 
     * @param relativePath {@link String} Relative path
     * @param verId        {@link UUID} Version ID
     * @return File descriptor of the file on the server for later data transfer
     * @throws RemoteException
     */
    int putFile(String relativePath, UUID verId) throws RemoteException;

    /**
     * Write to a temporary file on the server
     * 
     * @param serverFd File descriptor of the file in the server
     * @param data     Data to write
     * @throws RemoteException
     */
    void writeFile(int serverFd, byte[] data) throws RemoteException;

    /**
     * Remove a file on the server
     * 
     * @param reqPathStr {@link String} Requested path
     * @return {@link FileRemoveResult} Result of the file remove
     * @throws RemoteException
     */
    FileRemoveResult removeFile(String reqPathStr) throws RemoteException;
}
