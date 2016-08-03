package Utility;

import Central.PeerInfo;
import Peer.Peer;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by JJ on 07-05-2015.
 */
public class TaskRunner<T> implements Runnable {
    private LinkedBlockingQueue<Task<T>> taskQueue;
    private LinkedBlockingQueue<TaskRunner<T>> resultQueue;
    private Task<T> task;
    private T result;
    private Peer targetPeer;
    private PeerInfo peerInfo;
    private double verificationProbability;
    private ConcurrentHashMap<Task<T>, ResultPair<T>> duplicateMap;
    private LinkedBlockingQueue<Task<T>> duplicateQueue;
    private PeerCommunicator peerCommunicator;

    public TaskRunner(LinkedBlockingQueue<Task<T>> taskQueue, LinkedBlockingQueue<TaskRunner<T>> resultQueue, PeerInfo targetPeerInfo, double verificationProbability, ConcurrentHashMap<Task<T>, ResultPair<T>> duplicateMap, LinkedBlockingQueue<Task<T>> duplicateQueue, PeerCommunicator peerCommunicator) throws Exception{
        this.taskQueue = taskQueue;
        this.resultQueue = resultQueue;
        peerInfo = targetPeerInfo;
        this.verificationProbability = verificationProbability;
        this.duplicateMap = duplicateMap;
        this.duplicateQueue = duplicateQueue;
        this.peerCommunicator = peerCommunicator; // new

        targetPeer = (Peer) Naming.lookup("//" + targetPeerInfo.getId() + ":1099" + "/Peer");
    }

    @Override
    public void run() {
        if(Thread.currentThread().isInterrupted()){
            return;
        }
        result = null;
        task = null;
        try {
            try {
                task = duplicateQueue.poll();
                if(task != null) {
                    if (!duplicateMap.get(task).getPeerIp().equals(peerInfo.getId())){
                        System.out.println("Pushing duplication task to peer: " + peerInfo.getId());
                        result = peerCommunicator.executeTaskOnPeer(targetPeer, task);  // new
                        if (result == null) {
                            System.out.println("Peer " + peerInfo.getId() + " returned null. TaskRunner stopped");
                            duplicateQueue.put(task);
                            return;
                        }
                        resultQueue.put(this);
                        return;
                    }else{
                        duplicateQueue.put(task);
                    }
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }


            try {
                task = taskQueue.poll(1000, TimeUnit.MILLISECONDS);
                if(task == null){
                    run(); //Start over - check for duplications.
                    return;
                }
                System.out.println("Pushing task to peer: " + peerInfo.getId());
                result = targetPeer.executeTask(task);
                if(result == null){
                    System.out.println("Peer " + peerInfo.getId() + " returned null. TaskRunner stopped");
                    taskQueue.put(task);
                    return;
                }
                resultQueue.put(this);
            } catch (RemoteException e) {
                taskQueue.put(task);
                System.out.println("Peer with ip " + peerInfo.getId() + " died.");
                e.printStackTrace();
            }
        } catch (InterruptedException e){
            System.out.println("I'm a task runner and i got interrupted!");
            //e.printStackTrace();
        }
    }

    public T getResult(){
        return result;
    }

    public PeerInfo getPeerInfo() { return peerInfo; }

    public Task<T> getTask() {
        return task;
    }
}
