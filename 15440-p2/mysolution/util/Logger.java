/**
 * Logger.java
 * 
 * @Author Cundao Yu <cundaoy@andrew.cmu.edu>
 */
import java.io.File;

public class Logger {
    private static final Boolean DEBUG = true;

    public static void log(String message) {
        if(DEBUG) {
            System.err.println(message);
        }
    }

    public static void logFileInfo(File file){
        if(!DEBUG) {
            return;
        }

        System.err.println("-----------------");
        System.err.println("File Information:");
        System.err.println("Name: " + file.getName());
        System.err.println("Path: " + file.getPath());
        System.err.println("Absolute Path: " + file.getAbsolutePath());
        System.err.println("Parent: " + file.getParent());
        System.err.println("Exists: " + file.exists());
        System.err.println("Is Directory: " + file.isDirectory());
        System.err.println("Is File: " + file.isFile());
        System.err.println("Can Read: " + file.canRead());
        System.err.println("Can Write: " + file.canWrite());
        System.err.println("-----------------");
    }

}
