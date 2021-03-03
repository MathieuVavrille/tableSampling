package org.mvavrill.tableSampling.sampler;

import org.mvavrill.tableSampling.models.ModelGenerator;
import org.mvavrill.tableSampling.models.ModelAndVars;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.search.limits.SolutionCounter;
import org.chocosolver.solver.variables.IntVar;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

public class TableHashDichotomyUnpostSampling extends Sampler {

  private final int pivot;
  private int previousNbTables = 0;
  private final int nbVariablesInTable;
  private final double probaTuple;

  public TableHashDichotomyUnpostSampling(final int pivot, final int nbVariablesInTable, final double probaTuple, final ModelGenerator modGen, final java.util.Random random) {
    super(modGen,random);
    this.pivot = pivot;
    this.nbVariablesInTable = nbVariablesInTable;
    this.probaTuple = probaTuple;
  }

  public Solution sample() {
    int nbAdded = 0;
    int nbToAdd = 1;
    final ModelAndVars modelAndVars = modGen.generateModelAndVars();
    final Model model = modelAndVars.getModel();
    final Solver solver = modelAndVars.getModel().getSolver();
    List<Solution> solutions = solver.findAllSolutions(new SolutionCounter(model, pivot)); // fail si 0 solutions
    solver.reset();
    while (solutions.size() == 0 || solutions.size() >= pivot) {
      try {
        solver.propagate();
      } catch (Exception e) {
        throw new IllegalStateException("If there is an error here, it means that the tables added are not consistent, which should be impossible");
      }
      // Adding new tables
      List<Constraint> newTables = new ArrayList<Constraint>();
      while (newTables.size() < nbToAdd) { // Add tables
        StoredTable currentTable = StoredTable.generateTable(modelAndVars, nbVariablesInTable, probaTuple, random);
        if (currentTable == null) {// If null is returned, it means that the model is instantiated
          break;
        }
        Constraint currentConstraint = currentTable.addToModel(modelAndVars);
        try {
          model.getEnvironment().worldPush(); // Save the state
          solver.propagate();
          model.getEnvironment().worldPop();
          newTables.add(currentConstraint);
          nbAdded++;
        } catch (Exception e) { // An inconsistent table has been found, then come back to previous state, remove the constraint and exit the loop
          model.getEnvironment().worldPop();
          model.unpost(currentConstraint);
          break;
        }
      }
      
      /*// Le code qui suit est très long à s'executer, je ne sais pas vraiment pourquoi. Si on enlève le solver.reset(); à la fin alors il y a une erreur de solution inconsistante/propagation mauvaise.
      model.getEnvironment().worldPush();
      try {
        solver.propagate();
      } catch (Exception e) {
        throw new IllegalStateException("If there is an error here, it means that the tables added are not consistent, which should be impossible");
      }
      List<Constraint> newTables = new ArrayList<Constraint>();
      while (newTables.size() < nbToAdd) { // Add tables
        StoredTable currentTable = StoredTable.generateTable(modelAndVars, nbVariablesInTable, probaTuple, random);
        if (currentTable == null) {// If null is returned, it means that the model is instantiated
          break;
        }
        model.getEnvironment().worldPop();
        //solver.reset();
        Constraint currentConstraint = currentTable.addToModel(modelAndVars);
        try {
          model.getEnvironment().worldPush();
          solver.propagate();
          newTables.add(currentConstraint);
        } catch (Exception e) {
          model.unpost(currentConstraint);
          break;
        }
      }
      model.getEnvironment().worldPop();*/
      //solver.reset();



      
      if (newTables.size() == 0) { // If no constraint was added, restart the process
        nbToAdd = 1;
        continue;
      }
      solutions = solver.findAllSolutions(new SolutionCounter(model, pivot)); // Bound the solutions after adding constraints
      solver.reset();
      int count = 0;

      // Removing new tables that are inconsistent
      while (solutions.size() == 0) {
        int cstrsToRemove = (newTables.size()+1)/2;
        for (int i = 0; i < cstrsToRemove; i++) {
          model.unpost(newTables.remove(newTables.size()-1)); // Pop the constraint from the list, and remove it from the model
          nbAdded--;
        }
        solutions = solver.findAllSolutions(new SolutionCounter(model, pivot));
        solver.reset();
      }
      nbToAdd = (newTables.size() == 0) ? 1 : newTables.size()*2;
    }
    //System.out.print(solutions.size()*Math.pow(1/probaTuple,(double)nbAdded) + " ");
    return solutions.get(random.nextInt(solutions.size()));
  }

  @Override
  public String getSamplerName() {
    return "TableHashDichotomy-v" + nbVariablesInTable + "-pi" + pivot + "-pr" + probaTuple;
  }
}
