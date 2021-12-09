package org.mvavrill.tableSampling;

import org.mvavrill.tableSampling.models.*;
import org.mvavrill.tableSampling.sampler.*;
import org.mvavrill.tableSampling.memoizedData.SamplingData;

import org.chocosolver.solver.Solution;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.io.File;
import java.util.Random;
import java.util.Map;
import java.util.HashMap;

public class TimeMain {

  private final ModelGenerator modGen;
  private final String fileName;
  private final int nbPivots;
  private final int nbNVars;
  private final int nbProbas;
  private final int nbSamples;
  private final long maxTime;
  private final boolean doDichotomy;
  private final boolean doHashMod;
  private final boolean noPropagation;
  private final Random random;

  public TimeMain(final ModelGenerator modGen, final String fileName, final int nbPivots, final int nbNVars, final int nbProbas, final int nbSamples, final long maxTime, final boolean doDichotomy, final boolean doHashMod, final boolean noPropagation, final Random random) {
    this.modGen = modGen;
    this.fileName = fileName;
    this.nbPivots = nbPivots;
    this.nbNVars = nbNVars;
    this.nbProbas = nbProbas;
    this.nbSamples = nbSamples;
    this.maxTime = maxTime;
    this.doDichotomy = doDichotomy;
    this.doHashMod = doHashMod;
    this.noPropagation = noPropagation;
    this.random = random;
  }

  public void exec() {
    final SamplingData<Long> data = getData();
    updateModelTimes(data, true);
    saveData(data);
  }

  private void updateModelTimes(final SamplingData<Long> data, final boolean doKD) {
    System.out.println("Random Time -> " + (data.getRandomVarDom() != null ? data.getRandomVarDom()/1000000 +"ms": "-"));
    System.out.println("Table Sampling");
    final Map<Integer,Map<Integer,Map<Double,Long> >> tableHashTimes = data.getTableHash();
    for (int pivot_id = 0; pivot_id < nbPivots; pivot_id++) {
      final int pivot = new int[]{16,2,4,8,32,64,128,256}[pivot_id];
      System.out.println(" | pivot = " + pivot);
      final Map<Integer,Map<Double,Long>> pivotMap = (tableHashTimes.containsKey(pivot)) ? tableHashTimes.get(pivot) : new HashMap<Integer,Map<Double,Long>>();
      for (int nbvars_id = 0; nbvars_id < nbNVars; nbvars_id++) {
        final int nbVars = new int[]{2,3,4,5}[nbvars_id];
        System.out.println(" | | vars = " + nbVars);
        final Map<Double,Long> varMap = (pivotMap.containsKey(nbVars)) ? pivotMap.get(nbVars) : new HashMap<Double,Long>();
        for (int proba_id = 0; proba_id < nbProbas; proba_id++) {
          final int invProba = new int[]{16,2,4,8,32,64,128,256}[proba_id];
          final double proba = 1/(double)invProba;
          if (!varMap.containsKey(proba)) {
            final double probaFactor = (double)invProba*pivot;
            final Sampler sampler = new TableHashSampling(pivot, nbVars, proba, modGen, random);
            varMap.put(proba, getTimeSampler(sampler));
          }
          System.out.println(" | | | proba = 1/" + invProba + " -> " + (varMap.get(proba) != null ? varMap.get(proba)/1000000 + "ms" : "-"));
        }
        pivotMap.put(nbVars, varMap);
      }
      tableHashTimes.put(pivot, pivotMap);
    }
    if (doDichotomy) {
      System.out.println("Table Dichotomy");
      final Map<Integer,Map<Integer,Map<Double,Long> >> tableHashDichotomyTimes = data.getTableHashDichotomy();
      for (int pivot_id = 0; pivot_id < nbPivots; pivot_id++) {
        final int pivot = new int[]{2,4,8,16,32,64,128,256}[pivot_id];
        System.out.println(" | pivot = " + pivot);
        final Map<Integer,Map<Double,Long>> pivotMap = (tableHashDichotomyTimes.containsKey(pivot)) ? tableHashDichotomyTimes.get(pivot) : new HashMap<Integer,Map<Double,Long>>();
        for (int nbvars_id = 0; nbvars_id < nbNVars; nbvars_id++) {
          final int nbVars = new int[]{2,3,4,5}[nbvars_id];
          System.out.println(" | | vars = " + nbVars);
          final Map<Double,Long> varMap = (pivotMap.containsKey(nbVars)) ? pivotMap.get(nbVars) : new HashMap<Double,Long>();
          for (int proba_id = 0; proba_id < nbProbas; proba_id++) {
            final int invProba = new int[]{2,4,8,16,32,64,128,256}[proba_id];
            final double proba = 1/(double)invProba;
            if (!varMap.containsKey(proba)) {
              final double probaFactor = (double)invProba*pivot;
              final Sampler sampler = new TableHashDichotomySampling(pivot, nbVars, proba, modGen, random);
              varMap.put(proba, getTimeSampler(sampler));
            }
            System.out.println(" | | | proba = 1/" + invProba + " -> " + (varMap.get(proba) != null ? varMap.get(proba)/1000000 + "ms" : "-"));
          }
          pivotMap.put(nbVars, varMap);
        }
        tableHashDichotomyTimes.put(pivot, pivotMap);
      }
    }
    if (noPropagation) {
      System.out.println("Table No Propagation");
      final Map<Integer,Map<Integer,Map<Double,Long> >> tableHashNoPropagationTimes = data.getTableHashNoPropagation();
      for (int pivot_id = 0; pivot_id < nbPivots; pivot_id++) {
        final int pivot = new int[]{16,2,4,8,32,64,128,256}[pivot_id];
        System.out.println(" | pivot = " + pivot);
        final Map<Integer,Map<Double,Long>> pivotMap = (tableHashNoPropagationTimes.containsKey(pivot)) ? tableHashNoPropagationTimes.get(pivot) : new HashMap<Integer,Map<Double,Long>>();
        for (int nbvars_id = 0; nbvars_id < nbNVars; nbvars_id++) {
          final int nbVars = new int[]{2,3,4,5}[nbvars_id];
          System.out.println(" | | vars = " + nbVars);
          final Map<Double,Long> varMap = (pivotMap.containsKey(nbVars)) ? pivotMap.get(nbVars) : new HashMap<Double,Long>();
          for (int proba_id = 0; proba_id < nbProbas; proba_id++) {
            final int invProba = new int[]{2,4,8,16,32,64,128,256}[proba_id];
            final double proba = 1/(double)invProba;
            if (!varMap.containsKey(proba)) {
              final double probaFactor = (double)invProba*pivot;
              final Sampler sampler = new TableHashSamplingNoPropagation(pivot, nbVars, proba, modGen, random);
              varMap.put(proba, getTimeSampler(sampler));
            }
            System.out.println(" | | | proba = 1/" + invProba + " -> " + (varMap.get(proba) != null ? varMap.get(proba)/1000000 + "ms" : "-"));
          }
          pivotMap.put(nbVars, varMap);
        }
        tableHashNoPropagationTimes.put(pivot, pivotMap);
      }
    }
    /*HashMod*/
    if (doHashMod) {
      final Map<Integer,Long> hashKDtimes = data.getHashKDtables();
      System.out.println("HashMod");
      for (int pivot_id = 0; pivot_id < nbPivots; pivot_id++) {
        final int pivot = new int[]{16,2,4,8,32,64,128,256}[pivot_id];
        if (!hashKDtimes.containsKey(pivot)) {
          final Sampler sampler = new HashModKDSampling(pivot, modGen, random);
          System.out.println("do it");
          hashKDtimes.put(pivot, getTimeSampler(sampler));
        }
        System.out.println(" | pivot = " + pivot + " -> " + (hashKDtimes.get(pivot) != null ? hashKDtimes.get(pivot)/1000000 +"ms": "-"));
      }
    }
  }

