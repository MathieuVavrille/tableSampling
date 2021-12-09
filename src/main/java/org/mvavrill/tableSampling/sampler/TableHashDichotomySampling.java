package org.mvavrill.tableSampling.sampler;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.search.SearchState;
import org.chocosolver.solver.search.limits.SolutionCounter;
import org.chocosolver.solver.search.limits.TimeCounter;
import org.chocosolver.solver.variables.IntVar;
import org.mvavrill.tableSampling.models.ModelAndVars;
import org.mvavrill.tableSampling.models.ModelGenerator;

import java.util.ArrayList;
import java.util.List;

public class TableHashDichotomySampling extends Sampler {

  private final int pivot;
  private int previousNbTables = 0;
  private final int nbVariablesInTable;
  private final double probaTuple;

  public TableHashDichotomySampling(final int pivot, final int nbVariablesInTable, final double probaTuple, final ModelGenerator modGen, final java.util.Random random) {
    super(modGen,random);
    this.pivot = pivot;
    this.nbVariablesInTable = nbVariablesInTable;
    this.probaTuple = probaTuple;
  }

  public Solution sample(final long maxTime) {
    long startTime = System.nanoTime();
    int nbAdded = 0;
    int nbToAdd = 1;
    final ModelAndVars modelAndVars = modGen.generateModelAndVars();
    final Model model = modelAndVars.getModel();
    final Solver solver = modelAndVars.getModel().getSolver();
    //solver.addStopCriterion();
    List<Solution> solutions = solver.findAllSolutions(new SolutionCounter(model, pivot), new TimeCounter(model, Math.max(0L,maxTime-System.nanoTime()+startTime)));
    if (solver.getSearchState() == SearchState.STOPPED && solutions.size() < pivot) // Timeout
      return null;
    if (solutions.size() == 0)
      throw new IllegalStateException("the model is not satisfiable");
    solver.reset();
    while (solutions.size() == 0 || solutions.size() >= pivot) {
      //System.out.println("while");
      try {
        model.getEnvironment().worldPush(); // required to make sure initial propagation can be undone
        solver.propagate();
      } catch (Exception e) {
        throw new IllegalStateException("If there is an error here, it means that the tables added are not consistent, which should be impossible");
      }
      // Adding new tables
      List<StoredTable> newTables = new ArrayList<StoredTable>();
      while (newTables.size() < nbToAdd) { // Add tables
        newTables.add(StoredTable.generateTable(modelAndVars, nbVariablesInTable, probaTuple, random));
        nbAdded++;
        /*if (currentTable == null) {// If null is returned, it means that the model is instantiated
          break;
        }
        Constraint currentConstraint = currentTable.addToModel(modelAndVars);
        try { // add the table to check that it does not make the problem inconsistent
          model.getEnvironment().worldPush(); // Save the state
          solver.propagate();
          model.getEnvironment().worldPop();
        } catch (Exception e) { // An inconsistent table has been found, then come back to previous state, remove the constraint and exit the loop
          model.getEnvironment().worldPop();
          model.unpost(currentConstraint);
          break;
          }*/
      }
      model.getEnvironment().worldPop(); // undo initial propagation
      solver.getEngine().reset(); // prepare the addition of the new constraint
      List<Constraint> newCstrs = StoredTable.addTablesToModel(modelAndVars, newTables);
      //System.out.println(newCstrs.size());
      //solver.addStopCriterion(new TimeCounter(model, Math.max(maxTime-System.nanoTime()+startTime,0L)));
      solutions = solver.findAllSolutions(new SolutionCounter(model, pivot), new TimeCounter(model, Math.max(0L,maxTime-System.nanoTime()+startTime)));
      //System.out.println("found");
      if (solver.getSearchState() == SearchState.STOPPED && solutions.size() < pivot) // Timeout
        return null;
      solver.reset();
      // Removing new tables that are inconsistent
      while (solutions.size() == 0) {
        //System.out.println("remove " + solutions.size());
        int cstrsToRemove = (newCstrs.size()+1)/2;
        for (int i = 0; i < cstrsToRemove; i++) {
          model.unpost(newCstrs.remove(newCstrs.size()-1)); // Pop the constraint from the list, and remove it from the model
          nbAdded--;
        }
        //solver.addStopCriterion(new TimeCounter(model, Math.max(maxTime-System.nanoTime()+startTime,0L)));
        solutions = solver.findAllSolutions(new SolutionCounter(model, pivot), new TimeCounter(model, Math.max(0L,maxTime-System.nanoTime()+startTime)));
        if (solver.getSearchState() == SearchState.STOPPED && solutions.size() < pivot) // Timeout
          return null;
        solver.reset();
      }
      nbToAdd = (newCstrs.size() == 0) ? 1 : newCstrs.size()*2;
    }
    //System.out.print(solutions.size()*Math.pow(1/probaTuple,(double)nbAdded) + " ");
    return solutions.get(random.nextInt(solutions.size()));
  }









  
  public int[] sampleWithSolutions(final List<int[]> previousSolutions) {
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
        StoredTable currentTable = StoredTable.generateTable(modelAndVars, nbVariablesInTable, probaTuple, random, previousSolutions);
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
    Solution solToReturn = solutions.get(random.nextInt(solutions.size()));
    IntVar[] vars = modelAndVars.getVars();
    int[] varSol = new int[vars.length];
    for (int i = 0; i < vars.length; i++)
      varSol[i] = solToReturn.getIntVal(vars[i]);
    return varSol;
  }


  @Override
  public String getSamplerName() {
    return "TableHashDichotomy-v" + nbVariablesInTable + "-pi" + pivot + "-pr" + probaTuple;
  }
}
