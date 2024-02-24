import java.io.File;

public class RemoveFileHandler {
    private Server server;
    public RemoveFileHandler(Server server){
        this.server = server;
    }

    public FileRemoveResult removeFile(String reqPathStr) {
        /* Check if the path is valid, except the last component */
        String absolutePathStr = PathTools.getAbsolutePath(reqPathStr, server.getRootdir());
        int pathCheckRes = PathTools.checkPath(absolutePathStr, server.getRootdir()); 
        if(pathCheckRes < 0){
            return new FileRemoveResult(pathCheckRes, null);
        }
        File file = new File(absolutePathStr);
        if(file.isDirectory()){
            return new FileRemoveResult(ResCode.EISDIR, null);
        }

        /* Remove the file from the file table */
        String relativePath = PathTools.getRelativePath(absolutePathStr, server.getRootdir());
        int res = server.getFileTable().removeFile(relativePath);
        return new FileRemoveResult(res, relativePath);
    }
}
