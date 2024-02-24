import java.util.UUID;

public class PutFileHandler {
    private Server server;
    public PutFileHandler(Server server){
        this.server = server;
    }

    public void putFile(String relativePath, byte[] data, UUID verId, long offset){
        // get the file from the file table
        ServerFile serverFile = server.getFileTable().getFile(relativePath, true, false);
        if(serverFile == null){
            return;
        }
        serverFile.putData(data, verId, offset); // put the data
        server.getFileTable().giveBackFile(serverFile, false); // give back the file
    }
}
