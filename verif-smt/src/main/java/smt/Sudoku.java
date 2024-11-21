package smt;

import com.microsoft.z3.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Scanner;

public class Sudoku {

    public static void main(String[] args) {
        String inPath = args[0];
        String outPath = args[1];

        try (Context ctx = new Context()) {
            // Read the input txt file using a helper function, it returns an 2D array.
            int[][] grid = readInputFile(inPath);

            // Create a Z3 array to represent the game map
            ArrayExpr sudokuGrid = ctx.mkArrayConst("sudokuNodeMap", ctx.mkIntSort(), ctx.mkIntSort());
            Solver solver = ctx.mkSolver();

            // Adding constrain
            for (int i = 0; i < 9; i++) {
                for (int j = 0; j < 9; j++) {
                    // Calculate the index of current node, because this is an 1D array, so grid[1][0] is represented by 10, and grid[0][9] is represented by 9.
                    Expr index = ctx.mkAdd(ctx.mkInt(i), ctx.mkMul(ctx.mkInt(j), ctx.mkInt(9)));

                    // Constrain: The value of each node should between 1 to 9.
                    if (grid[i][j] == 0) {
                        solver.add(ctx.mkAnd(
                            ctx.mkLe(ctx.mkInt(1), (ArithExpr) ctx.mkSelect(sudokuGrid, index)),
                            ctx.mkLe((ArithExpr) ctx.mkSelect(sudokuGrid, index), ctx.mkInt(9))
                        ));
                    } else {
                        // If at one node position, the number of input file is not 0, means it already has the original value, to make the map array to that value.
                        solver.add(ctx.mkEq(ctx.mkSelect(sudokuGrid, index), ctx.mkInt(grid[i][j])));
                    }
                }
            }

            // Constrain1: Row constrain: each row must have all values between 1 to 9(include) and no duplicate
            for (int i = 0; i < 9; i++) {
                Expr[] row = new Expr[9];
                for (int j = 0; j < 9; j++) {
                    // Calculate the index of current node.
                    Expr index = ctx.mkAdd(ctx.mkInt(j), ctx.mkMul(ctx.mkInt(i), ctx.mkInt(9)));

                    // Add value to the row array.
                    row[j] = (ArithExpr) ctx.mkSelect(sudokuGrid, index);
                }
                solver.add(ctx.mkDistinct(row));
            }

            // Constrain2: Column constrain: each column must have all values between 1 to 9(include) and no duplicate
            for (int i = 0; i < 9; i++) {
                Expr[] col = new Expr[9];
                for (int j = 0; j < 9; j++) {
                    // Calculate the index of current node.
                    Expr index = ctx.mkAdd(ctx.mkInt(i), ctx.mkMul(ctx.mkInt(j), ctx.mkInt(9)));

                    // Add value to the column array.
                    col[j] = (ArithExpr) ctx.mkSelect(sudokuGrid, index);
                }
                solver.add(ctx.mkDistinct(col));
            }

            // Constrain3: Then each 3*3 block contains 1-9 without duplicate.
            for (int blockRow = 0; blockRow < 3; blockRow++) {
                for (int blockCol = 0; blockCol < 3; blockCol++) {
                    // At each iteration, create a new block array to store the value of 9 elements
                    Expr[] block = new Expr[9];

                    // An index to record the index of block element
                    int idx = 0;
                    for (int i = 0; i < 3; i++) {
                        for (int j = 0; j < 3; j++) {
                            // Calculate the index of current node.
                            int rowIndex = blockRow * 3 + i;
                            int colIndex = blockCol * 3 + j;
                            Expr index = ctx.mkAdd(ctx.mkInt(rowIndex), ctx.mkMul(ctx.mkInt(colIndex), ctx.mkInt(9)));

                            // Add value to the block array.
                            block[idx++] = (ArithExpr) ctx.mkSelect(sudokuGrid, index);
                        }
                    }
                    solver.add(ctx.mkDistinct(block));
                }
            }


            // Solve the problem
            if (solver.check() == Status.SATISFIABLE) {
                // Get model at first (solution)
                Model model = solver.getModel();

                // Create an new 9*9 array to store the result.
                int[][] solution = new int[9][9];

                // Store the value to the array
                for (int i = 0; i < 9; i++) {
                    for (int j = 0; j < 9; j++) {
                        Expr index = ctx.mkAdd(ctx.mkInt(j), ctx.mkMul(ctx.mkInt(i), ctx.mkInt(9)));
                        solution[i][j] = Integer.parseInt(model.eval(ctx.mkSelect(sudokuGrid, index), true).toString());
                    }
                }

                // Using helper function to write result back to the output path
                writeOutputFile(solution, outPath);
            } else {
                // Otherwise no solution, so using helper function to write result back to the output path
                writeNoSolution(outPath);
            }
        }
    }

    // Read and decode the input file (should be a 9*9 2D array)
    private static int[][] readInputFile(String fileName) {
        int[][] grid = new int[9][9];
        try (Scanner scanner = new Scanner(new File(fileName))) {
            for (int i = 0; i < 9; i++) {
                for (int j = 0; j < 9; j++) {
                    if (scanner.hasNextInt()) {
                        grid[i][j] = scanner.nextInt();
                    }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return grid;
    }

    // Write the solution back to the output file path
    private static void writeOutputFile(int[][] solution, String fileName) {
        try (PrintWriter writer = new PrintWriter(fileName)) {
            for (int i = 0; i < 9; i++) {
                for (int j = 0; j < 9; j++) {
                    writer.print(solution[i][j]);
                    if (j < 8) {
                        writer.print(" ");
                    }
                }
                writer.println();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    // If no solution, write no solution to the output file path.
    private static void writeNoSolution(String fileName) {
        try (PrintWriter writer = new PrintWriter(fileName)) {
            writer.println("No Solution");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
