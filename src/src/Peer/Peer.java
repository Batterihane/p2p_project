package Peer;

import Utility.Task;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Created by JJ on 22-04-2015.
 */
public interface Peer extends Remote {
    public <T> byte[] executeAndEncryptTask(byte[] encryptedTask) throws RemoteException;
    public <T> T executeTask(Task<T> task) throws RemoteException;
    public byte[] getSecretKey() throws RemoteException;
}
