package Researcher;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Created by JJ on 22-04-2015.
 */
public interface Researcher extends Remote {
    public byte[] decryptMessage(byte[] cipherText) throws RemoteException;
}
