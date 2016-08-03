package Test;

import Central.Central;
import Researcher.*;
import Utility.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by thoma_000 on 06-05-2015.
 */
public class ResearcherRunner {
    private static StandardResearcher researcher;
    private static Central central;
    private static Scheduler naiveScheduler, soloScheduler, effectiveScheduler;
    private static SchedulingProblem matrixProblem;
    private static int[][][] matrices;
    private static Set<String> whiteList = new HashSet<>();

    public static void main(String args[]) throws Exception {
        System.out.println(Arrays.toString(args));
        System.out.println("Researcher started");
        String centralIp = "";
        String centralPassword = "let me in";
        int port;

        try {
            centralIp = args[0];
            //centralPassword = args[1];
            //port = Integer.parseInt(args[2]);
            port = 1099;
        } catch (Exception e) {
            System.out.println("The format is expected to be: <Central IP>"); // <registry port: int>
            return;
        }

        whiteList.add(getMyHostName());

        try { //special exception handler for registry creation
            LocateRegistry.createRegistry(port);
            System.out.println("java RMI registry created.");
        } catch (RemoteException e) {
            LocateRegistry.getRegistry(port);
            //registry already exists
            System.out.println("java RMI registry already exists.");
        }
        central = (Central) Naming.lookup("//" + centralIp + "/Central");
        matrices = MatrixGenerator.genrateMatrixSequence(500, 50);
        matrixProblem = new MatrixMultiplicationProblem(matrices, 4);
        naiveScheduler = new NaiveScheduler(central, matrixProblem, 0, whiteList);
        researcher = new StandardResearcher(central, centralPassword, naiveScheduler);

        Naming.rebind("//localhost:" + port + "/Researcher", researcher);
        System.out.println("Researcher bound in registry");

        waitForInput();
    }

    private static String getMyHostName(){
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static void waitForInput() throws Exception {
        System.out.println("Waiting for input");
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        String input = "";
        String sizeInput;
        int size;
        long timer;
        input = bufferedReader.readLine();
        if(input == null){
            waitForInput();
            return;
        }
        switch (input) {
            case "join":
                researcher.joinOnCentral();
                break;
            case "startTask":
                System.out.println("Enter matrix size..");
                sizeInput = bufferedReader.readLine();
                if(!sizeInput.matches("[0-9]+")){
                    System.out.println("Size must be a number");
                    break;
                }
                size = Integer.parseInt(sizeInput);
                researcher.startMatrixMultiplication(size);
                break;
            case "startTask2":
                System.out.println("Enter matrix size..");
                sizeInput = bufferedReader.readLine();
                if(!sizeInput.matches("[0-9]+")){
                    System.out.println("Size must be a number");
                    break;
                }
                size = Integer.parseInt(sizeInput);
                researcher.startEncryptedMatrixMultiplication(size);
                break;
            case "startTask3":
                System.out.println("Starting matrix calculations and timer");
                timer = System.currentTimeMillis();
                researcher.startCalculation();
                System.out.println("It took in total: " + (System.currentTimeMillis() - timer) / 1000. + " seconds");
                break;
            case "startTask3e":
                System.out.println("Starting matrix calculations and timer");
                timer = System.currentTimeMillis();
                researcher.startEncryptedCalculation();
                System.out.println("It took in total: " + (System.currentTimeMillis() - timer) / 1000. + " seconds");
                break;
            case "startSoloTask":
                System.out.println("Starting matrix calculations and timer");
                timer = System.currentTimeMillis();
                SoloMatrixMultiplicationThreaded2.startMatrixMultiplication(matrices);
                System.out.println("It took in total: " + (System.currentTimeMillis() - timer) / 1000. + " seconds");
                break;
            case "startSoloThreaded":
                System.out.println("Starting matrix calculations and timer");
                timer = System.currentTimeMillis();
                SoloMatrixMultiplicationThreaded.startMatrixMultiplication(matrices);
                System.out.println("It took in total: " + (System.currentTimeMillis() - timer) / 1000. + " seconds");
                break;
            case "changeScheduler":
                System.out.println("Enter name of scheduler..");
                input = bufferedReader.readLine();
                switch (input){
                    case "naive":
                        System.out.println("Enter verification probability..");
                        input = bufferedReader.readLine();
                        try {
                            double verificationProbability = Double.parseDouble(input);
                            naiveScheduler = new NaiveScheduler(central, matrixProblem, verificationProbability, whiteList);
                            researcher.changeScheduler(naiveScheduler);
                        } catch (Exception e){
                            System.out.println("Input must be a double.");
                        }
                        break;
                    case "effective":
                        System.out.println("Enter verification probability..");
                        input = bufferedReader.readLine();
                        try {
                            double verificationProbability = Double.parseDouble(input);
                            effectiveScheduler = new NaiveScheduler(central, matrixProblem, verificationProbability, whiteList);
                            researcher.changeScheduler(effectiveScheduler);
                        } catch (Exception e){
                            System.out.println("Input must be a double.");
                        }
                        break;
                    case "solo":
                        researcher.changeScheduler(soloScheduler);
                        break;
                    default:
                        System.out.println("Didn't recognize scheduler");
                        break;
                }
                break;
            case "exit":
                return;
            default:
                System.out.println("Didn't recognize input");
                break;
        }
        waitForInput();
    }
}
