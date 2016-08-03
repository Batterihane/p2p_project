package EffectiveScheduling;

import Central.Central;
import Central.PeerInfo;
import Peer.Peer;
import Utility.*;

import java.rmi.RemoteException;
import java.security.KeyPair;
import java.util.*;
import java.util.concurrent.*;

/**
 * Created by JJ on 20-05-2015.
 */
public class EffectiveScheduler<T> implements Scheduler {
    private Central central;
    private SchedulingProblem<T> schedulingProblem;
    private List<PeerInfo> peers;
    private Set<String> whiteListedPeersId;
    private PeerCommunicator peerCommunicator;
    private double verificationProbability;
    private LinkedBlockingQueue<Task<T>> taskQueue;
    private ExecutorService executor;

    private ConcurrentHashMap<String, Peer> whiteListedPeersMap;
    private ConcurrentHashMap<Peer, PeerInfo> peerToInfoMap;
    private HashMap<Task<T>, EffectiveResultPair<T>> tasksForVerification;
    private LinkedBlockingQueue<Task<T>> verificationQueue;
    private Map<Peer, List<Long>> peerSpeedMap = new HashMap<>();

    private LinkedBlockingQueue<EffectiveTaskRunner<T>> completedTasks;
    private PriorityBlockingQueue<PeerProperty> availablePeers;
    private Set<Task<T>> runningTasks;
    private Map<EffectiveTaskRunner<T>, Long> estimatedTimeForCompletion;

    public EffectiveScheduler(Central central, SchedulingProblem<T> schedulingProblem, double verificationProbability, Set<String> whiteListedPeersId) {
        if(verificationProbability < 0){
            verificationProbability = 0;
        } else if(verificationProbability > 1){
            verificationProbability = 1;
        }

        this.central = central;
        this.schedulingProblem = schedulingProblem;
        this.whiteListedPeersId = whiteListedPeersId;
        this.verificationProbability = verificationProbability;
    }

