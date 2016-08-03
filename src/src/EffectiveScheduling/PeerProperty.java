package EffectiveScheduling;

import Peer.Peer;

/**
 * Created by JJ on 20-05-2015.
 */
public class PeerProperty implements Comparable<PeerProperty>{
    private Peer peer;
    private long timeForTaskCompletion = -1;
    private int capacity = -1;

    public PeerProperty(Peer peer, int capacity) {
        this.peer = peer;
        this.capacity = capacity;
    }

    public PeerProperty(Peer peer, int capacity, long timeForTaskCompletion) {
        this.peer = peer;
        this.capacity = capacity;
        this.timeForTaskCompletion = timeForTaskCompletion;
    }

    public PeerProperty(long timeForTaskCompletion, Peer peer) {
        this.timeForTaskCompletion = timeForTaskCompletion;
        this.peer = peer;
    }

    public Peer getPeer() {
        return peer;
    }

    public int getCapacity() {
        return capacity;
    }

    public long getTimeForTaskCompletion() {
        return timeForTaskCompletion;
    }

    @Override
    public int compareTo(PeerProperty o) {
        if(timeForTaskCompletion != o.timeForTaskCompletion)
            return Long.compare(o.timeForTaskCompletion, timeForTaskCompletion);

        return Integer.compare(capacity, o.capacity);
    }
}
