package org.mvavrill.tableSampling.sampler;

import org.mvavrill.tableSampling.LinEqSystemModP;
import org.mvavrill.tableSampling.models.ModelGenerator;
import org.mvavrill.tableSampling.models.ModelAndVars;
import org.mvavrill.tableSampling.zpz.*;


import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.search.limits.SolutionCounter;
import org.chocosolver.solver.search.limits.TimeCounter;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.variables.IntVar;


import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class HashModKDSampling extends Sampler {

  private final int pivot;
  private final ZpZ zpz;
  

  public HashModKDSampling(final int pivot, final ModelGenerator modGen, final java.util.Random random) {
    super(modGen,random);
    this.pivot = pivot;
    this.zpz = modGen.getZpZ();
  }

  @Override
  public Solution sample(final long maxTime) {
    final long startTime = System.nanoTime();
    final ModelAndVars modelAndVars = modGen.generateModelAndVars();
    final Model model = modelAndVars.getModel();
    final IntVar[] vars = modelAndVars.getVars();
    final Solver solver = model.getSolver();
    solver.addStopCriterion(new TimeCounter(model, Math.max(0L,maxTime-System.nanoTime()+startTime)));
    List<Solution> solutions = solver.findAllSolutions(new SolutionCounter(model, pivot));
    if (solver.isStopCriterionMet() && solutions.size() < pivot)
      return null;
    if (solutions.size() == 0)
      throw new IllegalStateException("the model is not satisfiable");
    List<ZpZEquation> addedEquations = new ArrayList<ZpZEquation>();
    while (solutions.size() == 0 || solutions.size() == pivot) {
      solver.reset();
      addedEquations.add(ZpZEquation.randomEquation(zpz, vars.length, random));
      LinEqSystemModP propagator = LinEqSystemModP.fromZpZEquations(vars, addedEquations.toArray(new ZpZEquation[0]));
      solver.setSearch(Search.minDomLBSearch(propagator.getParamVars()));
      Constraint currentConstraint = new Constraint("HashModConstraint", propagator);
      currentConstraint.post();
      solver.addStopCriterion(new TimeCounter(model, Math.max(0L,maxTime-System.nanoTime()+startTime)));
      solutions = solver.findAllSolutions(new SolutionCounter(model, pivot));
      if (solver.isStopCriterionMet() && solutions.size() < pivot)
        return null;
      model.unpost(currentConstraint);
      if (solutions.size() == 0)
        addedEquations.remove(addedEquations.size()-1);
    }
    return solutions.get(random.nextInt(solutions.size()));
  }

  @Override
  public String getSamplerName() {
    return "HashModKD-zpz"+zpz.p+"-p"+pivot;
  }





  

}
