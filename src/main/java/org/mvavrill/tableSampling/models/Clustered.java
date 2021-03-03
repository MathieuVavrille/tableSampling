package org.mvavrill.tableSampling.models;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.expression.discrete.relational.ReExpression;


/**
 * A toy example with variables in {0,1,2} and constraints x_1 = 0 <==> x_2 = 0 <==> ... <==> x_n = 0
 */
public class Clustered extends ModelGenerator {

  private final int n;

  public Clustered(final int n) {
    this.n = n;
  }

  @Override
  public ModelAndVars generateModelAndVars() {
    Model model = new Model();
    IntVar[] vars = model.intVarArray(n,0,2);
    ReExpression[] eqToZero = new ReExpression[n-1];
    for (int i = 1; i < vars.length; i++)
      eqToZero[i-1] = vars[i].eq(0);
    vars[0].eq(0).iff(eqToZero).post();
    model.getSolver().setSearch(Search.inputOrderLBSearch(vars));
    return new ModelAndVars(model, vars);
  }
  
  @Override
  public String getName() {
    return "Clustered-" + n;
  }
}
