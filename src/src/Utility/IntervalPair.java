package Utility;

/**
 * Created by JJ on 07-05-2015.
 */
public class IntervalPair {
    public Interval left;
    public Interval right;

    public IntervalPair(Interval left, Interval right) {
        this.left = left;
        this.right = right;
    }

    public Interval getLeft() {
        return left;
    }

    public void setLeft(Interval left) {
        this.left = left;
    }

    public Interval getRight() {
        return right;
    }

    public void setRight(Interval right) {
        this.right = right;
    }
}
