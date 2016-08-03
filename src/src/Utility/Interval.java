package Utility;

import java.io.Serializable;

/**
 * Created by JJ on 07-05-2015.
 */
public class Interval implements Comparable<Interval>, Serializable {
    public long low = -1;
    public long high = -1;

    public long getLow() {
        return low;
    }

    public void setLow(long low) {
        this.low = low;
    }

    public long getHigh() {
        return high;
    }

    public void setHigh(long high) {
        this.high = high;
    }

    public Interval(long low, long high) {
        this.low = low;
        this.high = high;
    }

    @Override
    public int compareTo(Interval o) {
        return Long.compare(low,o.getLow());
    }

    @Override
    public String toString() {
        return "Interval{" +
                "low=" + low +
                ", high=" + high +
                '}';
    }
}
