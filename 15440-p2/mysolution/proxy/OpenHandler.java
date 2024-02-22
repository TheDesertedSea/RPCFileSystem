import java.nio.file.OpenOption;

public class OpenHandler {
    private FDTable fdTable;  

    public OpenHandler(FDTable fdTable) {
        this.fdTable = fdTable;
    }

    int open(String path, OpenOption option){
        return 0;
    }
}
