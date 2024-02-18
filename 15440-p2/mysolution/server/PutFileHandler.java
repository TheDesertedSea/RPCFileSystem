import java.io.File;
import java.io.FileOutputStream;

public class PutFileHandler {
    public int putFile(String path, byte[] data){
        File file = new File(path);
        if(!file.exists()){
            try{
                file.createNewFile();
            } catch (Exception e){
                e.printStackTrace();
            }
        }

        try{
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(data);
            fileOutputStream.close();
        } catch (Exception e){
            e.printStackTrace();
        }

        return 0;
    }
}
