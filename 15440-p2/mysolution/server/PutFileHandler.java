import java.nio.file.Path;
import java.nio.file.Paths;

public class PutFileHandler {

    private ServerFileTable fileTable;
    private String rootPath;

    public PutFileHandler(ServerFileTable fileTable, String rootPath) {
        this.fileTable = fileTable;
        this.rootPath = rootPath;
    }

    public void putFile(String path, byte[] data){
        Path absolutePathObj = Paths.get(path);
        if (!absolutePathObj.isAbsolute()) {
            absolutePathObj = Paths.get(rootPath, path);
        }
        absolutePathObj = absolutePathObj.normalize();
        if(!absolutePathObj.startsWith(rootPath)){
            return; 
        }
        String normalizedPath = absolutePathObj.toString();

        fileTable.put(normalizedPath, data);
    }
}
