
/**
 * UnlinkHandler.java
 * 
 * @author Cundao Yu <cundaoy@andrew.cmu.edu>
 */
import java.rmi.RemoteException;

/**
 * Handler for unlink operation
 */
public class UnlinkHandler {
    /**
     * Constructor
     */
    public UnlinkHandler() {
    }

    /**
     * Unlink a file
     * 
     * @param path {@link String} File path
     * @return {@link Integer} 0 if success, otherwise a negative error code
     */
    public int unlink(String path) {
        try {
            FileRemoveResult res = Proxy.getServer().removeFile(path); // Remove on server
            if (res.getResCode() < 0) {
                return res.getResCode(); // Return error code if failed
            }
            Proxy.getCache().removeFile(res.getRelativePath()); // Remove from cache
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return 0;
    }
}