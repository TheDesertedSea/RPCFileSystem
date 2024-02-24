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

    public String toString(){
        return "----------FileOpenResult---------\n" + 
                "resCode: " + resCode + "\n" +
                "openFile: " + (openFile == null ? "" : openFile.toString()) + "\n" +
                "-------------------------------\n";
    }
}
