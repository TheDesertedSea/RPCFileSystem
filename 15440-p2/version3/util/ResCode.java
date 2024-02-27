/**
 * Errno.java
 * 
 * @author Cundao Yu <cundaoy@andrew.cmu.edu>
 */

/**
 * Result code and error code
 */
public class ResCode {
    /* Error code */
    public static final int EACCES = -13;
    public static final int EBADF = -9;
    public static final int EINVAL = -22;
    public static final int EISDIR = -21;
    public static final int EEXIST = -17;
    public static final int ENOTDIR = -20;
    public static final int ENOENT = -2;
    public static final int EPERM = -1;
    public static final int EMFILE = -24;

    /* Result code */
    public static final int SUCCESS = 0;
    public static final int NEW_VERSION = 1; // New version available
    public static final int NO_UPDATE = 2; // No update available
    public static final int NOT_EXIST = 3; // File not exist on server
    public static final int IS_DIR = 4; // File is a directory
}
