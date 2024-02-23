/**
 * UnlinkHandler.java
 * 
 * @author Cundao Yu <cundaoy@andrew.cmu.edu>
 */
import java.rmi.RemoteException;

public class UnlinkHandler{
    public UnlinkHandler() {
    }

    public int unlink(String path) {
        try {
            FileRemoveResult res = Proxy.getServer().removeFile(path);
            if(res.getRescode() < 0) {
                return res.getRescode();
            }
            Proxy.getCache().removeFile(res.getNormalizedPath());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return 0;
    }
}