
/**
 * UnlinkHandler.java
 * 
 * @author Cundao Yu <cundaoy@andrew.cmu.edu>
 */
import java.rmi.RemoteException;

public class UnlinkHandler {
    public UnlinkHandler() {
    }

    public int unlink(String path) {
        try {
            FileRemoveResult res = Proxy.getServer().removeFile(path);
            Logger.log("Remove file result:" + res.toString());
            if (res.getResCode() < 0) {
                return res.getResCode();
            }
            Proxy.getCache().removeFile(res.getRelativePath());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return 0;
    }
}