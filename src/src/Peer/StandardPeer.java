package Peer;

import Utility.Fac;
import Utility.Serializer;
import Utility.Task;
import Central.Central;
import Researcher.Researcher;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

/**
 * Created by thoma_000 on 29-04-2015.
 */
public class StandardPeer extends UnicastRemoteObject implements Peer {

    private String centralIp;
    private Central central;
    private int capacity;
    private Cipher rsaCipher, aesCipher;
    private HashMap<String, ResearcherKeys> verifiedResearchers = new HashMap<>();
    private KeyGenerator keyGen;
    private Random random = new Random();

    public StandardPeer(String centralIp, int capacity) throws RemoteException {
        super();
        this.centralIp = centralIp;
        this.capacity = capacity;

        try {
            rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            aesCipher = Cipher.getInstance("AES");
        } catch(Exception e){
            e.printStackTrace();
        }
        createKeyGenerator();
    }

    public static void main(String args[]) throws Exception {
        System.out.println("Peer started");
        String centralIp;
        int capacity;
        int port;
        try {
            centralIp = args[0];
            capacity = calculateCapacity();
            /*if(capacity < 1){
                System.out.println("You must have at least 1 capacity to join the system.");
                return;
            }*/
            port = 1099;
        }
        catch(Exception e) {
            System.out.println("The format is expected to be: <Central IP>"); // <registry port: int>
            return;
        }

        //Instantiate Peer
        try {
            LocateRegistry.createRegistry(port);
            System.out.println("java RMI registry created.");


        } catch (RemoteException e) {
            //registry already exists
            System.out.println("java RMI registry already exists.");
        }

        StandardPeer obj = new StandardPeer(centralIp, capacity);
        // Bind this object instance to the name "Peer"
        Naming.rebind("//localhost" + "/Peer", obj);
        System.out.println("Peer bound in registry");
        obj.joinOnCentral();
    }

    public static int calculateCapacity(){
        long start = System.currentTimeMillis();
        Fac.iterative(75000);
        int timeInSeconds = (int)((System.currentTimeMillis() - start)/1000);
        if(timeInSeconds > 10)
            timeInSeconds = 10;
        int capacity = 10 - timeInSeconds;
        return capacity;
    }

    public boolean joinOnCentral() {
        try {
            central = (Central) Naming.lookup("//" + centralIp + "/Central");
            if(central.joinAsPeer(capacity)){
                System.out.println("Successfully joined on central");
                return true;
            }
            System.out.println("Failed joining on central with ip: " + centralIp);
            return false;
        } catch(Exception e) {
            System.out.println("Failed joining on central with ip: " + centralIp);
            return false;
        }
    }

    public boolean verifyAndAddResearcher(String researcherIp) {
        try {
            System.out.println("Requesting verification of: " + researcherIp);
            Key publicKey = central.getResearcherPublicKey(researcherIp);
            rsaCipher.init(Cipher.ENCRYPT_MODE, publicKey);

            byte[] plainText = new byte[100];
            random.nextBytes(plainText);
            byte[] cipherText = rsaCipher.doFinal(plainText);
            Researcher researcher = (Researcher)Naming.lookup("//" + researcherIp + ":1099" + "/Researcher");
            if(Arrays.equals(researcher.decryptMessage(cipherText), plainText)){
                ResearcherKeys keys = new ResearcherKeys(publicKey, keyGen.generateKey());
                verifiedResearchers.put(researcherIp, keys);
                System.out.println("Verified and added researcher");
                return true;
            }
            return false;

        } catch(Exception e) {
            System.out.println("Failed to verify researcher with ip: " + researcherIp);
            return false;
        }
    }

    @Override
    public <T> T executeTask(Task<T> task) throws RemoteException {
        System.out.println("Received task... Timer started.");
        long start = System.currentTimeMillis();
        try {
            String researcher = getClientHost();
            if(!verifiedResearchers.containsKey(researcher)){
                if(!verifyAndAddResearcher(researcher)){
                    return null;
                }
            }
            System.out.println("Starting calculation of task.");
            T result = task.execute();
            System.out.println("Task completed.");
            System.out.println("The task took (including possible verification): " + (System.currentTimeMillis() - start) + " ms");
            return result;

        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public <T> byte[] executeAndEncryptTask(byte[] encryptedTask) throws RemoteException {
        try {
            String researcher = getClientHost();
            if(!verifiedResearchers.containsKey(researcher)){
                System.out.println("Unknown researcher tried to start execution.");
                return null; // researcher should get secret key from peer (and be verified) before starting execution
            }
            SecretKey secretKey = verifiedResearchers.get(researcher).getSecretKey();
            aesCipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] byteTask = aesCipher.doFinal(encryptedTask);
            Task<T> task = Serializer.deserialize(byteTask);
            T result = executeTask(task);

            aesCipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return aesCipher.doFinal(Serializer.serialize(result));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public byte[] getSecretKey() {
        byte[] res;
        SecretKey secretKey;
        Key publicKey;
        String researcherId = "";
        try {
            researcherId = getClientHost();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(!verifiedResearchers.containsKey(researcherId)) {
            verifyAndAddResearcher(researcherId);
        }
        secretKey = verifiedResearchers.get(researcherId).getSecretKey();
        publicKey = verifiedResearchers.get(researcherId).getPublicKey();
        try {
            rsaCipher.init(Cipher.ENCRYPT_MODE, publicKey);
            res = rsaCipher.doFinal(Serializer.serialize(secretKey));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return res;
    }

    private void createKeyGenerator() {
        try {
            keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(128);
        } catch(NoSuchAlgorithmException e){
            System.out.println("Failed to create KeyPairGenerator.");
        }
    }
}
