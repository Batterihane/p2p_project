package Utility;

import java.io.Serializable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by JJ on 07-05-2015.
 */
public class MatrixMultiplicationTaskWithIntervals implements Task<int[][]>, Serializable {
	private static final long serialVersionUID = 6529685098267757691L;

    private int[][] leftMatrix;
    private int[][] rightMatrix;
    private Interval firstInterval;
    private Interval secondInterval;

    public MatrixMultiplicationTaskWithIntervals(int[][] a, int[][] b, Interval firstInterval, Interval secondInterval) {
        this.rightMatrix = b;
        this.leftMatrix = a;
        this.firstInterval = firstInterval;
        this.secondInterval = secondInterval;
    }

    public Interval getFirstInterval() {
        return firstInterval;
    }

    public Interval getSecondInterval() {
        return secondInterval;
    }

    @Override
    public int[][] execute() {
        return multiplyMatrices(leftMatrix, rightMatrix);
    }

    public static int[][] multiplyMatrices(int[][] a, int[][] b) {
        ExecutorService executor = Executors.newFixedThreadPool(16);
        int rowsInA = a.length;
        int columnsInA = a[0].length; // same as rows in B
        int columnsInB = b[0].length;
        int[][] c = new int[rowsInA][columnsInB];
        for (int i = 0; i < rowsInA; i++) {
            for (int j = 0; j < columnsInB; j++) {
                final int finalI = i;
                final int finalJ = j;
                executor.execute(new Runnable(){
                    public void run() {
                        for (int k = 0; k < columnsInA; k++) {
                            c[finalI][finalJ] = c[finalI][finalJ] + a[finalI][k] * b[k][finalJ];
                        }
                    }
                });
            }
        }

        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return c;
    }

    @Override
    public String toString() {
        return "MatrixMultiplicationTaskWithIntervals{" +
                "firstInterval=" + firstInterval +
                ", secondInterval=" + secondInterval +
                '}';
    }
}
