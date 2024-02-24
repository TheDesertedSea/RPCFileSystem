import java.io.File;
import java.io.FileOutputStream;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ServerFileTable {
    private ConcurrentHashMap<String, ServerFile> fileTable;
    private String rootdir;

    public ServerFileTable(String rootdir) {
        fileTable = new ConcurrentHashMap<>();
        this.rootdir = rootdir;
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
    }

    public void endGet(String path) {
        ServerFile serverVersion = fileTable.get(path);
        if (serverVersion == null) {
            Logger.log("ServerVersionTable: endGet: serverVersion is null");
            return;
        }
        serverVersion.endGet();
        serverVersion.unfreezeRemove();
    }

    public void put(String path, byte[] data, UUID version) {
        ServerFile tempForAbsent = new ServerFile(version);
        ServerFile serverVersion = fileTable.putIfAbsent(path, tempForAbsent);
        if(serverVersion == null){
            serverVersion = tempForAbsent;
        }
        serverVersion.freezeRemove();
        if (serverVersion != fileTable.get(path)) {
            serverVersion.unfreezeRemove();
            return;
        }
        serverVersion.startPut();
        File file = new File(rootdir + path);
        Logger.logFileInfo(file);
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

    public int remove(String path) {
        ServerFile tempForAbsent = new ServerFile();
        ServerFile serverVersion = fileTable.putIfAbsent(path, tempForAbsent);
        if(serverVersion == null){
            serverVersion = tempForAbsent;
        }
        serverVersion.startRemove();
        if (serverVersion != fileTable.get(path)) {
            serverVersion.endRemove();
            return ResCode.ENOENT;
        }
        fileTable.remove(path);
        File file = new File(rootdir + path);
        Logger.logFileInfo(file);
        if (!file.exists()) {
            serverVersion.endRemove();
            return ResCode.ENOENT;
        }
        try {
            file.delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
        serverVersion.endRemove();
        return 0;
    } 

    public void addNewFileToManage(String path) {
        fileTable.putIfAbsent(path, new ServerFile());
    }
}
