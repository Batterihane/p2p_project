package Researcher;

import Central.*;
import Peer.Peer;
import Utility.*;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.security.Key;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by thoma_000 on 29-04-2015.
 */
public class StandardResearcher extends UnicastRemoteObject implements Researcher {

    private Cipher rsaCipher, aesCipher;
    private Central central;
    private String centralPassword;
    private KeyPair keyPair;
    private Scheduler scheduler;

    public StandardResearcher(Central central, String centralPassword) throws RemoteException{
        super();

        this.central = central;
        this.centralPassword = centralPassword;
        try {
            rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            aesCipher = Cipher.getInstance("AES");
        } catch(Exception e){
            System.out.println(e.getStackTrace());
        }
        //joinOnCentral();

    }

    public StandardResearcher(Central central, String centralPassword, Scheduler scheduler) throws RemoteException{
        this(central, centralPassword);
        this.scheduler = scheduler;
    }

    public static void main(String args[]) throws Exception {
        System.out.println(Arrays.toString(args));
        System.out.println("Researcher started");
        String centralIp = "";
        String centralPassword = "";
        int port;

        try {
            centralIp = args[0];
            centralPassword = args[1];
            //port = Integer.parseInt(args[2]);
            port = 1099;
        } catch (Exception e) {
            System.out.println("The format is expected to be: <Central IP> <Central password>"); // <registry port: int>
            return;
        }

        try { //special exception handler for registry creation
            LocateRegistry.createRegistry(port);
            System.out.println("java RMI registry created.");
        } catch (RemoteException e) {
            LocateRegistry.getRegistry(port);
            //registry already exists
            System.out.println("java RMI registry already exists.");
        }
        Central central = (Central) Naming.lookup("//" + centralIp + "/Central");
        StandardResearcher obj = new StandardResearcher(central, centralPassword);

        Naming.rebind("//localhost:" + port + "/Researcher", obj);
        System.out.println("Researcher bound in registry");
        obj.joinOnCentral();
    }

    public void changeScheduler(Scheduler scheduler){
        this.scheduler = scheduler;
    }

    public boolean joinOnCentral() {
        try {
            keyPair = central.joinAsResearcher(centralPassword);
            if(keyPair != null){
                System.out.println("Successfully joined on central");
                return true;
            }
            System.out.println("Failed joining on central.");
            return false;
        } catch(Exception e) {
            System.out.println("Failed joining on central.");
            return false;
        }
    }

    @Override
    public byte[] decryptMessage(byte[] cipherText) throws RemoteException {
        System.out.println("A request to decrypt a message has been received");

        try {
            Key privateKey = keyPair.getPrivate();
            rsaCipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] plainText = rsaCipher.doFinal(cipherText);
            return plainText;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<PeerInfo> getPeers(int amount){
        System.out.println("A request for the peer list has been received");
        try {
            return central.getPeersCapacity(amount);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return new ArrayList<PeerInfo>();
    }

    public Task generateMatrixTask(int size){
        int[][] a = MatrixGenerator.generateMatrix(size);
        int[][] b = MatrixGenerator.generateMatrix(size);

        //return new MatrixCalculator(a,b);
        //return new MatrixMultiplicationTask(a,b);
        return new MatrixMultiplicationTaskWithIntervals(a,b, null, null);
    }

    public void startCalculation(){
        scheduler.disableEncryption();
        scheduler.start();
    }

    public void startEncryptedCalculation(){
        scheduler.enableEncryption(keyPair);
        scheduler.start();
    }

    public void startMatrixMultiplication(int size){
        System.out.println("Starting matrix multiplicaiton");
        System.out.println("Generating matrices");
        Task t = generateMatrixTask(size);
        System.out.println("Matrixes generated");

        System.out.println("timer initiated");
        long start = System.currentTimeMillis();

        List<PeerInfo> list = getPeers(10);
        if(list.size() == 0) {
            System.out.println("No peers were available for processing");
            return;
        }

        try {
            Peer p = (Peer) Naming.lookup("//" + list.get(0).id + ":1099" + "/Peer");
            System.out.println("Peer found. Pushing task.");
            Object obj = p.executeTask(t);
            System.out.println("Peer returned: " + obj);
            System.out.println("The task took: " + (System.currentTimeMillis() - start) + " ms");
        } catch (NotBoundException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void startEncryptedMatrixMultiplication(int size){
        System.out.println("Starting matrix multiplicaiton");
        System.out.println("Generating matrices");
        Task t = generateMatrixTask(size);
        System.out.println("Matrixes generated");

        System.out.println("timer initiated");
        long start = System.currentTimeMillis();

        List<PeerInfo> list = getPeers(10);
        if(list.size() == 0) {
            System.out.println("No peers were available for processing");
            return;
        }

        try {
            Peer p = (Peer) Naming.lookup("//" + list.get(0).id + ":1099" + "/Peer");
            System.out.println("Peer found, establishing safe connection.");

            Key privateKey = keyPair.getPrivate();
            rsaCipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] byteKey = rsaCipher.doFinal(p.getSecretKey());
            SecretKey secretKey = Serializer.deserialize(byteKey);

            byte[] byteTask = Serializer.serialize(t);
            aesCipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedTask = aesCipher.doFinal(byteTask);

            System.out.println("Pushing task.");
            byte[] bytes = p.executeAndEncryptTask(encryptedTask);
            System.out.println("cipher: " + bytes);

            aesCipher.init(Cipher.DECRYPT_MODE,secretKey);
            int[][] obj = Serializer.deserialize(aesCipher.doFinal(bytes));

            System.out.println("Peer returned: " + obj.length);
            System.out.println("The task took: " + (System.currentTimeMillis() - start) + " ms");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
