package Central;

import Peer.Peer;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.*;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;
import java.security.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class StandardCentral extends UnicastRemoteObject implements Central{
    private final static String researcherPassword = "let me in";

    private KeyPairGenerator keyGen;
    private Set<String> evilPeers = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
    private List<String> orderedPeerList = Collections.synchronizedList(new LinkedList<String>());
    private ConcurrentHashMap<String,Integer> peerCapacities = new ConcurrentHashMap<String,Integer>();
    private ConcurrentHashMap<String,KeyPair> researcherList = new ConcurrentHashMap<String,KeyPair>();


    protected StandardCentral() throws RemoteException {
        super();
        createKeyPairGenerator();
    }

    public static void main(String args[]) throws Exception {
        System.out.println("Central started");

        try { //special exception handler for registry creation
            LocateRegistry.createRegistry(1099);
            System.out.println("java RMI registry created on port 1099.");
        } catch (RemoteException e) {
            LocateRegistry.getRegistry(1099);
            //registry already exists
            System.out.println("java RMI registry already exists.");
        }

        //Instantiate Central

        Central obj = new StandardCentral();

        // Bind this object instance to the name "Central"
        Naming.rebind("//localhost/Central", obj);
        System.out.println("Central bound in registry");
    }

    private void createKeyPairGenerator() {
        try {
            keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
        } catch(NoSuchAlgorithmException e){
            System.out.println("Failed to create KeyPairGenerator.");
        }
    }

    @Override
    public boolean joinAsPeer(int capacity) throws RemoteException {
        try {
            String host = getClientHost();
            if(evilPeers.contains(host)){
                System.out.println("Evil peer tried to join and was denied: " + host);
                return false;
            }
            if(peerCapacities.containsKey(host)){
                System.out.println("Joining peer already exists in system.");
                orderedPeerList.remove(host);
                orderedPeerList.add(0, host);
                return true;
            }
            orderedPeerList.add(0, host);
            peerCapacities.put(host, capacity);
        } catch (ServerNotActiveException e) {
            System.out.println("Peer trying to join, died.");
            return false;
        }
        System.out.println("Peer list: " + orderedPeerList);
        return true;
    }

    @Override
    public KeyPair joinAsResearcher(String password) throws RemoteException {
        if(!password.equals(researcherPassword))
            return null;

        String researcher = null;
        try {
            researcher = getClientHost();
        } catch (ServerNotActiveException e) {
            System.out.println("Researcher disconnected while joining. ");
            return null;
        }

        if(researcherList.containsKey(researcher)){
            System.out.println("Joining researcher already exists in system. Returning old keypair.");
            return researcherList.get(researcher);
        }

        KeyPair keyPair = keyGen.generateKeyPair();
        System.out.println("Researcher added");
        researcherList.put(researcher, keyPair);
        System.out.println("Researcher list: " + researcherList);
        return keyPair;
    }

    @Override
    public List<PeerInfo> getPeersAmount(int amount) throws RemoteException {
        String researcher;
        try {
            researcher = getClientHost();
        } catch (ServerNotActiveException e) {
            System.out.println("Researcher disconnected whlie requesting peers");
            return null;
        }

        if(!researcherList.containsKey(researcher))
            return null;

        Iterator<String> it = orderedPeerList.iterator();
        int count = 0;
        List<PeerInfo> list = new ArrayList<PeerInfo>();

        if(orderedPeerList.contains(researcher) && amount > 0){  // Researcher's own peer is always prefered
            int capacity = peerCapacities.get(researcher);
            list.add(new PeerInfo(researcher, capacity));
            count++;
        }

        while(it.hasNext() && count < amount){
            String tmp = it.next();
            if(tmp.equals(researcher)) continue;
            int capacity = peerCapacities.get(tmp);

            count++;
            list.add(new PeerInfo(tmp, capacity));
        }

        return list;
    }


    @Override
    public List<PeerInfo> getPeersCapacity(int amount) throws RemoteException {
        try {
            if(!researcherList.containsKey(getClientHost()))
                return null;
        } catch (ServerNotActiveException e) {
            System.out.println("Researcher disconnected whlie requesting peers");
            return null;
        }

        Iterator<String> it = orderedPeerList.iterator();
        int count = 0;
        List<PeerInfo> list = new ArrayList<PeerInfo>();

        while(it.hasNext() && count < amount){
            String tmp = it.next();
            int capacity = peerCapacities.get(tmp);

            count += capacity;
            list.add(new PeerInfo(tmp, capacity));
        }

        return list;
    }

    @Override
    public boolean verifyLiveness() throws RemoteException{
        try {
            String host = getClientHost();
            if(peerCapacities.get(host) == null) return false;
            orderedPeerList.remove(host);
            orderedPeerList.add(host);
            return true;
        } catch (ServerNotActiveException e) {
            System.out.println("Peer died whlie verifying liveness");
            return false;
        }

    }

    @Override
    public Key getResearcherPublicKey(String researcher) throws RemoteException {
        System.out.println("Received public key request for researcher at " + researcher);
        KeyPair keyPair = researcherList.get(researcher);
        if(keyPair == null) return null;
        return keyPair.getPublic();
    }

    @Override
    public void repportPeer(String peer) throws RemoteException {
        System.out.println(peer + " added as evil peer!");
        evilPeers.add(peer);
        orderedPeerList.remove(peer);
        peerCapacities.remove(peer);
    }

    @Override
    public void releasePeers(List<PeerInfo> peerList) throws RemoteException {
        System.out.println("Peers were released: " + peerList);
        for(PeerInfo p : peerList)
            if(!evilPeers.contains(p.getId()))
                orderedPeerList.add(p.getId());
    }
}
