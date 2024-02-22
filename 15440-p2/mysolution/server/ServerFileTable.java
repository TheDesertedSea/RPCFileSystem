import java.io.File;
import java.io.FileOutputStream;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ServerFileTable {
    private ConcurrentHashMap<String, ServerFile> fileTable;

    public ServerFileTable() {
        fileTable = new ConcurrentHashMap<>();
    }

    public UUID startGet(String path) {
        ServerFile serverVersion = fileTable.get(path);
        if (serverVersion == null) {
            return null;
        }
        serverVersion.freezeRemove();
        if (serverVersion != fileTable.get(path)) {
            serverVersion.unfreezeRemove();
            return null;
        }
        return serverVersion.startGet();
    } // if got, remove_lock and then get to check if removed from table, return if
      // removed

    public void endGet(String path) {
        ServerFile serverVersion = fileTable.get(path);
        if (serverVersion == null) {
            Logger.log("ServerVersionTable: endGet: serverVersion is null");
            return;
        }
        serverVersion.endGet();
        serverVersion.unfreezeRemove();
    }

    public void put(String path, byte[] data) {
        ServerFile serverVersion = fileTable.getOrDefault(path, new ServerFile());
        serverVersion.freezeRemove();
        if (serverVersion != fileTable.get(path)) {
            serverVersion.unfreezeRemove();
            return;
        }
        serverVersion.startPut();
        File file = new File(path);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (Exception e) {
                serverVersion.endPut();
                serverVersion.unfreezeRemove();
                e.printStackTrace();
                return;
            }
        }
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(data);
            fileOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        serverVersion.endPut();
        serverVersion.unfreezeRemove();
    }

    public void remove(String path) {
        ServerFile serverVersion = fileTable.get(path);
        if (serverVersion == null) {
            return;
        }
        serverVersion.startRemove();
        if (serverVersion != fileTable.get(path)) {
            serverVersion.endRemove();
            return;
        }
        fileTable.remove(path);
        File file = new File(path);
        if (!file.exists()) {
            serverVersion.endRemove();
            return;
        }
        try {
            file.delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
        serverVersion.endRemove();
    } // if got, write remove_lock and then check, if null return
}
