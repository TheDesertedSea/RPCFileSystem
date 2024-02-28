import java.io.File;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.UUID;

public class Server extends UnicastRemoteObject implements ServerOperations {

    public static final int CHUNK_SIZE = 1024 * 128;

    private String rootdir;
    private ServerFileTable fileTable;
    private ServerFDTable fdTable;

    protected Server(String rootdir) throws RemoteException {
        super();
        this.rootdir = rootdir;
        this.fileTable = new ServerFileTable(this);
        this.fdTable = new ServerFDTable();
    }

    public String getRootdir() {
        return rootdir;
    }

    public ServerFileTable getFileTable() {
        return fileTable;
    }

    public static void main(String[] args) {
        int port = Integer.parseInt(args[0]);
        Path rootPath = Paths.get(args[1]);
        String rootdir = rootPath.toAbsolutePath().normalize().toString();
        if (rootdir.charAt(rootdir.length() - 1) != '/') {
            rootdir += "/";
        }
        try {
            Server server = new Server(rootdir);
            LocateRegistry.createRegistry(port);
            Naming.rebind("//localhost:" + port + "/Server", server);
            Logger.log("Server is running " + rootdir + " on port " + port);
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public FileCheckResult checkFile(String reqPathStr, UUID proxyVerId) throws RemoteException {
        Logger.log("Checking file: " + reqPathStr + " with proxyVerId: " + proxyVerId);
        /* Check if the path is valid, except the last component */
        String absolutePathStr = PathTools.getAbsolutePath(reqPathStr, rootdir);
        int pathCheckRes = PathTools.checkPath(absolutePathStr, rootdir);
        if (pathCheckRes < 0) {
            return new FileCheckResult(pathCheckRes, null, null, false, false, -1, -1, null);
        }
        File file = new File(absolutePathStr);
        if (file.isDirectory()) {
            return new FileCheckResult(ResCode.EISDIR, null, null, false, false, -1, -1, null);
        }

        /* Get the file from the file table */
        String relativePath = PathTools.getRelativePath(absolutePathStr, rootdir);
        ServerFile serverFile = fileTable.getFile(relativePath, true, null, false);
        if (serverFile == null) {
            return new FileCheckResult(ResCode.NOT_EXIST, relativePath, null, false, false, -1, -1, null);
        }
        if (proxyVerId != null && proxyVerId.equals(serverFile.getVerId())) {
            return new FileCheckResult(ResCode.NO_UPDATE, relativePath, proxyVerId, serverFile.canRead(),
                    serverFile.canWrite(), -1, -1, null);
        }

        ServerOpenFile openFile = serverFile.open(true, null);
        int serverFd = fdTable.addOpenFile(openFile);
        FileCheckResult fileCheckResult = new FileCheckResult(ResCode.NEW_VERSION, relativePath, serverFile.getVerId(), serverFile.canRead(),
        serverFile.canWrite(), serverFd, serverFile.getSize(), openFile.read());
        Logger.log("File check result: " + fileCheckResult.toString());
        return fileCheckResult;
    }

    @Override
    public void closeFile(int serverFd) throws RemoteException {
        Logger.log("Closing file: " + serverFd);
        if(!fdTable.verifyFd(serverFd)){
            throw new RemoteException("Invalid file descriptor");
        }
        ServerOpenFile openFile = fdTable.getOpenFile(serverFd);
        openFile.close();
        fdTable.removeOpenFile(serverFd);
    }

    @Override
    public byte[] readFile(int serverFd) throws RemoteException {
        if(!fdTable.verifyFd(serverFd)){
            throw new RemoteException("Invalid file descriptor");
        }
        ServerOpenFile openFile = fdTable.getOpenFile(serverFd);
        return openFile.read();
    }

    public int putFile(String relativePath, UUID verId) throws RemoteException {
        Logger.log("Putting file: " + relativePath + " with verId: " + verId);
        ServerFile serverFile = fileTable.getFile(relativePath, false, verId, true);
        int serverFd = fdTable.addOpenFile(serverFile.open(false, verId));
        return serverFd;
    }

    @Override
    public void writeFile(int serverFd, byte[] data) throws RemoteException {
        if(!fdTable.verifyFd(serverFd)){
            throw new RemoteException("Invalid file descriptor");
        }
        ServerOpenFile openFile = fdTable.getOpenFile(serverFd);
        openFile.write(data);
    }

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
}
