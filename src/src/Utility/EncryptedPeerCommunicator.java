package Utility;

import Central.PeerInfo;
import Peer.Peer;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.security.Key;
import java.security.KeyPair;
import java.util.HashMap;
import java.util.List;

/**
 * Created by thoma_000 on 19-05-2015.
 */
public class EncryptedPeerCommunicator implements PeerCommunicator {
    private HashMap<Peer, Key> peerKeys = new HashMap<>();
    private Cipher aesCipher, rsaCipher;
    private KeyPair keyPair;

    public EncryptedPeerCommunicator(KeyPair keyPair){
        this.keyPair = keyPair;
        try {
            rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            aesCipher = Cipher.getInstance("AES");
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    public void init(List<PeerInfo> peers){
        try {
            for (PeerInfo p : peers) {
                Peer peer = (Peer) Naming.lookup("//" + p.id + ":1099" + "/Peer");
                Key privateKey = keyPair.getPrivate();
                rsaCipher.init(Cipher.DECRYPT_MODE, privateKey);
                byte[] byteKey = rsaCipher.doFinal(peer.getSecretKey());
                SecretKey secretKey = Serializer.deserialize(byteKey);
                peerKeys.put(peer, secretKey);
            }
        } catch (Exception e){
            System.out.println("Failed to start encrypted communication with peer.");
        }
    }

    @Override
    public <T> T executeTaskOnPeer(Peer peer, Task<T> task) throws RemoteException{
        try {
            Key secretKey = peerKeys.get(peer);
            byte[] byteTask = Serializer.serialize(task);
            aesCipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedTask = aesCipher.doFinal(byteTask);
            byte[] encryptedResult = peer.executeAndEncryptTask(encryptedTask);
            aesCipher.init(Cipher.DECRYPT_MODE, secretKey);
            return Serializer.deserialize(aesCipher.doFinal(encryptedResult));
        } catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }
}
