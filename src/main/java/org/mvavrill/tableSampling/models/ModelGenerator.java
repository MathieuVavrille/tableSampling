package org.mvavrill.tableSampling.models;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.variables.IntVar;

import org.javatuples.Triplet;

/**
 * The abstract class to wrap all the models.
 * This class helps to add constraints in the model such as the hashMod constraints, or random tables.
 */
public abstract class ModelGenerator {

  public Model generateModel() {
    return this.generateModelAndVars().getModel();
  }

  public abstract ModelAndVars generateModelAndVars();

  public abstract String getName();

  /**
   * Get the maximum range of the variables, i.e. max_x UB(x)-LB(x)
   */
  public int getMaxRange() {
    IntVar[] vars = this.generateModelAndVars().getVars();
    int maxRange = vars[0].getUB() - vars[0].getLB();
    for (int i = 1; i < vars.length; i++) {
      if (vars[i].getUB() - vars[i].getLB() > maxRange)
        maxRange = vars[i].getUB() - vars[i].getLB();
    }
    return maxRange;
  }

  /** Get the minimum lower bound */
  public int getMinLB() {
    IntVar[] vars = this.generateModelAndVars().getVars();
    int minLB = vars[0].getLB();
    for (int i = 1; i < vars.length; i++) {
      if (vars[i].getLB() < minLB)
        minLB = vars[i].getLB();
    }
    return minLB;
  }

  public int getNbVars() {
    return this.generateModelAndVars().getVars().length;
  }
}
