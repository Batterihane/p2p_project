package Utility;

import Central.Central;
import Central.PeerInfo;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by JJ on 07-05-2015.
 */
public class NaiveMatrixMultiplicationScheduler implements ProblemSpecificScheduler{
    private static ExecutorService executor = Executors.newFixedThreadPool(30);
    private int[][][] arrayOfMatrices;
    private Central central;
    private List<PeerInfo> peerList;
    private int amountOfPeersUsed;

    private ArrayList<Interval> intervalList = new ArrayList<>();
    private Map<Interval, int[][]> intervalMap = new HashMap<>();
    private LinkedBlockingQueue<Task<int[][]>> taskQueue = new LinkedBlockingQueue<>();
    private LinkedBlockingQueue<TaskRunner> completedTasks = new LinkedBlockingQueue<>();
    private Map<Task<int[][]>, IntervalPair> intervalPairMap = new HashMap<>();

    public NaiveMatrixMultiplicationScheduler(Central central, int[][][] arrayOfMatrices, int amountOfPeersUsed){
        this.arrayOfMatrices = arrayOfMatrices;
        this.central = central;
        this.amountOfPeersUsed = amountOfPeersUsed;
    }

    private void init() throws RemoteException, InterruptedException, MalformedURLException, NotBoundException {

        if(amountOfPeersUsed < 1)
            peerList = central.getPeersCapacity(arrayOfMatrices.length * 2);
        else
            peerList = central.getPeersAmount(amountOfPeersUsed);

        Interval interval;
        for(int i = 0; i < arrayOfMatrices.length; i++) {
            interval = new Interval(i, i);
            intervalMap.put(interval, arrayOfMatrices[i]);
            intervalList.add(interval);
        }


        Collections.sort(intervalList);
        while(intervalList.size() > 1){
            Interval interval1 = intervalList.get(0);
            intervalList.remove(0);
            Interval interval2 = intervalList.get(0);
            intervalList.remove(0);
            taskQueue.put(new MatrixMultiplicationTaskWithIntervals(intervalMap.get(interval1), intervalMap.get(interval2), interval1, interval2));
        }

        System.out.println("peerlist size: " + peerList.size());
        System.out.println("peerlist: " + peerList);

        System.out.println("task queue size: " + taskQueue.size());
        System.out.println("interval queue size: " + intervalList.size());


        for (PeerInfo p : peerList){
            executor.execute(new TaskRunner(taskQueue, completedTasks, p));
        }
    }

    public void start() {
        try {
            init();
            Interval newInterval = new Interval(0, 0);
            while (intervalMap.size() > 1) {
                System.out.println("I'm out! intervalMap size " + intervalMap.size());
                TaskRunner<int[][]> taskRunner = completedTasks.take();
                MatrixMultiplicationTaskWithIntervals task = (MatrixMultiplicationTaskWithIntervals) taskRunner.getTask();
                intervalMap.remove(task.getFirstInterval());
                intervalMap.remove(task.getSecondInterval());
                newInterval = new Interval(task.getFirstInterval().getLow(), task.getSecondInterval().getHigh());
                System.out.println("Task completed - " + newInterval);
                intervalMap.put(newInterval, taskRunner.getResult());
                intervalList.add(newInterval);
                Collections.sort(intervalList);


                System.out.println("IntervalQueue: " + intervalList);
                System.out.println("TaskQueue: " + taskQueue);


                if(intervalList.size() > 1) {

                    while(true){
                        Interval prev = null;
                        Interval current = null;
                        for(Interval in : intervalList){
                            if(prev != null && prev.getHigh() + 1 == in.getLow()){
                                current = in;
                                break;
                            }
                            prev = in;
                        }

                        if(current == null)
                            break;

                        taskQueue.put(new MatrixMultiplicationTaskWithIntervals(intervalMap.get(prev), intervalMap.get(current), prev, current));

                        intervalList.remove(prev);
                        intervalList.remove(current);

                        current = null;
                    }

                }

                if(taskQueue.size() > 0) {
                    executor.execute(taskRunner);
                }

            }

            //MatrixGenerator.printMatrix(intervalMap.get(newInterval));

        } catch (Exception e){
            e.printStackTrace();
        }

    }

}
