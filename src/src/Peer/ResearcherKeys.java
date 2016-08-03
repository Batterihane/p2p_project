package Peer;

import javax.crypto.SecretKey;
import java.security.Key;

/**
 * Created by thoma_000 on 07-05-2015.
 */
public class ResearcherKeys {
    private SecretKey secretKey;
    private Key publicKey;

    public ResearcherKeys(Key publicKey, SecretKey secretKey){
        this.secretKey = secretKey;
        this.publicKey = publicKey;
    }

    public Key getPublicKey() {
        return publicKey;
    }

    public SecretKey getSecretKey() {
        return secretKey;
    }
}
