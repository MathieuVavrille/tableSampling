package org.mvavrill.tableSampling.models;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.variables.IntVar;
import org.mvavrill.tableSampling.zpz.ZpZ;

import static java.util.Arrays.fill;
import static org.chocosolver.solver.search.strategy.Search.inputOrderLBSearch;

/**
 * The Magic Square model. Taken from the examples of chocosolver
 */
public class MagicSquare extends ModelGenerator {
  
  private final int n;
  private final ZpZ zpz;

  public MagicSquare(final int n) {
    this.n = n;
    zpz = new ZpZ(ZpZ.getPrimeGreaterThan(this.getMaxRange()));
  }

  @Override
  public ModelAndVars generateModelAndVars() {
    Model model = createModel("MagicSquare");
    int ms = n * (n * n + 1) / 2;
    IntVar[][] matrix = new IntVar[n][n];
    IntVar[][] invMatrix = new IntVar[n][n];
    IntVar[] vars = new IntVar[n*n];
    int k = 0;
    for (int i = 0; i < n; i++) {
      for (int j = 0; j < n; j++, k++) {
        matrix[i][j] = model.intVar("square" + i + "," + j, 1, n * n, false);
        vars[k] = matrix[i][j];
        invMatrix[j][i] = matrix[i][j];
      }
    }
    IntVar[] diag1 = new IntVar[n];
    IntVar[] diag2 = new IntVar[n];
    for (int i = 0; i < n; i++) {
      diag1[i] = matrix[i][i];
      diag2[i] = matrix[(n-1)-i][i];
    }
    model.allDifferent(vars, "BC").post();
    int[] coeffs = new int[n];
    fill(coeffs, 1);
    for (int i = 0; i < n; i++) {
      model.scalar(matrix[i], coeffs, "=", ms).post();
      model.scalar(invMatrix[i], coeffs, "=", ms).post();
    }
    model.scalar(diag1, coeffs, "=", ms).post();
    model.scalar(diag2, coeffs, "=", ms).post();
    // Symetries breaking
    model.arithm(matrix[0][n - 1], "<", matrix[n - 1][0]).post();
    model.arithm(matrix[0][0], "<", matrix[n - 1][n - 1]).post();
    model.arithm(matrix[0][0], "<", matrix[n - 1][0]).post();
    model.getSolver().setSearch(inputOrderLBSearch(vars));
    return new ModelAndVars(model, model.retrieveIntVars(true));
  }

  @Override
  public String getName() {
    return "MagicSquare-" + n;
  }

  @Override
  public ZpZ getZpZ() {
    return zpz;
  }
}