    private void reset(){
        if(executor != null){
            try {
                executor.shutdownNow();
                executor.awaitTermination(1000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e){
                e.printStackTrace();
            }
        }
    }

    private void init(){
        whiteListedPeersMap = new ConcurrentHashMap<>();
        peerToInfoMap = new ConcurrentHashMap<>();
        tasksForVerification = new HashMap<>();
        verificationQueue = new LinkedBlockingQueue<>();
        completedTasks = new LinkedBlockingQueue<>();
        availablePeers = new PriorityBlockingQueue<PeerProperty>();
        runningTasks = new HashSet();
        estimatedTimeForCompletion = new ConcurrentHashMap<>();
        taskQueue = schedulingProblem.getAvailableTasks();
        executor = Executors.newFixedThreadPool(30);

        try {
            peers = central.getPeersAmount(schedulingProblem.getAmountOfNeededPeers());
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        System.out.println("peerlist size: " + peers.size());
        System.out.println("peerlist: " + peers);

    }

    @Override
    public void start() {
        reset();
        schedulingProblem.init();
        init();
        if(peerCommunicator == null){
            peerCommunicator = new StandardPeerCommunicator();
        }
        peerCommunicator.init(peers);
        for(PeerInfo p : peers){
            executor.execute(new PeerInitiatorRunner(p,availablePeers, whiteListedPeersMap, whiteListedPeersId.contains(p.getId()), peerToInfoMap, peerSpeedMap));
        }

        EffectiveTaskRunner<T> completedTaskRunner;

        while(!schedulingProblem.isDone() || !verificationQueue.isEmpty() ){
            //try {

                //Thread.currentThread().sleep(1);

                while(!completedTasks.isEmpty()){
                    completedTaskRunner =  completedTasks.poll();
                    if(completedTaskRunner.isCompletedSuccessfully()){
                        if(!handleAndVerifyTaskResult(completedTaskRunner)){
                            return;  // A new problem calculation has been started, so we stop the old calculation
                        }
                    } else { //We consider peer dead! TODO Report to central?
                        taskQueue.add(completedTaskRunner.getTask());
                        askCentralForAnotherPeer();
                    }
                    if(estimatedTimeForCompletion.containsKey(completedTaskRunner)){
                        estimatedTimeForCompletion.remove(completedTaskRunner);
                    }
                }
                if(!verificationQueue.isEmpty() && !availablePeers.isEmpty()){
                    System.out.println("Starting verification of task..");
                    Task<T> task = verificationQueue.poll();
                    Peer bestPeer = getBestAvailablePeer();
                    if(tasksForVerification.get(task).getPeer() != bestPeer){
                        EffectiveTaskRunner<T> taskRunner = new EffectiveTaskRunner<T>(task, bestPeer, completedTasks);
                        executor.execute(taskRunner);
                        if(peerSpeedMap.containsKey(bestPeer)){
                            estimatedTimeForCompletion.put(taskRunner, System.currentTimeMillis() + averageList(peerSpeedMap.get(bestPeer)));
                        } else {
                            long estimatedTime = 11000 - peerToInfoMap.get(bestPeer).getCapacity()*100;
                            estimatedTimeForCompletion.put(taskRunner, System.currentTimeMillis() + estimatedTime);
                        }
                    } else {
                        if(!availablePeers.isEmpty()) {
                            Peer secondBestPeer = getBestAvailablePeer();
                            EffectiveTaskRunner<T> taskRunner = new EffectiveTaskRunner<T>(task, secondBestPeer, completedTasks);
                            executor.execute(taskRunner);
                            if(peerSpeedMap.containsKey(secondBestPeer)){
                                estimatedTimeForCompletion.put(taskRunner, System.currentTimeMillis() + averageList(peerSpeedMap.get(secondBestPeer)));
                            } else {
                                long estimatedTime = 11000 - peerToInfoMap.get(secondBestPeer).getCapacity()*100;
                                estimatedTimeForCompletion.put(taskRunner, System.currentTimeMillis() + estimatedTime);
                            }
                        }
                        availablePeers.add(getPeerProperty(bestPeer));
                    }
                }

                if(!taskQueue.isEmpty() && !availablePeers.isEmpty()){
                    Peer bestPeer = getBestAvailablePeer();
                    Task<T> task = taskQueue.poll();
                    EffectiveTaskRunner<T> taskRunner = new EffectiveTaskRunner<T>(task,bestPeer, completedTasks);
                    duplicateByChance(taskRunner);
                    System.out.println("Sending task to peer");
                    executor.execute(taskRunner);
                    runningTasks.add(task);
                    if(peerSpeedMap.containsKey(bestPeer)){
                        estimatedTimeForCompletion.put(taskRunner, System.currentTimeMillis() + averageList(peerSpeedMap.get(bestPeer)));
                    } else {
                        long estimatedTime = 11 - peerToInfoMap.get(bestPeer).getCapacity();
                        estimatedTimeForCompletion.put(taskRunner, System.currentTimeMillis() + estimatedTime);
                    }

                }

                if(taskQueue.isEmpty() && verificationQueue.isEmpty() && !availablePeers.isEmpty() && !estimatedTimeForCompletion.isEmpty()){
                    long estimatedCalculationTimeOfBestPeer = averageList(peerSpeedMap.get(availablePeers.peek().getPeer()));

                    Map.Entry<EffectiveTaskRunner<T>, Long> latestEstimatedCompletion = estimatedTimeForCompletion.entrySet().stream()
                            .max((o1, o2) -> Long.compare(o1.getValue(),o2.getValue()))
                            .get();

                    if(System.currentTimeMillis() + estimatedCalculationTimeOfBestPeer + 2000 < latestEstimatedCompletion.getValue()){
                        System.out.println("Found better peer for task. Sending task to new peer");
                        Peer bestPeer = availablePeers.poll().getPeer();
                        EffectiveTaskRunner<T> taskRunner = new EffectiveTaskRunner<T>(latestEstimatedCompletion.getKey().getTask(), bestPeer, completedTasks);
                        executor.execute(taskRunner);
                        estimatedTimeForCompletion.put(taskRunner, System.currentTimeMillis() + averageList(peerSpeedMap.get(taskRunner.getTargetPeer())));
                        estimatedTimeForCompletion.remove(latestEstimatedCompletion.getKey());
                        continue;
                    }

                    Map.Entry<EffectiveTaskRunner<T>, Long> firstEstimatedCompletion = estimatedTimeForCompletion.entrySet().stream()
                            .min((o1, o2) -> Long.compare(o1.getValue(), o2.getValue()))
                            .get();

                    if(System.currentTimeMillis() > firstEstimatedCompletion.getValue()  + ((double)estimatedCalculationTimeOfBestPeer)*0.1 + 2000){
                        System.out.println("Peer didn't finish task in estimated time. Sending task to new peer");
                        Peer bestPeer = availablePeers.poll().getPeer();
                        EffectiveTaskRunner<T> taskRunner = new EffectiveTaskRunner<T>(firstEstimatedCompletion.getKey().getTask(), bestPeer, completedTasks);
                        executor.execute(taskRunner);
                        estimatedTimeForCompletion.put(taskRunner, System.currentTimeMillis() + estimatedCalculationTimeOfBestPeer);
                        estimatedTimeForCompletion.remove(firstEstimatedCompletion.getKey());
                        continue;
                    }

                }

                /*
            } catch (InterruptedException e) {
                e.printStackTrace();
            }*/

        }
        System.out.println("I completed the problem");
        reset();
    }

    private PeerProperty getPeerProperty(Peer peer){
        return new PeerProperty(averageList(peerSpeedMap.get(peer)),peer);
    }

    private void duplicateByChance(EffectiveTaskRunner taskRunner){
        Task<T> task = taskRunner.getTask();
        if(!whiteListedPeersMap.containsKey(taskRunner.getTargetPeer()) && !tasksForVerification.containsKey(task) && Math.random() <= verificationProbability){
            tasksForVerification.put(task, new EffectiveResultPair<T>(null, null));
            verificationQueue.add(task);
        }
    }

    private Peer getBestAvailablePeer(){ //TODO optimize
        return availablePeers.poll().getPeer();
    }

    //Returns false if we need to restart.
    private boolean handleAndVerifyTaskResult(EffectiveTaskRunner<T> taskRunner) {
        System.out.println("Duplication queue size: " + verificationQueue.size());
        Task<T> task = taskRunner.getTask();

        if(!tasksForVerification.containsKey(task)) {
            if(runningTasks.contains(task)){
                giveSchedulerResult(taskRunner);
                runningTasks.remove(task);
            }
            releasePeerAndUpdateSpeed(taskRunner);
            return true;
        }

        if(tasksForVerification.get(task).getResult() == null){
            tasksForVerification.put(task, new EffectiveResultPair<T>(taskRunner.getResult(),taskRunner.getTargetPeer()));
            giveSchedulerResult(taskRunner);
            releasePeerAndUpdateSpeed(taskRunner);
            return true;
        } else {
            if(schedulingProblem.compare(tasksForVerification.get(task).getResult(), taskRunner.getResult())) {
                releasePeerAndUpdateSpeed(taskRunner);
                tasksForVerification.remove(task);
                runningTasks.remove(task);
                return true;
            } else { //Somebody has fucked up / been a bad boy
                findAndReportEvilPeer(taskRunner);
                return false;
            }
        }
    }

    private void findAndReportEvilPeer(EffectiveTaskRunner<T> taskRunner){
        T correctResult = taskRunner.getTask().execute();
        String evilPeer;
        if(!schedulingProblem.compare(correctResult, taskRunner.getResult())){
            evilPeer = peerToInfoMap.get(taskRunner.getTargetPeer()).getId();
        } else{
            evilPeer = peerToInfoMap.get(tasksForVerification.get(taskRunner.getTask()).getPeer()).getId();
        }
        try {
            central.repportPeer(evilPeer);
        } catch (RemoteException e) {
            System.out.println("FAILED REPORTING EVIL PEER TO CENTRAL!");
        }
        schedulingProblem.reset();
        start();
    }

    private void giveSchedulerResult(EffectiveTaskRunner<T> taskRunner){
        schedulingProblem.handleTaskResult(taskRunner.getTask(), taskRunner.getResult());
    }

    private void releasePeerAndUpdateSpeed(EffectiveTaskRunner<T> taskRunner){
        Peer peer = taskRunner.getTargetPeer();
        List<Long> runningTimes = peerSpeedMap.getOrDefault(peer, new ArrayList<>());
        runningTimes.add(taskRunner.getTimeForCompletionInMilliSeconds());
        peerSpeedMap.put(peer, runningTimes);
        availablePeers.add(getPeerProperty(peer));
    }

    private Long averageList(List<Long> list){
        return (long)list.stream().mapToLong(l -> l).average().getAsDouble();
    }

    private void askCentralForAnotherPeer(){
        //TODO START THREAD ASKING CENTRAL FOR ANOTHER PEER!
    }

    @Override
    public void enableEncryption(KeyPair keyPair) {
        peerCommunicator = new EncryptedPeerCommunicator(keyPair);
    }

    @Override
    public void disableEncryption() {
        peerCommunicator = new StandardPeerCommunicator();
    }

    public List<PeerInfo> getPeers(){
        return peers;
    }
}
