package org.mvavrill.tableSampling.sampler;

import org.mvavrill.tableSampling.models.ModelGenerator;
import org.mvavrill.tableSampling.models.ModelAndVars;
import org.chocosolver.solver.search.limits.TimeCounter;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.variables.IntVar;

import java.util.ArrayList;
import java.util.List;

public class EnumerationSampling extends Sampler {

  public EnumerationSampling(final ModelGenerator modGen, final java.util.Random random) {
    super(modGen,random);
  }

  @Override
  public Solution sample(final long maxTime) {
    final Model model = modGen.generateModel();
    List<Solution> solutions = model.getSolver().findAllSolutions(new TimeCounter(model, maxTime));
    if (model.getSolver().isStopCriterionMet())
      return null;
    return solutions.get(random.nextInt(solutions.size()));
  }

  public int[] sampleArray() {
    ModelAndVars modelAndVars = modGen.generateModelAndVars();
    List<Solution> solutions = modelAndVars.getModel().getSolver().findAllSolutions();
    Solution solToReturn = solutions.get(random.nextInt(solutions.size()));
    IntVar[] vars = modelAndVars.getVars();
    int[] varSol = new int[vars.length];
    for (int i = 0; i < vars.length; i++)
      varSol[i] = solToReturn.getIntVal(vars[i]);
    return varSol;
  }

  @Override
  public String getSamplerName() {
    return "Enumeration";
  }
}
