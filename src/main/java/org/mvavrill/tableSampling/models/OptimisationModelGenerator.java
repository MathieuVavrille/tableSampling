package org.mvavrill.tableSampling.models;

import org.chocosolver.solver.variables.IntVar;

import org.javatuples.Triplet;


/**
 * The abstract class to wrap all the models.
 * This class extends the ModelGenerator to deal with optimisation problems.
 */
public abstract class OptimisationModelGenerator extends ModelGenerator {

  /**
   * returns a triplet containing the model and variables of the problem, the objective, and a boolean = 1 if it is a maximisation problem
   */
  public abstract Triplet<ModelAndVars, IntVar, Boolean> generateModelVarsObj();
  
}
