import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.UUID;

public class Server extends UnicastRemoteObject implements ServerOperations {

    public static final int CHUNK_SIZE = 1024 * 1024;

    private String rootdir;
    private ServerFileTable fileTable;

    private PutFileHandler putFileHandler;
    private GetFileHandler getFileHandler;
    private RemoveFileHandler removeFileHandler;

    protected Server(String rootdir) throws RemoteException {
        super();
        this.rootdir = rootdir;
        this.fileTable = new ServerFileTable(this);
    }

    @Override
    public FileGetResult getFile(String reqPathStr, UUID proxyVerId, long offset) throws RemoteException {
        return getFileHandler.getFile(reqPathStr, proxyVerId, offset);
    }

    @Override
    public void putFile(String relativePath, byte[] data, UUID verId, long offset) throws RemoteException {
        putFileHandler.putFile(relativePath, data, verId, offset);
    }

    @Override
    public FileRemoveResult removeFile(String reqPathStr) throws RemoteException {
        return removeFileHandler.removeFile(reqPathStr);
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
        if(rootdir.charAt(rootdir.length() - 1) != '/') {
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
}
