package CollatzSequence;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Created by JJ on 18-05-2015.
 */
public class CollatzResult implements Serializable, Comparable<CollatzResult>, Comparator<CollatzResult>{
    private long length;
    private long number;

    public long getLength() {
        return length;
    }

    public long getNumber() {
        return number;
    }

    public CollatzResult(long length, long number) {
        this.length = length;
        this.number = number;
    }

    @Override
    public int compareTo(CollatzResult o) {
        return Long.compare(length, o.getLength());
    }

    @Override
    public int compare(CollatzResult o1, CollatzResult o2) {
        return Long.compare(o1.length, o2.length);
    }
}
