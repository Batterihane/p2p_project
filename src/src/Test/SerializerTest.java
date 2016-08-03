package Test;

import Central.PeerInfo;
import Utility.*;

/**
 * Created by thoma_000 on 07-05-2015.
 */
public class SerializerTest {
    public static void main(String[] args) {
        PeerInfo p = new PeerInfo("asdf", 10);
        byte[] b = Serializer.serialize(p);
        PeerInfo p2 = Serializer.deserialize(b);
        System.out.println(p2.getId());
    }

    //public stringGenerate()
}
