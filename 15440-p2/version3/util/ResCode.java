/**
 * Errno.java
 * 
 * @author Cundao Yu <cundaoy@andrew.cmu.edu>
 */

public class ResCode{
    public static final int EACCES = -13;
    public static final int EBADF = -9;
    public static final int EINVAL = -22;
    public static final int EISDIR = -21;
    public static final int EEXIST = -17;
    public static final int ENOTDIR = -20;
    public static final int ENOENT = -2;
    public static final int EPERM = -1;
    public static final int EMFILE = -24;
    
    public static final int SUCCESS = 0;
    public static final int NEW_VERSION = 1;
    public static final int NO_UPDATE = 2;
    public static final int NOT_EXIST = 3;
    public static final int IS_DIR = 4;
}
