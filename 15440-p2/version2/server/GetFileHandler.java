import java.io.File;
import java.util.UUID;

public class GetFileHandler {
    private Server server;
    public GetFileHandler(Server server){
        this.server = server;
    }
    
    public FileGetResult getFile(String reqPathStr, UUID proxyVerId, long offset){
        /* Check if the path is valid, except the last component */
        String absolutePathStr = PathTools.getAbsolutePath(reqPathStr, server.getRootdir());
        int pathCheckRes = PathTools.checkPath(absolutePathStr, server.getRootdir()); 
        if(pathCheckRes < 0){
            return new FileGetResult(pathCheckRes, null, null, false, false, null);
        }
        File file = new File(absolutePathStr);
        if(file.isDirectory()){
            return new FileGetResult(ResCode.EISDIR, null, null, false, false, null);
        }

        /* Get the file from the file table */
        String relativePath = PathTools.getRelativePath(absolutePathStr, server.getRootdir());
        ServerFile serverFile = server.getFileTable().getFile(relativePath, false, true);
        if(serverFile == null){
            return new FileGetResult(ResCode.NOT_EXIST, relativePath, null, null, null, null);
        }
        if(proxyVerId != null && proxyVerId.equals(serverFile.getVerId())){
            return new FileGetResult(ResCode.NO_UPDATE, relativePath, null, null, null, null);
        }
        FileGetResult fileGetResult = new FileGetResult(ResCode.NEW_VERSION, relativePath, serverFile.getVerId(), serverFile.canRead(), serverFile.canWrite(), serverFile.getData(offset, Server.CHUNK_SIZE));
        server.getFileTable().giveBackFile(serverFile, true); // Give back the file to the file table
        return fileGetResult;
    }

}
