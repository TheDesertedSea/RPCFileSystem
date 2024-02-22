import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.UUID;

public class Server extends UnicastRemoteObject implements ServerOperations {

    public static final int NEW_VERSION = 0;
    public static final int NO_UPDATE = 1;
    public static final int NOT_EXIST = 2;
    public static final int IS_DIR = 3;

    private String rootdir;
    private ServerFileTable fileTable;

    private PutFileHandler putFileHandler;
    private GetFileHandler getFileHandler;
    private RemoveFileHandler removeFileHandler;


    protected Server(String rootdir) throws RemoteException {
        super();
        this.rootdir = rootdir;
        this.fileTable = new ServerFileTable();
        putFileHandler = new PutFileHandler(this.fileTable, this.rootdir);
        getFileHandler = new GetFileHandler(this.fileTable, this.rootdir);
        removeFileHandler = new RemoveFileHandler(this.fileTable);
    }

    @Override
    public FileGetResult getFile(String requestPath, UUID proxyVersion) throws RemoteException {
        return getFileHandler.getFile(requestPath, proxyVersion);
    }

    @Override
    public void putFile(String path, byte[] data) throws RemoteException {
        putFileHandler.putFile(path, data);
    }

    @Override
    public void removeFile(String path) throws RemoteException {
        removeFileHandler.removeFile(path);
    }

    public static void main(String[] args) {
        int port = Integer.parseInt(args[0]);
        Path rootPath = Paths.get(args[1]);
        String rootdir = rootPath.normalize().toString();
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
