import java.util.ArrayList;
import java.util.List;

public class FDTable {
    private static final int MAX_SIZE = 10240;

    private List<OpenFile> openFiles;

    public FDTable() {
        openFiles = new ArrayList<OpenFile>();
    }

    public Boolean verifyFd(int fd) {
        if (fd < 0 || fd >= openFiles.size()) {
            return false;
        }

        if (openFiles.get(fd) == null) {
            return false;
        }

        return true;
    }

    public int getFreeFd() {
        for (int i = 0; i < openFiles.size(); i++) {
            if (openFiles.get(i) == null) {
                return i;
            }
        }

        if (openFiles.size() < MAX_SIZE) {
            openFiles.add(null);
            return openFiles.size() - 1;
        }

        return -1;
    }

    public void addOpenFile(int fd, OpenFile file) {
        openFiles.set(fd, file);
    }

    public OpenFile getOpenFile(int fd) {
        return openFiles.get(fd);
    }

    /**
     * remove the file from the table
     * This just removes the open file from the table, does not close the file
     * @param fd
     */
    public void removeOpenFile(int fd) {
        openFiles.set(fd, null);
    }
}
