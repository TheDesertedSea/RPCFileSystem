public class FileOpenResult {
    private int resCode;
    private OpenFile openFile;

    public FileOpenResult(int resCode, OpenFile openFile) {
        this.resCode = resCode;
        this.openFile = openFile;
    }

    public int getResCode() {
        return resCode;
    }

    public OpenFile getOpenFile() {
        return openFile;
    }
}
