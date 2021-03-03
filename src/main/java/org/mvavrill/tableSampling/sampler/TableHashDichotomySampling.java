package org.mvavrill.tableSampling.sampler;

import org.mvavrill.tableSampling.models.ModelGenerator;
import org.mvavrill.tableSampling.models.ModelAndVars;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.search.limits.SolutionCounter;
import org.chocosolver.solver.constraints.extension.Tuples;
import org.chocosolver.solver.variables.IntVar;

import org.javatuples.Pair;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

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

  public Solution sample() {
    List<StoredTable> storedTables = new ArrayList<StoredTable>();
    int nbToAdd = 1;
    List<Solution> solutions = boundedCSP(modGen.generateModelAndVars(), pivot);
    if (solutions.size() == 0)
      throw new IllegalStateException("The model is not satisfiable");
    while (solutions.size() == 0 || solutions.size() >= pivot) {
      ModelAndVars modelAndVars = modGen.generateModelAndVars();
      StoredTable.addTablesToModel(modelAndVars, storedTables);
      try {
        modelAndVars.getModel().getSolver().propagate();
      } catch (Exception e) {
        throw new IllegalStateException("If there is an error here, it means that the tables added are not consistent, which should be impossible");
      }
      List<StoredTable> newTables = new ArrayList<StoredTable>();
      while (newTables.size() < nbToAdd) { // Add tables
        StoredTable currentTable = StoredTable.generateTable(modelAndVars, nbVariablesInTable, probaTuple, random);
        if (currentTable == null) // If null is returned, it means that the model is instantiated
          break;
        currentTable.addToModel(modelAndVars);
        try {
          modelAndVars.getModel().getSolver().propagate();
          newTables.add(currentTable);
        } catch (Exception e) { // An inconsistent table has been found, then reinitialize the model, add the constraint and exit the loop
          modelAndVars = modGen.generateModelAndVars();
          StoredTable.addTablesToModel(modelAndVars, storedTables);
          StoredTable.addTablesToModel(modelAndVars, newTables);
          break;
        }
      }
      if (newTables.size() == 0) {
        nbToAdd = 1;
        continue;
      }
      solutions = boundedCSP(modelAndVars, pivot);
      while (solutions.size() == 0) {
        int tuplesToRemove = (newTables.size()+1)/2;
        for (int i = 0; i < tuplesToRemove; i++)
          newTables.remove(newTables.size()-1);
        ModelAndVars modelAndVarsLoop = modGen.generateModelAndVars();
        StoredTable.addTablesToModel(modelAndVarsLoop, storedTables);
        StoredTable.addTablesToModel(modelAndVarsLoop, newTables);
        solutions = boundedCSP(modelAndVarsLoop, pivot);
      }
      storedTables.addAll(newTables);
      nbToAdd = (newTables.size() == 0) ? 1 : newTables.size()*2;
    }
    System.out.print(storedTables.size() + " ");
    return solutions.get(random.nextInt(solutions.size()));
  }

  private List<Solution> boundedCSP(final ModelAndVars modelAndVars, final int maxSols) {
    return modelAndVars.getModel().getSolver().findAllSolutions(new SolutionCounter(modelAndVars.getModel(), maxSols));
  }

  @Override
  public String getSamplerName() {
    return "TableHashDichotomy-v" + nbVariablesInTable + "-pi" + pivot + "-pr" + probaTuple;
  }
}
