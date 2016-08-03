package Utility;

import javax.crypto.SecretKey;
import java.io.*;

/**
 * Created by thoma_000 on 07-05-2015.
 */
public class Serializer {
    public static byte[] serialize(Object obj){
        byte[] res;
        try
        {
            ByteArrayOutputStream byteStream =
                    new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(byteStream);
            out.writeObject(obj);
            out.flush();
            res = byteStream.toByteArray();
            byteStream.close();
            return res;
        }catch(IOException i)
        {
            i.printStackTrace();
            return null;
        }
    }

    public static <T> T deserialize(byte[] bytes){
        T res;
        try{
            ByteArrayInputStream byteStream = new ByteArrayInputStream(bytes);
            ObjectInputStream in = new ObjectInputStream(byteStream);
            res = (T)in.readObject();
            in.close();
            byteStream.close();
            return res;
        } catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }
}
