/**
 * Logger.java
 * 
 * @Author Cundao Yu <cundaoy@andrew.cmu.edu>
 */

/**
 * Logger
 * 
 * Just a simple logger
 */
public class Logger {
    private static final Boolean DEBUG = true;

    public static void log(String message) {
        if (DEBUG) {
            System.err.println(message);
        }
    }
}
