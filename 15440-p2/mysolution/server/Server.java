import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;

public class Server extends UnicastRemoteObject implements Operations {

    private String rootdir;

    private CheckHandler checkFileHandler;
    private PutFileHandler putFileHandler;
    private GetFileHandler getFileHandler;

    protected Server(String rootdir) throws RemoteException {
        super();
        this.rootdir = rootdir;
        checkFileHandler = new CheckHandler(rootdir);
        putFileHandler = new PutFileHandler();
        getFileHandler = new GetFileHandler();
    }

    @Override
    public CheckResult check(String path, CheckOption option) throws RemoteException{
        return checkFileHandler.check(path, option);
    }

    @Override
    public byte[] getFile(String path) throws RemoteException{
        return getFileHandler.getFile(path);
    }

    @Override
    public int putFile(String path, byte[] data) throws RemoteException{
        return putFileHandler.putFile(path, data);
    }

    public static void main(String[] args) {
        int port = Integer.parseInt(args[0]);
        String rootdir = args[1];
        Path rootPath = Paths.get(rootdir);
        rootdir = rootPath.normalize().toString();
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
