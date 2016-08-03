package Utility;

import java.math.BigInteger;
import java.util.stream.IntStream;

/**
 * Created by JJ on 27-04-2015.
 */
public class Fac {
    private static BigInteger one = new BigInteger("1");

    private static BigInteger recursive(BigInteger n){
        if(n.equals(one))
            return one;

        return n.multiply(recursive(n.subtract(one)));
    }

    public static BigInteger iterative(int stop) {
        BigInteger integer = new BigInteger("1");
        for(int i = 2; i <= stop; i++){
            integer = integer.multiply(new BigInteger(i +""));
        }

        return integer;
    }

    private static BigInteger iterative2(int stop) {
        return IntStream.range(1,stop).parallel().mapToObj(i -> new BigInteger(i + "")).reduce(one, (acc, val) -> acc.multiply(val));
    }

    public static void main(String[] args) {
        int stop = 100000;

        long start = System.currentTimeMillis();
        recursive(new BigInteger("" + stop));
        System.out.println("n = " + stop + ". Bigint recursive took: " + (System.currentTimeMillis() - start) + " ms");
        start = System.currentTimeMillis();
        iterative(stop);
        System.out.println("n = " + stop + ". Bigint iterative took: " + (System.currentTimeMillis() - start) + " ms");
        start = System.currentTimeMillis();
        iterative2(stop);
        System.out.println("n = " + stop + ". Bigint iterative2 took: " + (System.currentTimeMillis() - start) + " ms");
    }
}
