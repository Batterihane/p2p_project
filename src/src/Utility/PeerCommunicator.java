package Utility;

import Central.*;
import Peer.Peer;

import java.rmi.RemoteException;
import java.util.List;

/**
 * Created by thoma_000 on 19-05-2015.
 */
public interface PeerCommunicator {
    public void init(List<PeerInfo> peers);
    public <T> T executeTaskOnPeer(Peer peer, Task<T> task) throws RemoteException;
}
