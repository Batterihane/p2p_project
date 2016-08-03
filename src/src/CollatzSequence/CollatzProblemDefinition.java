package CollatzSequence;

import Utility.Interval;
import Utility.SchedulingProblem;
import Utility.Task;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by JJ on 18-05-2015.
 */
public class CollatzProblemDefinition implements SchedulingProblem<CollatzResult> {
    private int amountOfNeededPeers;
    private Interval searchInterval;
    private LinkedBlockingQueue<Task<CollatzResult>> taskQueue = new LinkedBlockingQueue<>();
    private long largestResultSoFar;
    private long largestNumberSoFar;
    private int taskSize = 1000000;
    private final int minimumTaskSize = 500000;
    private long remainingIntervalSize;
    private final int taskSizeReduction = 20000;

    public CollatzProblemDefinition(int amountOfNeededPeers, Interval collatzIntervalInclusive) {
        this.amountOfNeededPeers = amountOfNeededPeers;
        searchInterval = collatzIntervalInclusive;
    }



    @Override
    public void init() {
        reset();
        System.out.println("Problem definition has the interval: " + searchInterval);
        remainingIntervalSize = searchInterval.getHigh() - searchInterval.getLow();

        try {
            for(long i = searchInterval.getLow(); i <= searchInterval.getHigh(); ){
                //System.out.println("I in the init loop: " + i);
                if(i + taskSize < searchInterval.getHigh()){
                    taskQueue.put(new CollatzTask(new Interval(i, (i + taskSize))));
                    i += taskSize;
                    if (taskSize > minimumTaskSize)
                        taskSize -= taskSizeReduction;

                } else {
                    taskQueue.put(new CollatzTask(new Interval(i, searchInterval.getHigh())));
                    break;
                }
            }
        } catch (InterruptedException e) {      e.printStackTrace();       }
    }

    @Override
    public int getAmountOfNeededPeers() {
        return amountOfNeededPeers;
    }

    @Override
    public LinkedBlockingQueue<Task<CollatzResult>> getAvailableTasks() {
        return taskQueue;
    }

    @Override
    public void handleTaskResult(Task<CollatzResult> task, CollatzResult result) {
        CollatzTask intervalTask = (CollatzTask) task;
        long intervalResult = result.getLength();

        if (largestResultSoFar < intervalResult) {
            largestResultSoFar = intervalResult;
            largestNumberSoFar = result.getNumber();
        }

        remainingIntervalSize -= intervalTask.numberInterval.getHigh() - intervalTask.numberInterval.getLow();
        if(remainingIntervalSize == 0){
            System.out.println("Problem solved: The number " + largestNumberSoFar + " gave the largest sequence of length " + largestResultSoFar);
        }
    }

    @Override
    public boolean isDone() {
        return remainingIntervalSize == 0;
    }

    @Override
    public boolean compare(CollatzResult left, CollatzResult right) {
        return left.getLength() == right.getLength() && left.getNumber() == right.getNumber();
    }

    @Override
    public void reset() {
        taskQueue = new LinkedBlockingQueue<>();
        largestNumberSoFar = 0;
        largestResultSoFar = 0;
        taskSize = 500000;
        remainingIntervalSize = searchInterval.getHigh() - searchInterval.getLow();
    }
}
