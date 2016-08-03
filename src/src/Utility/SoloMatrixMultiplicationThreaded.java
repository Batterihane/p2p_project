package Utility;

import java.util.Arrays;

/**
 * Created by thoma_000 on 13-05-2015.
 */
public class SoloMatrixMultiplicationThreaded {
    public static void startMatrixMultiplication(int[][][] matrices) {
        System.out.println("SoloMatrixMultiplicationSchedulerThreaded started");
        Arrays.asList(matrices).stream()
                .skip(1)
                .parallel()
                .reduce(matrices[0], SoloMatrixMultiplicationThreaded::mul);

        System.out.println("SoloMatrixMultiplicationSchedulerThreaded is done");

        //MatrixGenerator.printMatrix(res);
    }

    private static int[][] mul(int[][] left, int[][] right){
        int rowsInA = left.length;
        int columnsInA = left[0].length; // same as rows in B
        int columnsInB = right[0].length;
        int[][] c = new int[rowsInA][columnsInB];
        for (int i = 0; i < rowsInA; i++) {
            for (int j = 0; j < columnsInB; j++) {
                for (int k = 0; k < columnsInA; k++) {
                    c[i][j] = c[i][j] + left[i][k] * right[k][j];
                }
            }
        }
        return c;
    }
}
