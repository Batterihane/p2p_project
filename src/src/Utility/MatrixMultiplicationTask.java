package Utility;

import java.io.Serializable;

/**
 * Created by JJ on 07-05-2015.
 */
public class MatrixMultiplicationTask implements Task, Serializable {
    private int[][] firstMatrix;
    private int[][][] matrices;

    public MatrixMultiplicationTask(int[][] firstMatrix, int[][]... matrices) {
        this.matrices = matrices;
        this.firstMatrix = firstMatrix;
    }

    @Override
    public int[][] execute() {
        int[][] result = firstMatrix;
        for (int z = 0; z < matrices.length; z++) {
            int rowsInA = result.length;
            int columnsInA = result[0].length; // same as rows in B
            int columnsInB = matrices[z][0].length;
            int[][] c = new int[rowsInA][columnsInB];
            for (int i = 0; i < rowsInA; i++) {
                for (int j = 0; j < columnsInB; j++) {
                    for (int k = 0; k < columnsInA; k++) {
                        c[i][j] = c[i][j] + result[i][k] * matrices[z][k][j];
                    }
                }
            }
            result = c;
        }

        return result;
    }

    /*
    @Override
    public int[][] execute() {
        int[][] result = firstMatrix;
        for (int i = 0; i < matrices.length; i++)
            result = multiplyMatrices(result,matrices[i]);

        return result;
    }


    private static int[][] multiplyMatrices(int[][] a, int[][] b) {
        int rowsInA = a.length;
        int columnsInA = a[0].length; // same as rows in B
        int columnsInB = b[0].length;
        int[][] c = new int[rowsInA][columnsInB];
        for (int i = 0; i < rowsInA; i++) {
            for (int j = 0; j < columnsInB; j++) {
                for (int k = 0; k < columnsInA; k++) {
                    c[i][j] = c[i][j] + a[i][k] * b[k][j];
                }
            }
        }
        return c;
    }
    */
}
