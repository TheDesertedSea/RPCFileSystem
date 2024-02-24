import java.util.ArrayList;
import java.util.List;

public class ServerFDTable {
    private static final int MAX_SIZE = 10240;

    private List<ServerOpenFile> openFiles;

    public ServerFDTable() {
        openFiles = new ArrayList<ServerOpenFile>();
    }

    public Boolean verifyFd(int fd) {
        if(fd < 0 || fd >= openFiles.size()) {
            return false;
        }

        if(openFiles.get(fd) == null) {
            return false;
        }

        return true;
    }

    public int getFreeFd() {
        for(int i = 0; i < openFiles.size(); i++) {
            if(openFiles.get(i) == null) {
                return i;
            }
        }

        if(openFiles.size() < MAX_SIZE) {
            openFiles.add(null);
            return openFiles.size() - 1;
        }

        return -1;
    }

    public void addOpenFile(int fd, ServerOpenFile file) {
        openFiles.set(fd, file);
    }

    public ServerOpenFile getOpenFile(int fd) {
        return openFiles.get(fd);
    }

    public void removeOpenFile(int fd) {
        openFiles.set(fd, null);
    }
}
