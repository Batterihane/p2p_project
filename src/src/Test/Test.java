package Test;

import Utility.MatrixCalculator;
import Utility.MatrixGenerator;

import java.util.Random;

/**
 * Created by JJ on 06-05-2015.
 */
public class Test {
    private static Random rng = new Random();

    public static void main(String[] args) {
        int matrixSize = 2000;
        int amountOfMatrices = 100;

        int[][][] matrices = new int[amountOfMatrices][matrixSize][matrixSize];
        for (int i = 0; i < matrices.length; i++)
            matrices[i] = MatrixGenerator.generateMatrix(matrixSize);

        int[][] vector = new int[1][matrixSize];
        for (int i = 0; i < vector[0].length; i++)
            vector[0][i] = rng.nextInt(25) + 1;

        long start = System.currentTimeMillis();

        for (int i = 0; i < matrices.length; i++)
            vector = multiplyMatrix(vector,matrices[i]);

        System.out.println("It took " + (System.currentTimeMillis() - start) + " ms to multiply the vector onto " + matrices.length + " matrices size " + matrixSize + "x" + matrixSize);
    }


    public static int[][] multiplyMatrix(int[][] leftMatrix, int[][] rightMatrix) {
        int rowsInA = leftMatrix.length;
        int columnsInA = leftMatrix[0].length; // same as rows in B
        int columnsInB = rightMatrix[0].length;
        int[][] c = new int[rowsInA][columnsInB];
        for (int i = 0; i < rowsInA; i++) {
            for (int j = 0; j < columnsInB; j++) {
                for (int k = 0; k < columnsInA; k++) {
                    c[i][j] = c[i][j] + leftMatrix[i][k] * rightMatrix[k][j];
                }
            }
        }
        return c;
    }
}
