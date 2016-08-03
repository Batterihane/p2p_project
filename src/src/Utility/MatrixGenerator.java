package Utility;

import java.util.Random;

/**
 * Created by JJ on 06-05-2015.
 */
public class MatrixGenerator {
    private static Random rng = new Random();

    public static int[][] generateMatrix(int size){
        int[][] result = new int[size][size];
        for (int i = 0; i < result.length; i++){
            for(int j = 0; j < result.length; j++)
                result[i][j] = rng.nextInt(5);
        }

        return result;
    }

    public static int[][] generateFixedMatrix(int size){
        int[][] result = new int[size][size];

        for (int i = 0; i < result.length; i++){
            for(int j = 0; j < result.length; j++)
                result[i][j] = i+j;
        }

        return result;
    }


    public static int[][][] genrateMatrixSequence(int matrixSize, int sequenceLength){
        int[][][] result = new int[sequenceLength][][];
        for (int i = 0; i < sequenceLength; i++){
            result[i] = generateMatrix(matrixSize);
        }

        return result;
    }

    public static int[][][] genrateFixedMatrixSequence(int matrixSize, int sequenceLength){
        int[][][] result = new int[sequenceLength][][];
        for (int i = 0; i < sequenceLength; i++){
            result[i] = generateFixedMatrix(matrixSize);
        }

        return result;
    }


    public static void printMatrix(int[][] m){
        try{
            int rows = m.length;
            int columns = m[0].length;
            String str = "|\t";

            for(int i=0;i<rows;i++){
                for(int j=0;j<columns;j++){
                    str += m[i][j] + "\t";
                }

                System.out.println(str + "|");
                str = "|\t";
            }

        }catch(Exception e){System.out.println("Matrix is empty!!");}
    }
}
