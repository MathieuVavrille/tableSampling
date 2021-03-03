package org.mvavrill.tableSampling;

import org.mvavrill.tableSampling.models.*;
import org.mvavrill.tableSampling.sampler.*;
import org.mvavrill.tableSampling.memoizedData.MemoizedData;
import org.mvavrill.tableSampling.plotting.*;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.extension.Tuples;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.IntVar;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import org.javatuples.Pair;
import org.javatuples.Triplet;

import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.List;
import java.util.ArrayList;


/** This class can be executed as a Main. It is made for testing and generating files that contains the data for the CZNs */
public class TestMain {
  
  public static void main(String[] args) {
    Random random = new Random(97);
    /*try {
      generateAllPValueGraphs(random);
    } catch(IOException e) {
      System.exit(1);
      }*/

    new PvalueMain("samplerPvalue.json", 3, 3, 3, random).exec();
    new TimeMain("samplerTimes.json", 3, 3, 3, 10, random).exec();
    
  }







  
  /* -------- Test P-value -------- */
  private static void generateAllPValueGraphs(final Random random) throws IOException {
    PythonScriptCreator scriptCreator = new PythonScriptCreator("figures/global_script.py");
    //pValueModel(new FznLoader("/home/varville-m/Documents/these/code/gaussianZpZ/csplibmodels/feature.czn",20), 30.0, random, scriptCreator, "figures/p-value/feature20");
    //pValueModel(new FznLoader("/home/varville-m/Documents/these/code/gaussianZpZ/csplibmodels/feature.czn",50), 8.0, random, scriptCreator, "figures/p-value/feature50");
    //pValueModel(new FznLoader("/home/varville-m/Documents/these/code/gaussianZpZ/csplibmodels/feature.czn",90), 8.0, random, scriptCreator, "figures/p-value/feature90");
    //pValueModel(new FznLoader("/home/varville-m/Documents/these/code/gaussianZpZ/csplibmodels/oc-roster/oc-roster-4s-10d.czn"), 8.0, random, scriptCreator, "figures/p-value/ocr-4s-10d-0");
    //pValueModel(new FznLoader("/home/varville-m/Documents/these/code/gaussianZpZ/csplibmodels/oc-roster/oc-roster-4s-10d.czn", 1), 0.5, random, scriptCreator, "figures/p-value/ocr-4s-10d-1");
    //pValueModel(new NQueens(8), 20.0, random, scriptCreator, "figures/p-value/08-queens");
    pValueModel(new NQueens(9), 30.0, random, scriptCreator, "figures/p-value/09-queens");
    //pValueModel(new NQueens(10), 5.0, random, scriptCreator, "figures/p-value/10-queens");
    scriptCreator.close();
  }
  private static void pValueModel(final ModelGenerator modGen, final double solutionFactor, final Random random, final PythonScriptCreator scriptCreator, final String saveFile) throws IOException {
    final long time = System.nanoTime();
    System.out.println("\n" + modGen.getName());
    int nbSols = pValueSampler(new RandomStrategySampling(modGen, random), solutionFactor, scriptCreator);
    pValueSampler(new TableHashDichotomyUnpostSampling(8, 2, 0.125, modGen, random), solutionFactor, scriptCreator);
    pValueSampler(new TableHashDichotomyUnpostSampling(16, 2, 0.125, modGen, random), solutionFactor, scriptCreator);
    pValueSampler(new TableHashDichotomyUnpostSampling(16, 3, 0.0625, modGen, random), solutionFactor, scriptCreator);
    pValueSampler(new TableHashDichotomyUnpostSampling(32, 3, 0.0625, modGen, random), solutionFactor, scriptCreator);
    scriptCreator.addVerticalLine("Nb Sols", nbSols);
    scriptCreator.saveFigure("p-value on " + modGen.getName(), "nbSamples", "p-value", true, saveFile);
    System.out.println(" > " + (System.nanoTime()-time)/1000000000 + "s");
  }

  private static int pValueSampler(final Sampler sampler, final double solutionFactor, final PythonScriptCreator scriptCreator) throws IOException {
    System.out.println(" | " + sampler.getSamplerName());
    Triplet<double[], Long, Integer> res = getPValueIterated(sampler, solutionFactor);
    scriptCreator.addPlot(sampler.getSamplerName() + "-" + formatTime(res.getValue1()), PlottingUtils.convertToString(res.getValue0()), true, 0);
    return res.getValue2();
  }

  private static Triplet<double[], Long, Integer> getPValueIterated(final Sampler sampler, final double solutionFactor) {
    System.out.println(" | | Initialisation");
    final StatisticalTests statTests = new StatisticalTests(sampler);
    final long time = System.nanoTime();
    System.out.println(" | | Sampling");
    final int[] samples = statTests.generateSamples((int) Math.round(statTests.getNbSolutions()*solutionFactor));
    final long resTime = System.nanoTime()-time;
    return new Triplet<double[], Long, Integer>(statTests.getPValueIterated(samples), resTime, statTests.getNbSolutions());
  }

