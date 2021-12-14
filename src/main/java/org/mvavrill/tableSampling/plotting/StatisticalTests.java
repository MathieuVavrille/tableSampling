package org.mvavrill.tableSampling.plotting;

import org.mvavrill.tableSampling.models.ModelGenerator;
import org.mvavrill.tableSampling.models.ModelAndVars;
import org.mvavrill.tableSampling.sampler.Sampler;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.variables.IntVar;

import org.apache.commons.math3.stat.inference.TestUtils;

import java.util.Random;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class StatisticalTests {

  private final Sampler sampler;
  private final int nbSolutions;
  private final Map<String,Integer> solToInt = new HashMap<String,Integer>();
  private final List<String> intToSolStr;
  private final List<Solution> intToSolList;

  /* -------- Initialisation of data -------- */
  public StatisticalTests(final Sampler sampler) {
    this.sampler = sampler;
    ModelAndVars modelAndVars = sampler.getModelGenerator().generateModelAndVars();
    Solver solver = modelAndVars.getModel().getSolver();
    intToSolList = solver.findAllSolutions();
    intToSolStr = intToSolList.stream().map(s -> solRepr(s)).collect(Collectors.toList());
    for (int i = 0; i < intToSolStr.size(); i++) {
      solToInt.put(intToSolStr.get(i), i);
    }
    this.nbSolutions = intToSolStr.size();
  }

  private static String solRepr(final Solution sol) {
    List<IntVar> vars = sol.retrieveIntVars(true);
    return vars.stream()
      .map(v -> Integer.toString(sol.getIntVal(v)))
      .collect(Collectors.joining("|"));
  }

  public int sample() {
    return solToInt.get(solRepr(sampler.sample()));
  }

  public int[] generateSamples(final int nbSamples) {
    int[] samples = new int[nbSamples];
    for (int i = 0; i < nbSamples; i++) {
      samples[i] = solToInt.get(solRepr(sampler.sample()));
    }
    return samples;
  }

  public long[] samplesToCount(final int[] samples) {
    return samplesToCountLimited(samples, samples.length);
  }
  
  public long[] samplesToCountLimited(final int[] samples, final int maxId) {
    long[] solutionCount = new long[nbSolutions];
    for (int i = 0; i < maxId; i++) {
      solutionCount[samples[i]]++;
    }
    return solutionCount;
  }

  public double[] getAverageCounts(final int nbSolutions, final int nbSamples) { 
    double[] solutionAverage = new double[nbSolutions];
    for (int i = 0; i < nbSolutions; i++)
      solutionAverage[i] = (double) nbSamples/ nbSolutions;
    return solutionAverage;
  }

  public double pValueChiSquared(final List<Integer> samples) {
    return pValueChiSquared(samples.stream().mapToInt(v -> v).toArray());
  }

  public double pValueChiSquared(final int[] samples) {
    return TestUtils.chiSquareTest(this.getAverageCounts(nbSolutions, samples.length), this.samplesToCount(samples));
  }
  
  public double pValueChiSquaredLimited(final int[] samples, final int maxId) {
    return TestUtils.chiSquareTest(this.getAverageCounts(nbSolutions, maxId), this.samplesToCountLimited(samples, maxId));
  }

  public boolean chiSquaredVerified(final int[] samples, final double confidence) {
    return TestUtils.chiSquareTest(this.getAverageCounts(nbSolutions, samples.length), this.samplesToCount(samples), confidence);
  }

  public double[] getPValueIterated(final int[] samples) {
    double[] pvalues = new double[samples.length-1];
    for (int i = 1; i < samples.length; i++)
      pvalues[i-1] = pValueChiSquaredLimited(samples, i);
    return pvalues;
  }

  public double couponCollectorTimeObserved(final int[] samples) {
    int nbFullCollections = 0;
    int totalTimeFullCollections = 0;
    Set<Integer> currentCouponsCollected = new HashSet<Integer>();
    for (int i = 0; i < samples.length; i++) {
      currentCouponsCollected.add(samples[i]);
      if (currentCouponsCollected.size() == nbSolutions) {
        nbFullCollections++;
        totalTimeFullCollections = i+1;
        currentCouponsCollected = new HashSet<Integer>();
      }
    }
    return (double)totalTimeFullCollections / nbFullCollections;
  }

  public double couponCollecterTimeExpected() {
    double res = 0;
    for (int i = nbSolutions; i > 0; i--) {
      res += 1/(double)i;
    }
    return nbSolutions * res;
  }

  // We record the first time there is a collision. If there is a collision sooner than average, it means that the probabilityes are less likely to be uniform
  public double firstCollisionTimeObserved(final int[] samples) {
    int nbCollisions = 0;
    int totalTimeCollisions = 0;
    Set<Integer> currentCouponsCollected = new HashSet<Integer>();
    for (int i = 0; i < samples.length; i++) {
      if (currentCouponsCollected.contains(samples[i])) {
        nbCollisions++;
        totalTimeCollisions = i+1;
        currentCouponsCollected = new HashSet<Integer>();
      }
      else
        currentCouponsCollected.add(samples[i]);
    }
    return (double)totalTimeCollisions / nbCollisions;
  }

  public double firstCollisionTimeExpected() { // Warning, probably some issues with floating point arithmetic
    double d = nbSolutions;
    double res = 0;
    for (int i = nbSolutions+1; i > 1; i--) {
      double prod = (double) (i*(i-1)) / (double) d;
      for (int j = 1; j < i-1; j++) {
        prod *= (d-j)/d;
      }
      res += prod;
    }
    return res;
  }

  public int getNbSolutions() {
    return nbSolutions;
  }
  
}
