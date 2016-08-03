package Utility;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by thoma_000 on 18-05-2015.
 */
public class MatrixMultiplicationProblem implements SchedulingProblem<int[][]> {
    private int amountOfNeededPeers;
    private LinkedBlockingQueue<Task<int[][]>> taskQueue = new LinkedBlockingQueue<>();
    private ArrayList<Interval> intervalList = new ArrayList<>();
    private Map<Interval, int[][]> intervalMap = new HashMap<>();
    private int[][] result;
    private int[][][] matrices;


    public MatrixMultiplicationProblem(int[][][] matrices, int amountOfNeededPeers){
        this.amountOfNeededPeers = amountOfNeededPeers;
        this.matrices = matrices;

    }

    public void init(){
        reset();
        Interval interval;
        for(int i = 0; i < matrices.length; i++) {
            interval = new Interval(i, i);
            intervalMap.put(interval, matrices[i]);
            intervalList.add(interval);
        }


        Collections.sort(intervalList);
        while(intervalList.size() > 1){
            Interval interval1 = intervalList.get(0);
            intervalList.remove(0);
            Interval interval2 = intervalList.get(0);
            intervalList.remove(0);
            try {
                taskQueue.put(new MatrixMultiplicationTaskWithIntervals(intervalMap.get(interval1), intervalMap.get(interval2), interval1, interval2));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public int getAmountOfNeededPeers() {
        return amountOfNeededPeers;
    }

    @Override
    public LinkedBlockingQueue<Task<int[][]>> getAvailableTasks() {
        return taskQueue;
    }

    @Override
    public void handleTaskResult(Task<int[][]> task, int[][] result) {
        MatrixMultiplicationTaskWithIntervals matrixTask = (MatrixMultiplicationTaskWithIntervals) task;
        intervalMap.remove(matrixTask.getFirstInterval());
        intervalMap.remove(matrixTask.getSecondInterval());
        Interval newInterval = new Interval(matrixTask.getFirstInterval().getLow(), matrixTask.getSecondInterval().getHigh());
        intervalMap.put(newInterval, result);
        intervalList.add(newInterval);
        Collections.sort(intervalList);

        System.out.println("Task completed - " + newInterval);
        System.out.println("intervalMap size: " + intervalMap.size() + ". IntervalList size: " + intervalList.size());
        System.out.println("taskQueue size: " + taskQueue.size());

        if(intervalMap.size() == 1) {
            this.result = result;
            //MatrixGenerator.printMatrix(result);
            System.out.println("Problem complete, result: " + (result != null));
            return;
        }

        while(intervalList.size() > 1) {
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

            try {
                taskQueue.put(new MatrixMultiplicationTaskWithIntervals(intervalMap.get(prev), intervalMap.get(current), prev, current));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            intervalList.remove(prev);
            intervalList.remove(current);
        }
    }

    @Override
    public boolean isDone() {
        return result != null;
    }

    @Override
    public boolean compare(int[][] left, int[][] right) {
        for(int i = 0 ; i<left.length ; i++){
            if(!Arrays.equals(left[i], right[i])){
                return false;
            }
        }
        return true;
    }

    @Override
    public void reset() {
        result = null;
        intervalList = new ArrayList<>();
        intervalMap = new HashMap<>();
        taskQueue = new LinkedBlockingQueue<>();

        /*
        intervalList.clear();
        intervalMap.clear();
        taskQueue.clear();
        */
    }
}
