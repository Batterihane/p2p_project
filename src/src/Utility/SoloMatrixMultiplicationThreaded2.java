package Utility;


import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by thoma_000 on 13-05-2015.
 */
public class SoloMatrixMultiplicationThreaded2 {
    public static void startMatrixMultiplication(int[][][] matrices) {
        int[][] res = matrices[0];
        for (int i = 1 ; i<matrices.length ; i++){
            res = mul(res, matrices[i]);
        }
        //MatrixGenerator.printMatrix(res);
    }

    public static int[][] mul(int[][] left, int[][] right){
        ExecutorService executor = Executors.newFixedThreadPool(16);
        int rowsInA = left.length;
        int columnsInA = left[0].length; // same as rows in B
        int columnsInB = right[0].length;
        int[][] c = new int[rowsInA][columnsInB];
        for (int i = 0; i < rowsInA; i++) {
            for (int j = 0; j < columnsInB; j++) {
                final int finalI = i;
                final int finalJ = j;
                executor.execute(new Runnable(){
                    public void run() {
                        for (int k = 0; k < columnsInA; k++) {
                            c[finalI][finalJ] = c[finalI][finalJ] + left[finalI][k] * right[k][finalJ];
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
}
