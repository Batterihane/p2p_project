package Researcher;

import Central.Central;
import CollatzSequence.CollatzProblemDefinition;
import CollatzSequence.CollatzResult;
import EffectiveScheduling.EffectiveScheduler;
import Utility.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.HashSet;
import java.util.Set;


public class DefaultResearcherRunner {
    private static StandardResearcher researcher;
    private static Central central;
    private static Scheduler effectiveMatrixScheduler, effectiveCollatzScheduler;
    private static SchedulingProblem matrixProblem;
    private static int[][][] matrices;
    private static Set<String> whiteList = new HashSet<>();

    public static void main(String args[]) throws Exception {
        int maxPeerAmount = 5;
        System.out.println("Researcher started");
        String centralIp = "";
        String centralPassword = "let me in";
        int port;

        try {
            centralIp = args[0];
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
        matrices = MatrixGenerator.genrateMatrixSequence(800, 50);
        matrixProblem = new MatrixMultiplicationProblem(matrices, maxPeerAmount);
        SchedulingProblem collatzProblem = new CollatzProblemDefinition(maxPeerAmount, new Interval(1, 10000000));
        effectiveMatrixScheduler = new EffectiveScheduler<int[][]>(central, matrixProblem, 0, whiteList);
        effectiveCollatzScheduler = new EffectiveScheduler<CollatzResult>(central, collatzProblem, 0, whiteList);
        researcher = new StandardResearcher(central, centralPassword, effectiveMatrixScheduler);

        Naming.rebind("//localhost:" + port + "/Researcher", researcher);
        System.out.println("Researcher bound in registry");
        researcher.joinOnCentral();
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
        input = bufferedReader.readLine();
        if(input == null){
            waitForInput();
            return;
        }
        switch (input) {
            case "startMatrixCalc":
                researcher.changeScheduler(effectiveMatrixScheduler);
                researcher.startEncryptedCalculation();
                break;
            case "startCollatzCalc":
                researcher.changeScheduler(effectiveCollatzScheduler);
                researcher.startEncryptedCalculation();
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
