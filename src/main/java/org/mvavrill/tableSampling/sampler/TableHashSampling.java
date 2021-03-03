package org.mvavrill.tableSampling.sampler;

import org.mvavrill.tableSampling.models.ModelGenerator;
import org.mvavrill.tableSampling.models.ModelAndVars;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.search.limits.SolutionCounter;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.variables.IntVar;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

public class TableHashSampling extends Sampler {

  private final int pivot;
  private int previousNbTables = 0;
  private final int nbVariablesInTable;
  private final double probaTuple;

  public TableHashSampling(final int pivot, final int nbVariablesInTable, final double probaTuple, final ModelGenerator modGen, final java.util.Random random) {
    super(modGen,random);
    this.pivot = pivot;
    this.nbVariablesInTable = nbVariablesInTable;
    this.probaTuple = probaTuple;
  }

  public Solution sample() { // There may be an error because the propagation is done before adding the constraint
    List<StoredTable> storedTables = new ArrayList<StoredTable>(); // List of tables consistent
    final ModelAndVars modelAndVars = modGen.generateModelAndVars();
    final Model model = modelAndVars.getModel();
    final Solver solver = model.getSolver();
    List<Solution> solutions = solver.findAllSolutions(new SolutionCounter(model, pivot));
    if (solutions.size() == 0)
      throw new IllegalStateException("the model is not satisfiable");
    while (solutions.size() == 0 || solutions.size() == pivot) {
      solver.reset();
      try {
        solver.propagate();
      } catch (Exception e) {
        throw new IllegalStateException("If there is an error here, it means that the previous tables were not consistent, and thus should not have been added");
      }
      // Add new table
      StoredTable currentTable = StoredTable.generateTable(modelAndVars, nbVariablesInTable, probaTuple, random);
      //sover.reset(); // Maybe to remove issues when adding the table on already propagated solver
      Constraint currentConstraint = currentTable.addToModel(modelAndVars);
      // Solve
      solutions = solver.findAllSolutions(new SolutionCounter(model, pivot));
      if (solutions.size() == pivot) { // If too many solutions, add the table to the list
        storedTables.add(currentTable);
      }
      else if (solutions.size() == 0)
        model.unpost(currentConstraint);
    }
    return solutions.get(random.nextInt(solutions.size()));
  }

  @Override
  public String getSamplerName() {
    return "TableHash-v" + nbVariablesInTable + "-pi" + pivot + "-pr" + probaTuple;
  }

}
