package org.mvavrill.tableSampling.sampler;

import org.mvavrill.tableSampling.models.ModelAndVars;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.constraints.extension.Tuples;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.iterators.DisposableValueIterator;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Random;
import java.util.stream.Collectors;

public class StoredTable {
  
  private final Tuples tuples;
  private final List<Integer> varsIds;
  
  public StoredTable(final Tuples tuples, final List<Integer> varsIds) {
    this.tuples = tuples;
    this.varsIds = varsIds;
  }

  public int size() {
    return tuples.nbTuples();
  }

  public Tuples getTuples() {
    return tuples;
  }

  public List<Integer> getVarsIds() {
    return varsIds;
  }

  public Constraint addToModel(final ModelAndVars modelAndVars) {
    final IntVar[] chosenVars = varsIds.stream()
      .map(i -> modelAndVars.getVars()[i])
      .toArray(IntVar[]::new);
    Constraint cstr = modelAndVars.getModel().table(chosenVars, tuples,"CT+");
    cstr.post();
    return cstr;
  }

  /* -------- Util functions to add tables to models -------- */
  public static List<Constraint> addTablesToModel(final ModelAndVars modelAndVars, final List<StoredTable> storedTables) {
    return storedTables.stream()
      .map(st -> st.addToModel(modelAndVars))
      .collect(Collectors.toList());
  }

  /* -------- Generation of tables -------- */
  /** Generates a table (type Tuples), and the associated indices of variables */
  public static StoredTable generateTable(final ModelAndVars modelAndVars, final int nbVariablesInTable, final double probaTuple, final Random random) {
    final List<IntVar> vars = Arrays.asList(modelAndVars.getVars());
    List<IntVar> uninstantiatedVars = vars.stream().filter(v -> !v.isInstantiated()).collect(Collectors.toList()); // Get all uninstantiated variables
    if (uninstantiatedVars.size() == 0)
      return null; // if there are no more uninstantiated variables, then do not return anything
    IntVar[] chosenVars = // Choose a variable among all the uninstantiated ones
      pickNAmongM(Math.min(nbVariablesInTable, uninstantiatedVars.size()), uninstantiatedVars.size(), new HashSet<Integer>(), random).stream()
      .map(i -> uninstantiatedVars.get(i))
      .toArray(IntVar[]::new);
    Tuples allowedTuples = new Tuples(true);
    fillTuples(allowedTuples, chosenVars, 0, new int[chosenVars.length], probaTuple, random);
    while (allowedTuples.nbTuples() == 0) // Just ensuring that the tuples are not empty, otherwise it is useless to solve
      fillTuples(allowedTuples, chosenVars, 0, new int[chosenVars.length], probaTuple, random);
    return new StoredTable(allowedTuples, Arrays.stream(chosenVars).map(var -> vars.indexOf(var)).collect(Collectors.toList()));
  }

  private static int hammingDistance(final int[] chosenVarsIds, final int[] currentInstantiation, final int[] previousSolutions) {
    int res = 0;
    for (int i = 0; i < chosenVarsIds.length; i++)
      if (currentInstantiation[i] != previousSolutions[chosenVarsIds[i]])
        res++;
    return res;
  }

  private static int computeMinHammingDistance(final int[] chosenVarsIds, final int[] currentInstantiation, final List<int[]> previousSolutions) {
    return previousSolutions.stream().mapToInt(sol -> hammingDistance(chosenVarsIds, currentInstantiation, sol)).min().orElse(chosenVarsIds.length);
  }

  private static double computeAverageHammingDistance(final int[] chosenVarsIds, final int[] currentInstantiation, final List<int[]> previousSolutions) {
    return previousSolutions.stream().mapToDouble(sol -> hammingDistance(chosenVarsIds, currentInstantiation, sol)).sum()/previousSolutions.size();
  }

  private static void fillTuplesWithProba(final List<int[]> allInstantiations, final List<Double> probabilities, Tuples tuples, final Random random) {
    for (int i = 0; i < allInstantiations.size(); i++)
      if (random.nextDouble() < probabilities.get(i))
        tuples.add(allInstantiations.get(i));
  }

