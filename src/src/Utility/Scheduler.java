package Utility;

import Central.PeerInfo;

import java.security.KeyPair;
import java.util.List;

/**
 * Created by JJ on 13-05-2015.
 */
public interface Scheduler<T> {
    public void start();
    public void enableEncryption(KeyPair keyPair);
    public void disableEncryption();
    public List<PeerInfo> getPeers();
}
