package EffectiveScheduling;

import Central.PeerInfo;
import Peer.Peer;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * Created by JJ on 20-05-2015.
 */
public class PeerInitiatorRunner implements Runnable{
    private PeerInfo targetPeerInfo;
    private PriorityBlockingQueue<PeerProperty> peerQueue;
    private ConcurrentHashMap<String, Peer> whiteListedPeerMap;
    private boolean peerIsWhiteListed;
    private ConcurrentHashMap<Peer, PeerInfo> peerToInfoMap;
    private Map<Peer, List<Long>> peerSpeedMap;


    public PeerInitiatorRunner(PeerInfo targetPeerInfo, PriorityBlockingQueue<PeerProperty> peerQueue, ConcurrentHashMap<String, Peer> whiteListedPeerMap, boolean peerIsWhiteListed, ConcurrentHashMap<Peer, PeerInfo> peerToInfoMap, Map<Peer, List<Long>> peerSpeedMap) {
        this.targetPeerInfo = targetPeerInfo;
        this.peerQueue = peerQueue;
        this.whiteListedPeerMap = whiteListedPeerMap;
        this.peerIsWhiteListed = peerIsWhiteListed;
        this.peerToInfoMap = peerToInfoMap;
        this.peerSpeedMap = peerSpeedMap;
    }

    @Override
    public void run() {
        Peer targetPeer = null;
        try {
            targetPeer = (Peer) Naming.lookup("//" + targetPeerInfo.getId() + ":1099" + "/Peer");
        } catch (NotBoundException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        if(targetPeer != null) {

            peerToInfoMap.put(targetPeer, targetPeerInfo);

            if(peerIsWhiteListed) {
                whiteListedPeerMap.put(targetPeerInfo.getId(), targetPeer);
            }

            peerQueue.add(new PeerProperty(targetPeer, targetPeerInfo.getCapacity()));
            System.out.println("Connected to " + targetPeerInfo.getId() + " successfully");
        }
    }
}