  private SamplingData<Long> initialSamplerTimes() {
    Sampler randomSampling = new RandomStrategySampling(modGen, random);
    long time = System.nanoTime();
    boolean noTimeout = true;
    for (int i = 0; i < nbSamples && noTimeout; i++) {
      final Solution sol = randomSampling.sample(maxTime);
      if (sol == null)
        noTimeout = false;
    }
    final long randomTime = (System.nanoTime()-time)/nbSamples;
    return new SamplingData<Long>(noTimeout ? randomTime : null, new HashMap<Integer,Map<Integer,Map<Double,Long> >>(), new HashMap<Integer,Map<Integer,Map<Double,Long> >>(), new HashMap<Integer,Map<Integer,Map<Double,Long> >>(), new HashMap<Integer,Long>(), new HashMap<Integer,Long>());
  }

  private Long getTimeSampler(final Sampler sampler) {
    long time = System.nanoTime();
    boolean noTimeout = true;
    for (int i = 0; i < nbSamples && noTimeout; i++) {
      final Solution sol = sampler.sample(maxTime);
      if (sol == null)
        noTimeout = false;
    }
    return noTimeout ? (System.nanoTime()-time)/nbSamples : null;
  }

  private SamplingData<Long> getData() {
    File file = new File(fileName);
    if (file.exists()) {
      try {
        ObjectMapper mapper = new ObjectMapper();
        TypeReference<SamplingData<Long>> typeRef = new TypeReference<SamplingData<Long>>() {};
        return mapper.readValue(file, typeRef);
      } catch (Exception e) {
        e.printStackTrace();
        System.exit(1);
        return null;
      }
    }
    else
      return initialSamplerTimes();
  }

  private void saveData(final SamplingData<Long> data) {
    try {
      ObjectWriter mapper = new ObjectMapper().writerWithDefaultPrettyPrinter();
      mapper.writeValue(new File(fileName), data);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }









  
}

