package Utility;

/**
 * Created by JJ on 19-05-2015.
 */
public class ResultPair<T>{
    private T result;
    private String peerIp;

    public ResultPair(T result, String peerIp){
        this.result = result;
        this.peerIp = peerIp;
    }

    public String getPeerIp() {
        return peerIp;
    }

    public T getResult() {
        return result;
    }
}