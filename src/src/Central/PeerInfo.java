package Central;

import java.io.Serializable;

/**
 * Created by JJ on 06-05-2015.
 */
public class PeerInfo implements Serializable {
    public String id;
    public int capacity;

    public PeerInfo(String id, int capacity) {
        this.id = id;
        this.capacity = capacity;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    @Override
    public String toString() {
        return "PeerInfo{" +
                "id='" + id + '\'' +
                ", capacity=" + capacity +
                '}';
    }
}
