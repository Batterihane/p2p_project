package Central;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.security.*;
import java.util.List;

/**
 * Created by JJ on 22-04-2015.
 */
public interface Central extends Remote {
    public boolean joinAsPeer(int capacity) throws RemoteException;
    public KeyPair joinAsResearcher(String password) throws RemoteException;
    public List<PeerInfo> getPeersCapacity(int amount) throws RemoteException;
    public List<PeerInfo> getPeersAmount(int amount) throws RemoteException;
    public boolean verifyLiveness() throws RemoteException;
    public Key getResearcherPublicKey(String researcher) throws RemoteException;
    public void repportPeer(String peer) throws RemoteException;
    public void releasePeers(List<PeerInfo> peerList) throws RemoteException;
}
