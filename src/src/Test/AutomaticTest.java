package Test;

import Central.Central;
import CollatzSequence.CollatzProblemDefinition;
import CollatzSequence.CollatzResult;
import Researcher.*;
import Utility.*;
import EffectiveScheduling.*;

import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by thoma_000 on 20-05-2015.
 */
public class AutomaticTest {
    static StandardResearcher researcher;
    static Central central;
    static Scheduler scheduler;
    static SchedulingProblem matrixProblem;
    static SchedulingProblem collatzProblem;
    static int[][][] matrices;
    static Set<String> whiteList = new HashSet<>();
    static String centralIp;
    static String centralPassword = "let me in";
    static int port, maxPeerAmount = 6;

    public static void main(String args[]) throws Exception{
        long[] peerAmountTestResult;
        long[] matrixSizeTestResult;
        long[] verificationAmountTestResult;

        if(!init(args)) return;
        researcher.startCalculation();

        //simpleMatrixTest(800, 50, false, new PrintWriter("naive.txt", "UTF-8"));
        simpleMatrixTest(800, 50, true, new PrintWriter("effective.txt", "UTF-8"));
        //encryptedMatrixSizeTest(50, true, new PrintWriter("encryptedMatrixSizeTest.txt", "UTF-8"));
        //encryptedVsNonEncryptedTest(600, 400, true, new PrintWriter("encryptedVsNonEncryptedTest.txt", "UTF-8"));
        /*System.out.println("Starting peerAmountTest with naive scheduler (600x600x400)..");
        peerAmountTest(600, 400, false, new PrintWriter("peerAmountNaiveTest1.txt", "UTF-8"));*/
        //System.out.println("Starting peerAmountTest with effective scheduler (600x600x400)..");
        //peerAmountTest(600, 400, true, new PrintWriter("peerAmountEffectiveTest1.txt", "UTF-8"));
        /*System.out.println("Starting peerAmountTest with naive scheduler (800x800x50)..");
        peerAmountTest(800, 50, false, new PrintWriter("peerAmountNaiveTest2.txt", "UTF-8"));
        System.out.println("Starting peerAmountTest with effective scheduler..");
        peerAmountTest(800, 50, true, new PrintWriter("peerAmountEffectiveTest2.txt", "UTF-8"));*/

        /*System.out.println("Starting verificationAmountTest with naive scheduler (600x600x400)..");
        verificationAmountTest(600, 400, false, new PrintWriter("verificationAmountNaiveTest1.txt", "UTF-8"));
        System.out.println("Starting verificationAmountTest with effective scheduler (600x600x400)..");
        verificationAmountTest(600, 400, true, new PrintWriter("verificationAmountEffectiveTest1.txt", "UTF-8"));*/
        //System.out.println("Starting verificationAmountTest with naive scheduler (800x800x50)..");
        //verificationAmountTest(800, 50, false, new PrintWriter("verificationAmountNaiveTest2.txt", "UTF-8"));
        //System.out.println("Starting verificationAmountTest with effective scheduler (800x800x50)..");
        //verificationAmountTest(800, 50, true, new PrintWriter("verificationAmountEffectiveTest2.txt", "UTF-8"));

        //System.out.println("Starting collatzPeerAmountTest with naive scheduler..");
        //collatzPeerAmountTest(50000000, false, new PrintWriter("collatzPeerAmountNaiveTest.txt", "UTF-8"));
        //System.out.println("Starting collatzPeerAmountTest with effective scheduler..");
        //collatzPeerAmountTest(50000000, true, new PrintWriter("collatzPeerAmountEffectiveTest.txt", "UTF-8"));

        /*System.out.println("Starting verificationAmountCollatzTest with naive scheduler..");
        verificationAmountCollatzTest(50000000, false, new PrintWriter("collatzVerificationAmountNaiveTest.txt", "UTF-8"));
        System.out.println("Starting verificationAmountCollatzTest with effective scheduler..");
        verificationAmountCollatzTest(50000000, true, new PrintWriter("collatzVerificationAmountEffectiveTest.txt", "UTF-8"));*/

        /*System.out.println("Starting matrixSizeTest..");
        matrixSizeTestResult = matrixSizeTest();
        System.out.println("Starting verificationAmountTest..");
        verificationAmountTestResult = verificationAmountTest();*/

        //System.out.println("Starting collatzTest..");
        //collatzTest();
        //System.out.println("Starting collatzPeerAmountTest..");
        //collatzPeerAmountTest();
        //simpleCollatzTest();
        //simpleEffectiveCollatzTest();
        //simpleEffectiveMatrixTest();

        /*System.out.println("All results:");
        System.out.println("Peer amount test: " + Arrays.toString(peerAmountTestResult));
        System.out.println("Matrix size test: " + Arrays.toString(matrixSizeTestResult));
        System.out.println("Verification amount test: " + Arrays.toString(verificationAmountTestResult));*/
    }

