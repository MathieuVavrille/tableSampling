package org.mvavrill.tableSampling.models;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.variables.IntVar;
import org.mvavrill.tableSampling.zpz.ZpZ;

/**
 * The Golombm Ruler model. Taken from the examples of chocosolver
 */
public class GolombRuler extends ModelGenerator {

  private final int m;
  protected final ZpZ zpz;
  
  public GolombRuler(final int m) {
    this.m = m;
    zpz = new ZpZ(ZpZ.getPrimeGreaterThan(this.getMaxRange()));
  }

  public ModelAndVars generateModelAndVars() {
    Model model = createModel("GolombRuler");
    IntVar[] ticks = model.intVarArray("a", m, 0, (m < 31) ? (1 << (m + 1)) - 1 : 9999, false);
    model.arithm(ticks[0], "=", 0).post();
    for (int i = 0; i < m - 1; i++) {
      model.arithm(ticks[i + 1], ">", ticks[i]).post();
    }
    IntVar[] diffs = model.intVarArray("d", (m * m - m) / 2, 0, (m < 31) ? (1 << (m + 1)) - 1 : 9999, false);
    IntVar[][] m_diffs = new IntVar[m][m];
    for (int k = 0, i = 0; i < m - 1; i++) {
      for (int j = i + 1; j < m; j++, k++) {
        model.scalar(new IntVar[]{ticks[j], ticks[i]}, new int[]{1, -1}, "=", diffs[k]).post();
        model.arithm(diffs[k], ">=", (j - i) * (j - i + 1) / 2).post();
        model.arithm(diffs[k], "-", ticks[m - 1], "<=", -((m - 1 - j + i) * (m - j + i)) / 2).post();
        model.arithm(diffs[k], "<=", ticks[m - 1], "-", ((m - 1 - j + i) * (m - j + i)) / 2).post();
        m_diffs[i][j] = diffs[k];
      }
    }
    model.allDifferent(diffs, "BC").post();
    // break symetries
    if (m > 2) {
      model.arithm(diffs[0], "<", diffs[diffs.length - 1]).post();
    }
    //model.getSolver().setSearch(inputOrderLBSearch(ticks));
    return new ModelAndVars(model, model.retrieveIntVars(true));
  }

  @Override
  public String getName() {
    return "GolombRuler-" + m;
  }
  
  @Override
  public ZpZ getZpZ() {
    return zpz;
  }
}
