package org.mvavrill.tableSampling.sampler;

import org.mvavrill.tableSampling.models.ModelGenerator;
import org.mvavrill.tableSampling.models.ModelAndVars;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.search.limits.SolutionCounter;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.search.strategy.selectors.values.IntValueSelector;
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainRandom;
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainRandomBound;
import org.chocosolver.solver.search.strategy.selectors.variables.Random;
import org.chocosolver.solver.variables.IntVar;

import java.util.List;
import java.util.ArrayList;

public class RandomStrategySampling extends Sampler {

  private final int nbSolutionSearched;

  public RandomStrategySampling(final ModelGenerator modGen, final int nbSolutionSearched, final java.util.Random random) {
    super(modGen,random);
    this.nbSolutionSearched = nbSolutionSearched;
  }

  public RandomStrategySampling(final ModelGenerator modGen, final java.util.Random random) {
    super(modGen, random);
    this.nbSolutionSearched = 1;
  }

  @Override
  public Solution sample() {
    List<Solution> solutions = sampleMultiple(nbSolutionSearched);
    return solutions.get(random.nextInt(solutions.size()));
  }
  
  @Override
  public List<Solution> sampleMultiple(final int nbSamples) {
    ModelAndVars modelAndVars = modGen.generateModelAndVars();
    IntVar[] vars = modelAndVars.getVars();
    Solver solver = modelAndVars.getModel().getSolver();
    
    // Bug fix for the random strategy
    IntValueSelector value = new IntDomainRandom(random.nextLong());
    IntValueSelector bound = new IntDomainRandomBound(random.nextLong());
    IntValueSelector selector = var -> {
      if (var.hasEnumeratedDomain()) {
        return value.selectValue(var);
      } else {
        return bound.selectValue(var);
      }
    };
    solver.setSearch(Search.intVarSearch(new Random<>(random.nextLong()), selector, vars));
    //solver.setSearch(Search.randomSearch(model.retrieveIntVars(true), random.nextLong())); // Bug with the random
    solver.setRestartOnSolutions();
    List<Solution> solutions = solver.findAllSolutions(new SolutionCounter(modelAndVars.getModel(), nbSamples));;
    return solutions;
  }

  @Override
  public String getSamplerName() {
    return "RandomStrategy" + ((nbSolutionSearched == 1) ? "" : "-" + nbSolutionSearched);
  }
}
