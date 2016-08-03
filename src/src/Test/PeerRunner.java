package Test;

import Peer.*;
import Utility.Fac;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.Random;

/**
 * Created by thoma_000 on 07-05-2015.
 */
public class PeerRunner {
    private static StandardPeer obj;

    public static void main(String args[]) throws Exception {
        System.out.println("Peer started");
        String centralIp;
        int capacity;
        int port;
        try {
            centralIp = args[0];
            //capacity = Integer.parseInt(args[1]);
            capacity = calculateCapacity();
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

        obj = new StandardPeer(centralIp, capacity);
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

    public static int calculateCapacity(){
        long start = System.currentTimeMillis();
        Fac.iterative(75000);
        int timeInSeconds = (int)((System.currentTimeMillis() - start)/1000);
        if(timeInSeconds > 10)
            timeInSeconds = 10;
        int capacity = 10 - timeInSeconds;
        return capacity;
    }
}
