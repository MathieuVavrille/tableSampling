package org.mvavrill.tableSampling.sampler;

import org.mvavrill.tableSampling.models.ModelGenerator;
import org.mvavrill.tableSampling.models.ModelAndVars;

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
  public Solution sample() {
    List<Solution> solutions = modGen.generateModel().getSolver().findAllSolutions();
    return solutions.get(random.nextInt(solutions.size()));
  }

  @Override
  public String getSamplerName() {
    return "Enumeration";
  }
}
