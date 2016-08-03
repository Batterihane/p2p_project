package EffectiveScheduling;

import Peer.Peer;
import Utility.Task;

import java.rmi.RemoteException;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by JJ on 20-05-2015.
 */
public class EffectiveTaskRunner<T> implements Runnable {
    private LinkedBlockingQueue<EffectiveTaskRunner<T>> resultQueue;
    private Task<T> task;
    private T result;
    private Peer targetPeer;
    private boolean completedSuccessfully = false;
    private long timeForCompletionInMilliSeconds;

    public EffectiveTaskRunner(Task<T> task, Peer targetPeer, LinkedBlockingQueue<EffectiveTaskRunner<T>> resultQueue) {
        this.task = task;
        this.targetPeer = targetPeer;
        this.resultQueue = resultQueue;
    }

    @Override
    public void run() {
        if(Thread.currentThread().isInterrupted()){
            return;
        }
        try {
            timeForCompletionInMilliSeconds = System.currentTimeMillis();
            result = targetPeer.executeTask(task);
            completedSuccessfully = true;
        } catch (RemoteException e) {
            System.out.println("Peer failed responding.");
        }
        timeForCompletionInMilliSeconds =  System.currentTimeMillis() - timeForCompletionInMilliSeconds;
        resultQueue.add(this);
    }

    public boolean isCompletedSuccessfully() {
        return completedSuccessfully;
    }

    public long getTimeForCompletionInMilliSeconds() {
        return timeForCompletionInMilliSeconds;
    }

    public T getResult() {
        return result;
    }

    public Task<T> getTask() {
        return task;
    }

    public Peer getTargetPeer() {
        return targetPeer;
    }
}
