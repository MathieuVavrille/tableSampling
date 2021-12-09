package org.mvavrill.tableSampling.models;

import org.mvavrill.tableSampling.zpz.ZpZ;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;

/**
 * A simple model with two boolean variables, and the constraint x or y = 1
 *
 * @author Mathieu Vavrille
 */
public class ToyOr extends ModelGenerator {

  protected final ZpZ zpz;

  public ToyOr() {
    zpz = new ZpZ(ZpZ.getPrimeGreaterThan(this.getMaxRange()));
  }

  @Override
  public ModelAndVars generateModelAndVars() {
    Model model = createModel("ToyOr");
    BoolVar x = model.boolVar("x");
    BoolVar y = model.boolVar("y");
    model.or(x,y).post();
    return new ModelAndVars(model, new IntVar[]{x,y});
  }

  @Override
  public String getName() {
    return "ToyOr";
  }

  @Override
  public ZpZ getZpZ() {
    return zpz;
  }
}
