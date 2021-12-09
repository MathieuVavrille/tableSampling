package org.mvavrill.tableSampling.models;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.variables.IntVar;
import org.mvavrill.tableSampling.zpz.ZpZ;

/**
 * The CostasArrays model. Taken from the examples of chocosolver
 */
public class CostasArrays extends ModelGenerator {
  
  private final int n;
  private final ZpZ zpz;

  public CostasArrays(final int n) {
    this.n = n;
    zpz = new ZpZ(ZpZ.getPrimeGreaterThan(this.getMaxRange()));
  }

  @Override
  public ModelAndVars generateModelAndVars() {
    Model model = new Model("CostasArrays"); // Disable views because of a bug in chocoSolver 4.10.6 making compact table not working with views
    IntVar[] vars = model.intVarArray("v", n, 0, n - 1, false);
    IntVar[] vectors = new IntVar[(n * (n - 1)) / 2];
    for (int i = 0, k = 0; i < n; i++) {
      for (int j = i + 1; j < n; j++, k++) {
        IntVar d = model.intVar(model.generateName(), -n, n, false);
        model.arithm(d, "!=", 0).post();
        model.sum(new IntVar[]{vars[i], d}, "=", vars[j]).post();
        vectors[k] = model.intOffsetView(d, 2 * n * (j - i));
      }
    }
    model.allDifferent(vars, "AC").post();
    model.allDifferent(vectors, "BC").post();

    // symmetry-breaking
    model.arithm(vars[0], "<", vars[n - 1]).post();
    return new ModelAndVars(model, model.retrieveIntVars(true));
  }

  @Override
  public String getName() {
    return "CostasArrays-" + n;
  }
  
  @Override
  public ZpZ getZpZ() {
    return zpz;
  }
}
