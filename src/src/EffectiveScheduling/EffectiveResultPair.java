package EffectiveScheduling;

import Peer.Peer;

/**
 * Created by JJ on 21-05-2015.
 */
public class EffectiveResultPair<T> {
    private T result;
    private Peer peer;

    public EffectiveResultPair(T result, Peer peer) {
        this.result = result;
        this.peer = peer;
    }

    public T getResult() {
        return result;
    }

    public Peer getPeer() {
        return peer;
    }
}
