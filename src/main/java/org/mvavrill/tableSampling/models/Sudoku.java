package org.mvavrill.tableSampling.models;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Cause;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.tools.ArrayUtils;

import org.javatuples.Pair;

public class Sudoku extends ModelGenerator {

  private final int n;
  private final int nbFixed;

  /** n is the size of a small square. The total grid has size n^4. Usual sudokus have n = 3*/
  public Sudoku(final int n, final int nbFixed) {
    this.n = n;
    this.nbFixed = nbFixed;
  }

  @Override
  public ModelAndVars generateModelAndVars() {
    Model model = new Model("Sudoku");
    IntVar[][] vars = model.intVarMatrix("x", n*n, n*n, 1, n*n);
    for (int k = 0; k < n*n; k++) {
      model.allDifferent(vars[k], "AC").post();
      model.allDifferent(ArrayUtils.getColumn(vars, k), "AC").post();
    }
    IntVar[] oneCase = new IntVar[n*n];
    for (int iCase = 0; iCase < n; iCase++)
      for (int jCase = 0; jCase < n; jCase++) {
        for (int i = 0; i < n; i++)
          for (int j = 0; j < n; j++)
            oneCase[i*n+j] = vars[iCase*n+i][jCase*n+j];
        model.allDifferent(oneCase, "AC").post();
      }
    for (int i = 0; i < (nbFixed+n*n-1)/(n*n); i++)
      for (int j = 0; j < Math.min(n*n, nbFixed-(n*n)*i); j++) {
        //vars[i][j].eq((j+n*i+(i/n))%(n*n)+1).post();
        try {
          vars[i][j].instantiateTo((j+n*i+(i/n))%(n*n)+1, Cause.Null);
        } catch (Exception e) {
          throw new IllegalStateException("It means that the method to fill the grid is wrong, aaaand I a pretty sure it is right");
        }
      }
    return new ModelAndVars(model, model.retrieveIntVars(true));
  }

  @Override
  public String getName() {
    return n + "-Sudoku";
  }
}
