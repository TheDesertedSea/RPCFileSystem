import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class GetFileHandler {
    
    public byte[] getFile(String path){
        File file = new File(path);
        if(!file.exists()){
            return null;
        }

        byte[] data = new byte[(int) file.length()];
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            fileInputStream.read(data);
            fileInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return data;
    }
}
