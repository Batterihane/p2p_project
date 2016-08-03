package EffectiveScheduling;

import Central.*;
import Peer.Peer;
import Utility.*;

import java.rmi.RemoteException;
import java.security.KeyPair;
import java.util.*;
import java.util.concurrent.*;

/**
 * Created by JJ on 20-05-2015.
 */
public class OldEffectiveScheduler<T> implements Scheduler {
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
    private ConcurrentHashMap<Task<T>, EffectiveResultPair<T>> tasksForVerification;
    private LinkedBlockingQueue<Task<T>> verificationQueue;
    private Map<Peer, List<Long>> peerSpeedMap = new HashMap<>();

    private LinkedBlockingQueue<EffectiveTaskRunner<T>> completedTasks;
    private PriorityBlockingQueue<PeerProperty> availablePeers;

    public OldEffectiveScheduler(Central central, SchedulingProblem<T> schedulingProblem, double verificationProbability, Set<String> whiteListedPeersId) {
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
                executor.awaitTermination(10000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e){
                e.printStackTrace();
            }
        }
    }

    private void init(){
        whiteListedPeersMap = new ConcurrentHashMap<>();
        peerToInfoMap = new ConcurrentHashMap<>();
        tasksForVerification = new ConcurrentHashMap<>();
        verificationQueue = new LinkedBlockingQueue<>();
        completedTasks = new LinkedBlockingQueue<>();
        availablePeers = new PriorityBlockingQueue<PeerProperty>();
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
            executor.execute(new PeerInitiatorRunner(p,availablePeers, whiteListedPeersMap, whiteListedPeersId.contains(p.getId()), peerToInfoMap, new HashMap<Peer, List<Long>>()));
        }

        EffectiveTaskRunner<T> completedTaskRunner;

        while(!schedulingProblem.isDone() || !verificationQueue.isEmpty() ){
            try {
                Thread.currentThread().sleep(1);

                while(!completedTasks.isEmpty()){
                    completedTaskRunner =  completedTasks.poll();
                    if(completedTaskRunner.isCompletedSuccessfully()){
                        handleAndVerifyTaskResult(completedTaskRunner);
                    } else { //We consider peer dead! TODO Report to central?
                        taskQueue.add(completedTaskRunner.getTask());
                        askForAnotherPeer();
                    }
                }
                if(!verificationQueue.isEmpty() && !availablePeers.isEmpty()){
                    Task<T> task = verificationQueue.poll();
                    Peer bestPeer = getBestPeer();
                    if(tasksForVerification.get(task) != bestPeer){
                        EffectiveTaskRunner<T> taskRunner = new EffectiveTaskRunner<T>(taskQueue.poll(),bestPeer, completedTasks);
                        executor.execute(taskRunner);
                    } else if(!availablePeers.isEmpty()) {
                        EffectiveTaskRunner<T> taskRunner = new EffectiveTaskRunner<T>(taskQueue.poll(), getBestPeer(), completedTasks);
                        availablePeers.add(getPeerProperty(bestPeer));
                        executor.execute(taskRunner);
                    }
                }

                if(!taskQueue.isEmpty() && !availablePeers.isEmpty()){
                    Peer bestPeer = getBestPeer();
                    EffectiveTaskRunner<T> taskRunner = new EffectiveTaskRunner<T>(taskQueue.poll(),bestPeer, completedTasks);
                    duplicateByChance(taskRunner);
                    executor.execute(taskRunner);
                }

                if(taskQueue.isEmpty() && verificationQueue.isEmpty() && !availablePeers.isEmpty()){
                    //TODO think abount running shit thats already running.
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
        System.out.println("I completed the problem");
    }

    private PeerProperty getPeerProperty(Peer peer){
        return new PeerProperty(averageList(peerSpeedMap.get(peer)),peer);
    }

    private void duplicateByChance(EffectiveTaskRunner taskRunner){
        Task<T> task = taskRunner.getTask();
        if(!whiteListedPeersMap.containsKey(taskRunner.getTargetPeer()) && !tasksForVerification.containsKey(task) && Math.random() <= verificationProbability){
            tasksForVerification.put(task, null);
            verificationQueue.add(task);
        }
    }

    private Peer getBestPeer(){ //TODO optimize
        return availablePeers.poll().getPeer();
    }

    //Returns false if we need to restart.
    private boolean handleAndVerifyTaskResult(EffectiveTaskRunner<T> taskRunner) {
        System.out.println("Duplication queue size: " + verificationQueue.size());
        Task<T> task = taskRunner.getTask();

        if(!tasksForVerification.containsKey(task)) {
            giveSchedulerResult(taskRunner);
            releasePeerAndUpdateSpeed(taskRunner);
            return true;
        }

        if(tasksForVerification.get(task) == null){
            tasksForVerification.put(task, new EffectiveResultPair<T>(taskRunner.getResult(),taskRunner.getTargetPeer()));
            giveSchedulerResult(taskRunner);
            releasePeerAndUpdateSpeed(taskRunner);
            return true;
        } else {
            if(schedulingProblem.compare(tasksForVerification.get(task).getResult(), taskRunner.getResult())){
                releasePeerAndUpdateSpeed(taskRunner);
                tasksForVerification.remove(task);
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

    private void askForAnotherPeer(){
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
