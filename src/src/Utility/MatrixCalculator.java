package Utility;

import java.io.Serializable;

/**
 * Created by JJ on 29-04-2015.
 */
public class MatrixCalculator implements Task, Serializable {
    private int[][] leftMatrix, rightMatrix;

    public MatrixCalculator(int[][] left, int[][] right){
        leftMatrix = left;
        rightMatrix = right;
    }

    public int[][] execute() {
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

    public void setMatrices(int[][] left, int[][] right){
        leftMatrix = left;
        rightMatrix = right;
    }

}
