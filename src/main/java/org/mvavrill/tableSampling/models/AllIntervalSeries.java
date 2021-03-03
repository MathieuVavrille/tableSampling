package org.mvavrill.tableSampling.models;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.variables.IntVar;

/**
 * The AllIntervalSeries model. Taken from the examples of chocosolver
 */
public class AllIntervalSeries extends ModelGenerator {

  private final int m;

  public AllIntervalSeries(final int m) {
    this.m = m;
  }

  @Override
  public ModelAndVars generateModelAndVars() {
    Model model = new Model("AllIntervalSeries");
    IntVar[] vars = model.intVarArray("v", m, 0, m - 1, false);
    IntVar[] dist = model.intVarArray("dist", m - 1, 1, m - 1, false);
    for (int i = 0; i < m - 1; i++) {
      model.distance(vars[i + 1], vars[i], "=", dist[i]).post();
    }
    model.allDifferent(vars, "BC").post();
    model.allDifferent(dist, "BC").post();
    // break symetries
    model.arithm(vars[1], ">", vars[0]).post();
    model.arithm(dist[0], ">", dist[m - 2]).post();
    return new ModelAndVars(model, model.retrieveIntVars(true));
  }

  @Override
  public String getName() {
    return "AllIntervalSeries-" + m;
  }
}
