package Utility;

import Central.Central;
import Central.PeerInfo;

import java.rmi.RemoteException;
import java.security.KeyPair;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

/**
 * Created by thoma_000 on 18-05-2015.
 */
public class NaiveScheduler<T> implements Scheduler {
    private ExecutorService executor;
    private Central central;
    private SchedulingProblem schedulingProblem;
    private List<PeerInfo> peers;
    private LinkedBlockingQueue<Task<T>> taskQueue;
    private LinkedBlockingQueue<TaskRunner<T>> completedTasks;
    private ConcurrentHashMap<Task<T>, ResultPair> tasksForVerification;
    private LinkedBlockingQueue<Task<T>> duplicateQueue;
    private double verificationProbability;
    private int duplicateCounter = 0;
    private int duplicateQueuedCounter = 0;
    private int completedTasksCounter = 0;
    private Set<String> whiteListedPeers;
    private PeerCommunicator peerCommunicator;

    public NaiveScheduler(Central central, SchedulingProblem<T> schedulingProblem, double verificationProbability, Set<String> whiteListedPeers){
        if(verificationProbability < 0){
            verificationProbability = 0;
        } else if(verificationProbability > 1){
            verificationProbability = 1;
        }
        this.central = central;
        this.schedulingProblem = schedulingProblem;
        this.verificationProbability = verificationProbability;

        this.whiteListedPeers = whiteListedPeers;
    }

    private void reset(){
        if(executor != null){
            try {
                executor.shutdownNow();
                executor.awaitTermination(10000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e){
                e.printStackTrace();
            }
        }
    }

    private void init(){
        tasksForVerification = new ConcurrentHashMap<>();
        duplicateQueue = new LinkedBlockingQueue<>();
        completedTasks = new LinkedBlockingQueue<>();
        duplicateCounter = 0;
        duplicateQueuedCounter = 0;
        completedTasksCounter = 0;


        executor = Executors.newFixedThreadPool(30);
        taskQueue = schedulingProblem.getAvailableTasks();

        try {
            peers = central.getPeersAmount(schedulingProblem.getAmountOfNeededPeers());
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        System.out.println("peerlist size: " + peers.size());
        System.out.println("peerlist: " + peers);

        System.out.println("task queue size: " + taskQueue.size());
        System.out.println("Task queue: " + taskQueue);
    }

    @Override
    public void start() {
        reset();
        schedulingProblem.init();
        init();
        if(peerCommunicator == null){
            peerCommunicator = new StandardPeerCommunicator(); // new
        }
        peerCommunicator.init(peers);
        TaskRunner<T> taskRunner;
        for(PeerInfo p : peers){
            try {
                taskRunner = new TaskRunner(taskQueue, completedTasks, p, verificationProbability, tasksForVerification, duplicateQueue, peerCommunicator);
            } catch (Exception e) {
                System.out.println("Failed creating taskRunner for peer " + p.getId());
                continue;
            }
            executor.execute(taskRunner);
        }

        TaskRunner<T> completedTaskRunner;
        while(!schedulingProblem.isDone() || !duplicateQueue.isEmpty()){
            System.out.println("isDone: " + schedulingProblem.isDone() + ", duplicateQueue empty: " + duplicateQueue.isEmpty());
            try {
                completedTaskRunner = completedTasks.take();
                System.out.println("Peer " + completedTaskRunner.getPeerInfo().getId() + " completed a task.");
                if(!verifyAndHandleResult(completedTaskRunner)){
                    T correctResult = completedTaskRunner.getTask().execute();
                    String evilPeer;
                    if(!schedulingProblem.compare(correctResult, completedTaskRunner.getResult())){
                        evilPeer = completedTaskRunner.getPeerInfo().getId();
                    } else{
                        evilPeer = tasksForVerification.get(completedTaskRunner.getTask()).getPeerIp();
                    }
                    central.repportPeer(evilPeer);
                    executor.shutdownNow();
                    executor.awaitTermination(10000, TimeUnit.MILLISECONDS);


                    schedulingProblem.reset();
                    start();
                    return;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        System.out.println("I've verified " + duplicateCounter + " tasks. Verification queue size: " + duplicateQueue.size());
        System.out.println("I've queued " + duplicateQueuedCounter + " duplication tasks.");
        System.out.println("I've completed " + completedTasksCounter + " tasks");
        reset();
    }

    @Override
    public void enableEncryption(KeyPair keyPair) {                         // new
        peerCommunicator = new EncryptedPeerCommunicator(keyPair);
    }

    @Override
    public void disableEncryption() {
        peerCommunicator = new StandardPeerCommunicator();
    }

    private boolean verifyAndHandleResult(TaskRunner<T> completedTaskRunner){
        System.out.println("Duplication queue size: " + duplicateQueue.size());
        completedTasksCounter++;
        Task<T> completedTask = completedTaskRunner.getTask();
        if(tasksForVerification.containsKey(completedTask)){
            duplicateCounter++;
            ResultPair verificationResultPair = tasksForVerification.get(completedTask);
            T taskResult = completedTaskRunner.getResult();

            if(!schedulingProblem.compare(verificationResultPair.getResult(), taskResult)){
                return false;
            }
            executor.execute(completedTaskRunner);
            tasksForVerification.remove(completedTask);
            return true; //result has already been handled
        } else {
            if(!whiteListedPeers.contains(completedTaskRunner.getPeerInfo().getId()) && Math.random() <= verificationProbability){ //
                tasksForVerification.put(completedTask, new ResultPair<T>(completedTaskRunner.getResult(), completedTaskRunner.getPeerInfo().getId()));
                duplicateQueue.add(completedTask);
                duplicateQueuedCounter++;
            }
        }
        schedulingProblem.handleTaskResult(completedTask, completedTaskRunner.getResult());
        executor.execute(completedTaskRunner);
        return true;
    }

    public List<PeerInfo> getPeers(){
        return peers;
    }
}