  public static StoredTable generateTable(final ModelAndVars modelAndVars, final int nbVariablesInTable, final double probaTuple, final Random random, final List<int[]> previousSolutions) {
    final List<IntVar> vars = Arrays.asList(modelAndVars.getVars());
    List<IntVar> uninstantiatedVars = vars.stream().filter(v -> !v.isInstantiated()).collect(Collectors.toList()); // Get all uninstantiated variables
    if (uninstantiatedVars.size() == 0)
      return null; // if there are no more uninstantiated variables, then do not return anything
    IntVar[] chosenVars = // Choose a variable among all the uninstantiated ones
      pickNAmongM(Math.min(nbVariablesInTable, uninstantiatedVars.size()), uninstantiatedVars.size(), new HashSet<Integer>(), random).stream()
      .map(i -> uninstantiatedVars.get(i))
      .toArray(IntVar[]::new);
    int nb_vars = chosenVars.length;
    int[] chosenVarsIds = new int[chosenVars.length];
    for (int i = 0; i < chosenVars.length; i++)
      for (int j = 0; j < vars.size(); j++)
        if (vars.get(j) == chosenVars[i])
          chosenVarsIds[i] = j;
    int prod_domains = 1;
    for (IntVar v : chosenVars)
      prod_domains = prod_domains * v.getDomainSize();
    List<int[]> allInstantiations = new ArrayList<int[]>();
    fillAllInstantiations(chosenVars, 0, allInstantiations, new int[chosenVars.length]);
    List<Double> averageHammingDistances = allInstantiations.stream()
      .map(inst -> (previousSolutions.size() == 0) ? 1.0 : computeAverageHammingDistance(chosenVarsIds, inst, previousSolutions))
      .collect(Collectors.toList());
    List<Double> probabilities = averageHammingDistances.stream()
      .map(h -> (1-probaTuple/1000)/nb_vars*h+probaTuple/1000)
      .collect(Collectors.toList());
    double factor_probabilities = prod_domains * probaTuple / probabilities.stream().mapToDouble(d -> d).sum();
    probabilities = probabilities.stream().map(d -> factor_probabilities*d).collect(Collectors.toList());
    Tuples allowedTuples = new Tuples(true);
    fillTuplesWithProba(allInstantiations, probabilities, allowedTuples, random);
    while (allowedTuples.nbTuples() == 0) // Just ensuring that the tuples are not empty, otherwise it is useless to solve
      fillTuplesWithProba(allInstantiations, probabilities, allowedTuples, random);
    return new StoredTable(allowedTuples, Arrays.stream(chosenVars).map(var -> vars.indexOf(var)).collect(Collectors.toList()));
  }

  /** Picks randomly nbSampled distinct integers from [0, totalNumber-1]. There are better ways to do this, but it will be enough for our applications */
  private static Set<Integer> pickNAmongM(final int nbSampled, final int totalNumber, final Set<Integer> currentIntegers, final Random random) {
    if (nbSampled == 0)
      return currentIntegers;
    int randVal;
    do {
      randVal = random.nextInt(totalNumber);
    } while (currentIntegers.contains(randVal));
    currentIntegers.add(randVal);
    return pickNAmongM(nbSampled-1, totalNumber, currentIntegers, random);
  }
  
  /** Fill the Tuple by adding with probability proba the current instantiation of the chosen variables. Recursive function to deal with an unknown number of variables*/
  private static void fillTuples(final Tuples allowedTuples, final IntVar[] chosenVars, final int varIndex, final int[] currentInstantiation, final double proba, final Random random) {
    if (varIndex == chosenVars.length) {
      if (random.nextDouble() < proba)
        allowedTuples.add(currentInstantiation);
    }
    else {
      DisposableValueIterator iterator = chosenVars[varIndex].getValueIterator(true);
      while(iterator.hasNext()){
        int currentValue = iterator.next();
        currentInstantiation[varIndex] = currentValue;
        fillTuples(allowedTuples, chosenVars, varIndex + 1, currentInstantiation, proba, random);
      }
    }
  }

  private static void fillAllInstantiations(final IntVar[] chosenVars, final int varIndex, List<int[]> allInstantiations, final int[] currentInstantiation) {
    if (varIndex == chosenVars.length) {
      int[] storedInstantiation = new int[varIndex];
      for (int i = 0; i < varIndex; i++)
        storedInstantiation[i] = currentInstantiation[i];
      allInstantiations.add(storedInstantiation);
    }
    else {
      DisposableValueIterator iterator = chosenVars[varIndex].getValueIterator(true);
      while(iterator.hasNext()){
        int currentValue = iterator.next();
        currentInstantiation[varIndex] = currentValue;
        fillAllInstantiations(chosenVars, varIndex + 1, allInstantiations, currentInstantiation);
      }
    }
  }
}
