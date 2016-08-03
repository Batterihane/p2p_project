package Utility;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by thoma_000 on 18-05-2015.
 */
public interface SchedulingProblem<T> {

    public void init();
    public int getAmountOfNeededPeers();
    public LinkedBlockingQueue<Task<T>> getAvailableTasks();
    public void handleTaskResult(Task<T> task, T result);
    public boolean isDone();
    public boolean compare(T left, T right);
    public void reset();

}
