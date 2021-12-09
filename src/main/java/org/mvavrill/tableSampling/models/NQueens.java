package org.mvavrill.tableSampling.models;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.variables.IntVar;
import org.mvavrill.tableSampling.zpz.ZpZ;

/**
 * <br/>
 *
 * @author Mathieu Vavrille
 * @since 02/11/20
 */
public class NQueens extends ModelGenerator {

  private final int n;
  protected final ZpZ zpz;
  
  public NQueens(final int n) {
    this.n = n;
    zpz = new ZpZ(ZpZ.getPrimeGreaterThan(this.getMaxRange()));
  }

  @Override
  public ModelAndVars generateModelAndVars() {
    Model model = createModel("NQueens");
    IntVar[] vars = new IntVar[n];
    for (int i = 0; i < vars.length; i++) {
      vars[i] = model.intVar("Q_" + i, 30, 30+n-1, false);
    }
    //model.allDifferent(vars, "BC").post();
    for (int i = 0; i < n - 1; i++) {
      for (int j = i + 1; j < n; j++) {
        model.arithm(vars[i], "!=", vars[j]).post();
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

  @Override
  public ZpZ getZpZ() {
    return zpz;
  }
  
}