    private static boolean init(String args[]) throws Exception{
        try {
            centralIp = args[0];
            port = 1099;
        } catch (Exception e) {
            System.out.println("The format is expected to be: <Central IP> <Amount of peers : int>"); // <registry port: int>
            return false;
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
        matrices = MatrixGenerator.genrateMatrixSequence(2, 50);
        matrixProblem = new MatrixMultiplicationProblem(matrices, maxPeerAmount);
        scheduler = new NaiveScheduler<int[][]>(central, matrixProblem, 0, whiteList);
        researcher = new StandardResearcher(central, centralPassword, scheduler);

        Naming.rebind("//localhost:" + port + "/Researcher", researcher);
        System.out.println("Researcher bound in registry");
        researcher.joinOnCentral();
        return true;
    }

    private static void encryptedVsNonEncryptedTest(int matrixSize, int matrixAmount, boolean effective, PrintWriter writer) {
        double verificationProbability = 0;
        long firstRun, secondRun;
        matrices = MatrixGenerator.genrateMatrixSequence(matrixSize, matrixAmount);
        matrixProblem = new MatrixMultiplicationProblem(matrices, maxPeerAmount);
        if(effective){
            scheduler = new EffectiveScheduler<int[][]>(central, matrixProblem, verificationProbability, whiteList);
        } else {
            scheduler = new NaiveScheduler<int[][]>(central, matrixProblem, verificationProbability, whiteList);
        }
        researcher.changeScheduler(scheduler);
        firstRun = getEncryptedCalculationTime();
        writeToFile(writer, maxPeerAmount, matrixSize, matrixAmount, verificationProbability, "encrypted", firstRun);
        secondRun = getCalculationTime();
        writeToFile(writer, maxPeerAmount, matrixSize, matrixAmount, verificationProbability, "nonEncrypted", secondRun);
    }

    private static long[] peerAmountTest(int matrixSize, int matrixAmount, boolean effective, PrintWriter writer){
        long[] res = new long[maxPeerAmount-1];
        double verificationProbability = 0;
        long firstRun, secondRun, thirdRun;
        matrices = MatrixGenerator.genrateMatrixSequence(matrixSize, matrixAmount);

        /*long timer = System.currentTimeMillis();
        SoloMatrixMultiplicationThreaded2.startMatrixMultiplication(matrices);
        long solo = System.currentTimeMillis() - timer;
        writeToFile(peerAmountTestWriter, 1, matrixSize, verificationProbability, "solo", solo);*/

        for(int i = 2 ; i <= maxPeerAmount ; i++){
            matrixProblem = new MatrixMultiplicationProblem(matrices, i);
            if(effective){
                scheduler = new EffectiveScheduler<int[][]>(central, matrixProblem, verificationProbability, whiteList);
            } else {
                scheduler = new NaiveScheduler<int[][]>(central, matrixProblem, verificationProbability, whiteList);
            }
            researcher.changeScheduler(scheduler);

            firstRun = getCalculationTime();
            writeToFile(writer, i, matrixSize, matrixAmount, verificationProbability, "first run", firstRun);
            secondRun = getCalculationTime();
            writeToFile(writer, i, matrixSize, matrixAmount, verificationProbability, "second run", secondRun);
            thirdRun = getCalculationTime();
            writeToFile(writer, i, matrixSize, matrixAmount, verificationProbability, "third run", thirdRun);
            res[i-2] = (firstRun + secondRun + thirdRun)/3;
            writeToFile(writer, i, matrixSize, matrixAmount, verificationProbability, "average", res[i-2]);
        }
        return res;
    }

    private static void simpleMatrixTest(int matrixSize, int matrixAmount, boolean effective, PrintWriter writer){
        double verificationProbability = 0;
        long firstRun, secondRun, thirdRun, average;
        matrices = MatrixGenerator.genrateMatrixSequence(matrixSize, matrixAmount);
        matrixProblem = new MatrixMultiplicationProblem(matrices, maxPeerAmount);
        if(effective){
            scheduler = new EffectiveScheduler<int[][]>(central, matrixProblem, verificationProbability, whiteList);
        } else {
            scheduler = new NaiveScheduler<int[][]>(central, matrixProblem, verificationProbability, whiteList);
        }
        researcher.changeScheduler(scheduler);
        firstRun = getCalculationTime();
        writeToFile(writer, maxPeerAmount, matrixSize, matrixAmount, verificationProbability, "first run", firstRun);
        secondRun = getCalculationTime();
        writeToFile(writer, maxPeerAmount, matrixSize, matrixAmount, verificationProbability, "second run", secondRun);
        thirdRun = getCalculationTime();
        writeToFile(writer, maxPeerAmount, matrixSize, matrixAmount, verificationProbability, "third run", thirdRun);
        average = (firstRun + secondRun + thirdRun) / 3;
        writeToFile(writer, maxPeerAmount, matrixSize, matrixAmount, verificationProbability, "average", average);
    }

    private static long[] matrixSizeTest(int matrixAmount, boolean effective, PrintWriter writer){
        int testAmount = 6;
        double verificationProbability = 0;
        long[] res = new long[testAmount];
        long firstRun, secondRun, thirdRun;

        for(int i = 1 ; i <= testAmount ; i++){
            matrices = MatrixGenerator.genrateMatrixSequence(200*i, matrixAmount);
            matrixProblem = new MatrixMultiplicationProblem(matrices, maxPeerAmount);
            if(effective){
                scheduler = new EffectiveScheduler<int[][]>(central, matrixProblem, verificationProbability, whiteList);
            } else {
                scheduler = new NaiveScheduler<int[][]>(central, matrixProblem, verificationProbability, whiteList);
            }
            researcher.changeScheduler(scheduler);

            firstRun = getCalculationTime();
            writeToFile(writer, maxPeerAmount, 200 * i, matrixAmount, verificationProbability, "first run", firstRun);
            secondRun = getCalculationTime();
            writeToFile(writer, maxPeerAmount, 200 * i, matrixAmount, verificationProbability, "second run", secondRun);
            thirdRun = getCalculationTime();
            writeToFile(writer, maxPeerAmount, 200 * i, matrixAmount, verificationProbability, "third run", thirdRun);

            res[i-1] = (firstRun + secondRun + thirdRun)/3;
            writeToFile(writer, maxPeerAmount, 200*i, matrixAmount, verificationProbability, "average", res[i-1]);
        }
        return res;
    }

    private static void encryptedMatrixSizeTest(int matrixAmount, boolean effective, PrintWriter writer){
        int testAmount = 5;
        double verificationProbability = 0;
        long firstRun, firstRunE, secondRun, secondRunE, average, averageE;

        for(int i = 1 ; i <= testAmount ; i++){
            matrices = MatrixGenerator.genrateMatrixSequence(200*i, matrixAmount);
            matrixProblem = new MatrixMultiplicationProblem(matrices, maxPeerAmount);
            if(effective){
                scheduler = new EffectiveScheduler<int[][]>(central, matrixProblem, verificationProbability, whiteList);
            } else {
                scheduler = new NaiveScheduler<int[][]>(central, matrixProblem, verificationProbability, whiteList);
            }
            researcher.changeScheduler(scheduler);

            firstRun = getCalculationTime();
            writeToFile(writer, maxPeerAmount, 200 * i, matrixAmount, verificationProbability, "first run non-encrypted", firstRun);
            firstRunE = getEncryptedCalculationTime();
            writeToFile(writer, maxPeerAmount, 200 * i, matrixAmount, verificationProbability, "first run encrypted", firstRunE);
            secondRun = getCalculationTime();
            writeToFile(writer, maxPeerAmount, 200 * i, matrixAmount, verificationProbability, "second run non-encrypted", secondRun);
            secondRunE = getEncryptedCalculationTime();
            writeToFile(writer, maxPeerAmount, 200 * i, matrixAmount, verificationProbability, "second run encrypted", secondRunE);

            average = (firstRun + secondRun)/2;
            writeToFile(writer, maxPeerAmount, 200*i, matrixAmount, verificationProbability, "average non-encrypted", average);
            averageE = (firstRunE + secondRunE)/2;
            writeToFile(writer, maxPeerAmount, 200*i, matrixAmount, verificationProbability, "average encrypted", averageE);
        }
    }

    private static long[] verificationAmountTest(int matrixSize, int matrixAmount, boolean effective, PrintWriter writer){
        int testAmount = 11;
        double verificationProbability;

        long[] res = new long[testAmount];
        long firstRun, secondRun, thirdRun;

        matrices = MatrixGenerator.genrateMatrixSequence(matrixSize, matrixAmount);
        matrixProblem = new MatrixMultiplicationProblem(matrices, maxPeerAmount);

        for(int i = 0 ; i <= testAmount-1 ; i++){
            verificationProbability = 1-(double)i/(testAmount-1);
            if(effective){
                scheduler = new EffectiveScheduler<int[][]>(central, matrixProblem, verificationProbability, whiteList);
            } else {
                scheduler = new NaiveScheduler<int[][]>(central, matrixProblem, verificationProbability, whiteList);
            }
            researcher.changeScheduler(scheduler);

            firstRun = getCalculationTime();
            writeToFile(writer, maxPeerAmount, matrixSize, matrixAmount, verificationProbability, "first run", firstRun);
            secondRun = getCalculationTime();
            writeToFile(writer, maxPeerAmount, matrixSize, matrixAmount, verificationProbability, "second run", secondRun);
            thirdRun = getCalculationTime();
            writeToFile(writer, maxPeerAmount, matrixSize, matrixAmount, verificationProbability, "third run", thirdRun);

            res[i] = (firstRun + secondRun + thirdRun)/3;
            writeToFile(writer, maxPeerAmount, matrixSize, matrixAmount, verificationProbability, "average", res[i]);
        }
        return res;
    }

    private static long[] verificationAmountCollatzTest(int maxNumber, boolean effective, PrintWriter writer){
        int testAmount = 11;
        double verificationProbability;

        long[] res = new long[testAmount];
        long firstRun, secondRun, thirdRun;

        collatzProblem = new CollatzProblemDefinition(maxPeerAmount, new Interval(1, maxNumber));

        for(int i = 0 ; i <= testAmount-1 ; i++){
            verificationProbability = 1-(double)i/(testAmount-1);
            if(effective){
                scheduler = new EffectiveScheduler<int[][]>(central, collatzProblem, verificationProbability, whiteList);
            } else {
                scheduler = new NaiveScheduler<int[][]>(central, collatzProblem, verificationProbability, whiteList);
            }
            researcher.changeScheduler(scheduler);

            firstRun = getCalculationTime();
            writeToCollatzFile(writer, maxPeerAmount, maxNumber, verificationProbability, "first run", firstRun);
            secondRun = getCalculationTime();
            writeToCollatzFile(writer, maxPeerAmount, maxNumber, verificationProbability, "second run", secondRun);
            thirdRun = getCalculationTime();
            writeToCollatzFile(writer, maxPeerAmount, maxNumber, verificationProbability, "third run", thirdRun);

            res[i] = (firstRun + secondRun + thirdRun)/3;
            writeToCollatzFile(writer, maxPeerAmount, maxNumber, verificationProbability, "average", res[i]);
        }
        return res;
    }

    private static void simpleCollatzTest(){
        double verificationProbability = 0.2;
        collatzProblem = new CollatzProblemDefinition(maxPeerAmount, new Interval(1, 5000000));
        scheduler = new NaiveScheduler<CollatzResult>(central,  collatzProblem, verificationProbability, whiteList);
        researcher.changeScheduler(scheduler);
        long time = getCalculationTime();
        System.out.println("Problem completed in " + time + " ms");
    }

    private static void simpleEffectiveCollatzTest(){
        double verificationProbability = 0.2;
        collatzProblem = new CollatzProblemDefinition(maxPeerAmount, new Interval(1, 5000000));
        scheduler = new EffectiveScheduler<CollatzResult>(central, collatzProblem, verificationProbability, whiteList);
        researcher.changeScheduler(scheduler);
        long time = getCalculationTime();
        System.out.println("Problem completed in " + time + " ms");
    }

    private static long[] collatzNaiveTest(int maxNumber, PrintWriter writer){
        int testAmount = 6;
        double verificationProbability = 0;
        long[] res = new long[testAmount];
        long firstRun, secondRun, thirdRun;

        for(int i = 1 ; i <= testAmount ; i++){
            collatzProblem = new CollatzProblemDefinition(maxPeerAmount, new Interval(1, 5000000*i));
            scheduler = new NaiveScheduler<CollatzResult>(central, collatzProblem, verificationProbability, whiteList);
            researcher.changeScheduler(scheduler);

            firstRun = getCalculationTime();
            writeToCollatzFile(writer, maxPeerAmount, maxNumber, verificationProbability, "first run", firstRun);
            secondRun = getCalculationTime();
            writeToCollatzFile(writer, maxPeerAmount, maxNumber, verificationProbability, "second run", secondRun);
            thirdRun = getCalculationTime();
            writeToCollatzFile(writer, maxPeerAmount, maxNumber, verificationProbability, "third run", thirdRun);

            res[i-1] = (firstRun + secondRun + thirdRun) / 3;

            writeToCollatzFile(writer, maxPeerAmount, maxNumber, verificationProbability, "average", res[i - 1]);
        }
        return res;
    }

    private static long[] collatzPeerAmountTest(int maxNumber, boolean effective, PrintWriter writer){
        long[] res = new long[maxPeerAmount-1];
        double verificationProbability = 0;
        long firstRun, secondRun, thirdRun;

        /*long timer = System.currentTimeMillis();
        SoloMatrixMultiplicationThreaded2.startMatrixMultiplication(matrices);
        long solo = System.currentTimeMillis() - timer;
        writeToFile(peerAmountTestWriter, 1, matrixSize, verificationProbability, "solo", solo);*/

        for(int i = 2 ; i <= maxPeerAmount ; i++){
            collatzProblem = new CollatzProblemDefinition(i, new Interval(1, 50000000));
            if(effective){
                scheduler = new EffectiveScheduler<int[][]>(central, collatzProblem, verificationProbability, whiteList);

            } else {
                scheduler = new NaiveScheduler<int[][]>(central, collatzProblem, verificationProbability, whiteList);
            }
            researcher.changeScheduler(scheduler);

            firstRun = getCalculationTime();
            writeToCollatzFile(writer, i, maxNumber, verificationProbability, "first run", firstRun);
            secondRun = getCalculationTime();
            writeToCollatzFile(writer, i, maxNumber, verificationProbability, "second run", secondRun);
            thirdRun = getCalculationTime();
            writeToCollatzFile(writer, i, maxNumber, verificationProbability, "third run", thirdRun);
            res[i-2] = (firstRun + secondRun + thirdRun)/3;
            writeToCollatzFile(writer, i, maxNumber, verificationProbability, "average", res[i - 2]);
        }
        return res;
    }

    private static void writeToFile(PrintWriter writer, int peerAmount, int matrixSize, int matrixAmount, double verificationProbability, String run, long result){
        writer.println(peerAmount + " peers: " + scheduler.getPeers() + ", " + matrixSize + "x" + matrixSize + "x" + matrixAmount + ", " + verificationProbability + " verification, " + run + ": " + result);
        writer.flush();
    }

    private static void writeToCollatzFile(PrintWriter writer, int peerAmount, int maxNumber, double verificationProbability, String run, long result){
        writer.println(peerAmount + " peers: " + scheduler.getPeers() + ", 1-" + maxNumber + ", " + verificationProbability + " verification, " + run + ": " + result);
        writer.flush();
    }

    private static long getCalculationTime(){
        System.out.println("Starting new calculation");
        long timer;
        timer = System.currentTimeMillis();
        researcher.startCalculation();
        return System.currentTimeMillis() - timer;
    }

    private static long getEncryptedCalculationTime(){
        System.out.println("Starting new calculation");
        long timer;
        timer = System.currentTimeMillis();
        researcher.startEncryptedCalculation();
        return System.currentTimeMillis() - timer;
    }

    private static String getMyHostName(){
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return "";
    }
}
