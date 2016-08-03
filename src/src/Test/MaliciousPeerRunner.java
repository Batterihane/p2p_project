package Test;

/**
 * Created by Nikolaj on 20-05-2015.
 */

import Peer.MaliciousPeer;
import Peer.StandardPeer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.Random;

public class MaliciousPeerRunner {
    private static MaliciousPeer obj;

    public static void main(String args[]) throws Exception {
        System.out.println("Peer started");
        String centralIp;
        int capacity;
        int port;
        int ioSpeed;
        try {
            centralIp = args[0];
            //capacity = Integer.parseInt(args[1]);
            capacity = getRandomCapacity();
            ioSpeed = getRandomIOSpeed();
            //port = Integer.parseInt(args[2]);
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

        obj = new MaliciousPeer(centralIp, capacity, ioSpeed);
        // Bind this object instance to the name "Peer"
        Naming.rebind("//localhost" + "/Peer", obj);
        System.out.println("Peer bound in registry");
        waitForInput();
    }

    public static void waitForInput() throws Exception{
        System.out.println("Waiting for input");
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        String input = "";
        input = bufferedReader.readLine();
        if(input == null){
            waitForInput();
            return;
        }
        switch (input) {
            case "join":
                obj.joinOnCentral();
                break;
            case "exit":
                return;
            default:
                System.out.println("Didn't recognize input");
                break;
        }
        waitForInput();
    }

    private static int getRandomCapacity() {
        Random r = new Random();
        return r.nextInt(10) + 1;
    }

    private static int getRandomIOSpeed() {
        Random r = new Random();
        return r.nextInt(10) + 1;
    }
}

