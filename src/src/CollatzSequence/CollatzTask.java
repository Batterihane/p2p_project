package CollatzSequence;

import Utility.Interval;
import Utility.Task;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

/**
 * Created by JJ on 18-05-2015.
 */
public class CollatzTask implements Serializable, Task<CollatzResult> {
    Interval numberInterval;

    /**
     * Both numbers in the interval are inclusive.
     * @param numberIntervalInclusive
     */
    public CollatzTask(Interval numberIntervalInclusive) {
        this.numberInterval = numberIntervalInclusive;
    }

    @Override
    public CollatzResult execute() {
        long maxLength = 0;
        long number = -1;

        System.out.println("My task is: " + numberInterval);

        return LongStream.rangeClosed(numberInterval.low, numberInterval.high)
                .parallel()
                .mapToObj(i -> new CollatzResult(collatzSequence(i), i))
                .max(new CollatzResult(0,0)).get();

                /*
        long current = -1;
        for(long i = numberInterval.low; i <= numberInterval.high; i++){
            current = collatzSequence(i);

            if(current > maxLength) {
                maxLength = current;
                number = i;
            }
        }

        return new CollatzResult(maxLength,number);
        */
    }

    private long collatzSequence(long n){
        BigInteger current = BigInteger.valueOf(n);
        BigInteger two = BigInteger.valueOf(2);
        BigInteger three = BigInteger.valueOf(3);

        long length = 1;

        while(!current.equals(BigInteger.ONE)){
            length++;
            if(isEven(current))
                current = current.divide(two);
            else
                current = current.multiply(three).add(BigInteger.ONE);
        }

        return length;
    }


    private boolean isEven(BigInteger number)
    {
        return number.getLowestSetBit() != 0;
    }
    /*
    private long collatzSequence(long n){
        long current = n;
        long length = 1;

        while(current != 1){
            length++;
            if((current & 1) == 0)
                current = current/2;
            else
                current = current * 3 + 1;
        }

        return length;
    }*/

    public static void main(String[] args) {
        CollatzTask task = new CollatzTask(new Interval(1, 50000000));
        long start = System.currentTimeMillis();
        task.execute();
        System.out.println("It took: " + (System.currentTimeMillis() - start) + " ms");
    }
}