  private static String formatTime(final long nanoTime) {
    long secondTime = nanoTime/1000000000;
    if (secondTime > 5*3600)
      return secondTime/3600 + "h";
    else if (secondTime > 600)
      return secondTime/60 + "m";
    else if (secondTime > 10)
      return secondTime + "s";
    else if (nanoTime > 10*1000000)
      return nanoTime/1000000 + "ms";
    else
      return nanoTime + "ns";
  }






  
  /* -------- Test of diversification -------- */
  /*private static void diversificationTest(final ModelGenerator modGen, final int nbSamples, final Random random, final PythonScriptCreator scriptCreator) throws IOException {
    long time = System.nanoTime();
    Sampler randomSampler = new RandomStrategySampling(modGen, random);
    List<List<Integer>> randomSamples = randomSampler.sampleMultiple(nbSamples);
    scriptCreator.addPlot("Random", PlottingUtils.convertToString(hammingAverage(randomSamples)), true, 0);
    System.out.println((System.nanoTime() - time)/1000000);
    long time2 = System.nanoTime();
    Sampler tableSampler = new TableHashSampling(5.0, 3, 0.125, modGen, random);
    List<List<Integer>> tableSamples = tableSampler.sampleMultiple(nbSamples);
    scriptCreator.addPlot("Table", PlottingUtils.convertToString(hammingAverage(tableSamples)), true, 0);
    System.out.println((System.nanoTime() - time2)/1000000);
    long time5 = System.nanoTime();
    Sampler tableDTSampler = new TableHashDichotomySampling(5.0, 3, 0.125, modGen, random);
    List<List<Integer>> tableDTSamples = tableDTSampler.sampleMultiple(nbSamples);
    scriptCreator.addPlot("TableDTS", PlottingUtils.convertToString(hammingAverage(tableDTSamples)), true, 0);
    System.out.println((System.nanoTime() - time5)/1000000);
  }

  private static List<Double> hammingAverage(final List<List<Integer>> solutions) {
    List<Integer> sums = new ArrayList<Integer>();
    sums.add(hammingDistance(solutions.get(0), solutions.get(1)));
    for (int i = 2; i < solutions.size(); i++) {
      final List<Integer> newSolution = solutions.get(i);
      int previousSum = sums.get(i-2);
      for (int j = 0; j < i; j++)
        previousSum += hammingDistance(newSolution, solutions.get(j));
      sums.add(previousSum);
    }
    List<Double> averages = new ArrayList<Double>();
    for (int i = 0; i < sums.size(); i++)
      averages.add(((double) sums.get(i))*2/((i+1)*(i+2)));
    return averages;
  }

  private static int hammingDistance(final List<Integer> v1, List<Integer> v2) {
    int distance = 0;
    for (int i = 0; i < v1.size(); i++) {
      if (v1.get(i) != v2.get(i))
        distance++;
    }
    return distance;
    }*/


  /* -------- Getting the optimal solution and saving the data -------- */
  private static int getOptimal(final Model model) {
    Solver solver = model.getSolver();
    int result = 0;
    while (solver.solve()) {
      result = model.getObjective().asIntVar().getValue();
      System.out.print(result + " ");
    }
    System.out.println("");
    return result;
  }

  private static void saveData(final String fileName) {
    ModelGenerator vrp = new FznLoader(fileName, -1);
    int optimal = getOptimal(vrp.generateModel());
    System.out.println("Optimal found = " + optimal);
    List<MemoizedData> datas = new ArrayList<MemoizedData>();
    int i = optimal;
    int previousNbSolutions = -1;
    int nbSolutions;
    do {
      ModelGenerator vrpReduced = new FznLoader(fileName, -1);
      Model model = vrpReduced.generateModel();
        switch (model.getResolutionPolicy()) {
        case MAXIMIZE:
          model.getObjective().asIntVar().ge(i).post();
          model.clearObjective();
          break;
        case MINIMIZE:
          model.getObjective().asIntVar().le(i).post();
          model.clearObjective();
          break;
        }
      nbSolutions = getNbSolutions(model);
      if (nbSolutions != previousNbSolutions) {
        previousNbSolutions = nbSolutions;
        datas.add(new MemoizedData(i, nbSolutions));
        System.out.println("Searching for all solutions with |obj-opt| <= " + (optimal-i)); 
        System.out.println(nbSolutions + " found");
      }
      i--;
    } while (nbSolutions < 100);
    try {
      ObjectWriter mapper = new ObjectMapper().writerWithDefaultPrettyPrinter();
      mapper.writeValue(new File(FznLoader.dataName(fileName)), datas);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  private static int getNbSolutions(final Model model) {
    Solver solver = model.getSolver();
    int count = 0;
    while (count < 10000 && solver.solve()) {
      for (IntVar v : model.retrieveIntVars(true)) {
      }
      count++;
    }
    return count;
  }
}
