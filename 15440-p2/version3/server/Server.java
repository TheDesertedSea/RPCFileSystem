
/**
 * Server.java
 * 
 * @author Cundao Yu <cundaoy@andrew.cmu.edu>
 */
import java.io.File;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.UUID;

/**
 * Server class
 * 
 * Main class of server
 */
public class Server extends UnicastRemoteObject implements ServerOperations {

    /**
     * Chunk size of any long sequence of data
     */
    public static final int CHUNK_SIZE = 1024 * 128;

    /**
     * Root directory of the server
     */
    private String rootdir;
    /**
     * {@link ServerFileTable}
     * File table of the server
     */
    private ServerFileTable fileTable;
    /**
     * {@link ServerTempFDTable}
     * File descriptor table of the server
     */
    private ServerTempFDTable fdTable;

    /**
     * Constructor
     * 
     * @param rootdir {@link String} Root directory of the server
     * @throws RemoteException
     */
    protected Server(String rootdir) throws RemoteException {
        super();
        this.rootdir = rootdir;
        this.fileTable = new ServerFileTable(this);
        this.fdTable = new ServerTempFDTable();
    }

    /**
     * Get the root directory of the server
     * 
     * @return {@link String} Root directory of the server
     */
    public String getRootdir() {
        return rootdir;
    }

    /**
     * Get the file table of the server
     * 
     * @return {@link ServerFileTable} File table of the server
     */
    public ServerFileTable getFileTable() {
        return fileTable;
    }

    /**
     * Check the file on the server
     * 
     * If there's new version. The server will created a temporary file and a
     * corresponding file descriptor for the client
     * to read the file data by chunks.
     * 
     * @param reqPathStr {@link String} Requested path
     * @param proxyVerId {@link UUID} Version ID of the file on the proxy
     * @return {@link FileCheckResult} Result of the file check
     * @throws RemoteException
     */
    @Override
    public FileCheckResult checkFile(String reqPathStr, UUID proxyVerId) throws RemoteException {
        /* Check if the path is valid, except the last component */
        String absolutePathStr = PathTools.getAbsolutePath(reqPathStr, rootdir);
        int pathCheckRes = PathTools.checkPath(absolutePathStr, rootdir);
        if (pathCheckRes < 0) {
            return new FileCheckResult(pathCheckRes, null, null, false, false, -1, -1, null);
        }
        File file = new File(absolutePathStr);
        if (file.isDirectory()) {
            // If the path is a directory
            return new FileCheckResult(ResCode.EISDIR, null, null, false, false, -1, -1, null);
        }

        /* Get the file from the file table */
        String relativePath = PathTools.getRelativePath(absolutePathStr, rootdir);
        ServerFile serverFile = fileTable.getFile(relativePath, true, null, false);
        if (serverFile == null) {
            // If the file does not exist
            return new FileCheckResult(ResCode.NOT_EXIST, relativePath, null, false, false, -1, -1, null);
        }

        if (proxyVerId != null && proxyVerId.equals(serverFile.getVerId())) {
            // If the file is up-to-date
            return new FileCheckResult(ResCode.NO_UPDATE, relativePath, proxyVerId, serverFile.canRead(),
                    serverFile.canWrite(), -1, -1, null);
        }

        // Return result of new version
        ServerTempFile openFile = serverFile.open(true, null);
        int serverFd = fdTable.addOpenFile(openFile);
        FileCheckResult fileCheckResult = new FileCheckResult(ResCode.NEW_VERSION, relativePath, serverFile.getVerId(),
                serverFile.canRead(),
                serverFile.canWrite(), serverFd, serverFile.getSize(), openFile.read());
        return fileCheckResult;
    }

    /**
     * Close the opened temporary file on the server
     * 
     * @param serverFd File descriptor of the file on the server
     * @throws RemoteException
     */
    @Override
    public void closeFile(int serverFd) throws RemoteException {
        if (!fdTable.verifyFd(serverFd)) {
            throw new RemoteException("Invalid file descriptor");
        }
        ServerTempFile openFile = fdTable.getOpenFile(serverFd);
        openFile.close();
        fdTable.removeOpenFile(serverFd);
    }

    /**
     * Read the file on the server
     * 
     * Used to read by chunks
     * 
     * @param serverFd File descriptor of the file on the server
     * @return {@link byte[]} Content of the file
     * @throws RemoteException
     */
    @Override
    public byte[] readFile(int serverFd) throws RemoteException {
        if (!fdTable.verifyFd(serverFd)) {
            throw new RemoteException("Invalid file descriptor");
        }
        ServerTempFile openFile = fdTable.getOpenFile(serverFd);
        return openFile.read();
    }

    /**
     * Request to put a file on the server
     * If allowed, the server will create a temporary file and a corresponding file
     * descriptor for the client
     * to write the file data by chunks.
     * 
     * @param relativePath {@link String} Relative path of the file
     * @param verId        {@link UUID} Version ID of the file
     * @return File descriptor of the temp file on the server for later data transfer
     * @throws RemoteException
     */
    public int putFile(String relativePath, UUID verId) throws RemoteException {
        ServerFile serverFile = fileTable.getFile(relativePath, false, verId, true);
        int serverFd = fdTable.addOpenFile(serverFile.open(false, verId));
        return serverFd;
    }

    /**
     * Write the file on the server
     * 
     * Used to write by chunks
     * 
     * @param serverFd File descriptor of the file on the server
     * @param data     {@link byte[]} Data to write
     * @throws RemoteException
     */
    @Override
    public void writeFile(int serverFd, byte[] data) throws RemoteException {
        if (!fdTable.verifyFd(serverFd)) {
            throw new RemoteException("Invalid file descriptor");
        }
        ServerTempFile openFile = fdTable.getOpenFile(serverFd);
        openFile.write(data);
    }

    /**
     * Remove the file on the server
     * 
     * @param reqPathStr {@link String} Requested path
     * @return {@link FileRemoveResult} Result of the file remove
     * @throws RemoteException
     */
    @Override
    public FileRemoveResult removeFile(String reqPathStr) throws RemoteException {
        /* Check if the path is valid, except the last component */
        String absolutePathStr = PathTools.getAbsolutePath(reqPathStr, rootdir);
        int pathCheckRes = PathTools.checkPath(absolutePathStr, rootdir);
        if (pathCheckRes < 0) {
            return new FileRemoveResult(pathCheckRes, null);
        }
        File file = new File(absolutePathStr);
        if (file.isDirectory()) {
            return new FileRemoveResult(ResCode.EISDIR, null);
        }

        /* Remove the file from the file table */
        String relativePath = PathTools.getRelativePath(absolutePathStr, rootdir);
        int res = fileTable.removeFile(relativePath);
        return new FileRemoveResult(res, relativePath);
    }

    public static void main(String[] args) {
        int port = Integer.parseInt(args[0]); // Get the port
        Path rootPath = Paths.get(args[1]); // Get the root directory
        String rootdir = rootPath.toAbsolutePath().normalize().toString(); // Normalize the root directory
        if (rootdir.charAt(rootdir.length() - 1) != '/') {
            rootdir += "/"; // Add a slash to the end of the root directory if it does not have one
        }

        /* Create the server and bind it to the registry */
        try {
            Server server = new Server(rootdir);
            LocateRegistry.createRegistry(port);
            Naming.rebind("//localhost:" + port + "/Server", server);
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        System.out.println("Server is running at " + rootdir + " on port " + port);
    }
}
