package Utility;

import Central.*;
import Peer.Peer;

import java.rmi.RemoteException;
import java.util.List;

/**
 * Created by thoma_000 on 19-05-2015.
 */
public class StandardPeerCommunicator implements PeerCommunicator {
    @Override
    public void init(List<PeerInfo> peers){}

    @Override
    public <T> T executeTaskOnPeer(Peer peer, Task<T> task) throws RemoteException{
        return peer.executeTask(task);
    }
}
