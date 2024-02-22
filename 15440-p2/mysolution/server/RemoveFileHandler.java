public class RemoveFileHandler {
    private ServerFileTable fileTable;

    public RemoveFileHandler(ServerFileTable fileTable) {
        this.fileTable = fileTable;
    }

    public void removeFile(String path) {
        fileTable.remove(path);
    }
}
