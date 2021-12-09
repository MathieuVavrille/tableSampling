package org.mvavrill.tableSampling.sampler;

import org.mvavrill.tableSampling.models.ModelGenerator;
import org.mvavrill.tableSampling.models.ModelAndVars;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.search.SearchState;
import org.chocosolver.solver.search.limits.SolutionCounter;
import org.chocosolver.solver.search.limits.TimeCounter;

import java.util.Random;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

public class TableHashSamplingNoPropagation extends Sampler {

  private final int pivot;
  private int previousNbTables = 0;
  private final int nbVariablesInTable;
  private final double probaTuple;

  public TableHashSamplingNoPropagation(final int pivot, final int nbVariablesInTable, final double probaTuples, final ModelGenerator modGen, final java.util.Random random) {
    super(modGen,random);
    //System.out.println(pivot + " " + nbVariablesInTable + " " + probaTuples);
    this.pivot = pivot;
    this.nbVariablesInTable = nbVariablesInTable;
    this.probaTuple = probaTuples;
  }

  public Solution sample(final long maxTime) {
    final long startTime = System.nanoTime();
    final ModelAndVars modelAndVars = modGen.generateModelAndVars();
    final Model model = modelAndVars.getModel();
    final Solver solver = model.getSolver();
    List<Solution> solutions = solver.findAllSolutions(new SolutionCounter(model, pivot), new TimeCounter(model, Math.max(0L,maxTime-System.nanoTime()+startTime)));
    if (solver.getSearchState() == SearchState.STOPPED && solutions.size() < pivot) // Timeout
      return null;
    if (solutions.size() == 0)
      throw new IllegalStateException("the model is not satisfiable");
    while (solutions.size() == 0 || solutions.size() == pivot) {
      solver.reset();
      // Add new table
      StoredTable currentTable = StoredTable.generateTable(modelAndVars, nbVariablesInTable, probaTuple, random);
      Constraint currentConstraint = currentTable.addToModel(modelAndVars);
      // Solve
      solutions = solver.findAllSolutions(new SolutionCounter(model, pivot), new TimeCounter(model, Math.max(0L,maxTime-System.nanoTime()+startTime)));
      if (solver.getSearchState() == SearchState.STOPPED && solutions.size() < pivot) // Timeout
        return null;
      if (solutions.size() == 0) {
        model.unpost(currentConstraint);
      }
    }
    return solutions.get(random.nextInt(solutions.size()));
  }

  @Override
  public String getSamplerName() {
    return "TableHashNoPropagation-v" + nbVariablesInTable + "-pi" + pivot + "-pr" + probaTuple;
  }

}
