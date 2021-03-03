package org.mvavrill.tableSampling.models;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.variables.IntVar;

/**
 * @author Mathieu Vavrille
 */
public class NQueens extends ModelGenerator {

  private final int n;
  
  public NQueens(final int n) {
    this.n = n;
  }

  @Override
  public ModelAndVars generateModelAndVars() {
    Model model = new Model("NQueens");
    IntVar[] vars = new IntVar[n];
    for (int i = 0; i < vars.length; i++) {
      vars[i] = model.intVar("Q_" + i, 0, n-1, false);
    }
    model.allDifferent(vars, "BC").post();
    for (int i = 0; i < n - 1; i++) {
      for (int j = i + 1; j < n; j++) {
        int k = j - i;
        model.arithm(vars[i], "!=", vars[j], "+", -k).post();
        model.arithm(vars[i], "!=", vars[j], "+", k).post();
      }
    }
    return new ModelAndVars(model, vars);
  }

  @Override
  public String getName() {
    return n + "-queens";
  }
}
