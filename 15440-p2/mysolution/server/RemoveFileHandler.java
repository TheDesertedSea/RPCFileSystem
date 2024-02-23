import java.nio.file.Path;
import java.nio.file.Paths;

public class RemoveFileHandler {
    private ServerFileTable fileTable;
    private String rootPath;

    public RemoveFileHandler(ServerFileTable fileTable, String rootPath) {
        this.fileTable = fileTable;
        this.rootPath = rootPath;
    }

    public void removeFile(String path) {
        Path absolutePathObj = Paths.get(path);
        if (!absolutePathObj.isAbsolute()) {
            absolutePathObj = Paths.get(rootPath, path);
        }
        absolutePathObj = absolutePathObj.normalize();
        if(!absolutePathObj.startsWith(rootPath)){
            return; 
        }
        String normalizedPath = absolutePathObj.relativize(Paths.get(rootPath)).toString();
        fileTable.remove(normalizedPath);
    }
}
