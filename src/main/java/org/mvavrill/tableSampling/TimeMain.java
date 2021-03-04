package org.mvavrill.tableSampling;

import org.mvavrill.tableSampling.models.*;
import org.mvavrill.tableSampling.sampler.*;
import org.mvavrill.tableSampling.memoizedData.SamplingData;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.io.File;
import java.util.Random;
import java.util.Map;
import java.util.HashMap;

public class TimeMain {

  private final String fileName;
  private final int nbPivots;
  private final int nbNVars;
  private final int nbProbas;
  private final int nbSamples;
  private final Random random;

  public TimeMain(final String fileName, final int nbPivots, final int nbNVars, final int nbProbas, final int nbSamples, final Random random) {
    this.fileName = fileName;
    this.nbPivots = nbPivots;
    this.nbNVars = nbNVars;
    this.nbProbas = nbProbas;
    this.nbSamples = nbSamples;
    this.random = random;
  }

  public void exec() {
    final Map<String,SamplingData<Long>> data = getData();
    updateModelTimes(new NQueens(12), data);
    updateModelTimes(new Megane(), data);
    updateModelTimes(new FznLoader("csplibmodels/oc-roster/oc-roster-4s-10d.czn", 2), data);
    //updateModelTimes(new FznLoader("csplibmodels/oc-roster/oc-roster-4s-23d.czn"), data);
    saveData(data);
  }

  private void updateModelTimes(final ModelGenerator modGen, final Map<String,SamplingData<Long>> data) {
    System.out.println(modGen.getName());
    SamplingData<Long> dataModel = (data.containsKey(modGen.getName())) ? data.get(modGen.getName()) : initialSamplerTimes(modGen);
    final Map<Integer,Map<Integer,Map<Double,Long> >> tableHashTimes = dataModel.getTableHash();
    for (int pivot_id = 0; pivot_id < nbPivots; pivot_id++) {
      final int pivot = new int[]{2,4,8,16,32,64}[pivot_id];
      System.out.println(" | pivot = " + pivot);
      final Map<Integer,Map<Double,Long>> pivotMap = (tableHashTimes.containsKey(pivot)) ? tableHashTimes.get(pivot) : new HashMap<Integer,Map<Double,Long>>();
      for (int nbvars_id = 0; nbvars_id < nbNVars; nbvars_id++) {
        final int nbVars = new int[]{2,3,4,5}[nbvars_id];
        System.out.println(" | | vars = " + nbVars);
        final Map<Double,Long> varMap = (pivotMap.containsKey(nbVars)) ? pivotMap.get(nbVars) : new HashMap<Double,Long>();
        for (int proba_id = 0; proba_id < nbProbas; proba_id++) {
          final int invProba = new int[]{2,4,8,16,32,64,128,256}[proba_id];
          final double proba = 1/(double)invProba;
          final double probaFactor = (double)invProba*pivot;
          final Sampler sampler = new TableHashDichotomySampling(pivot, nbVars, proba, modGen, random);
          if (!varMap.containsKey(proba)) {
            varMap.put(proba, getTimeSampler(sampler));
          }
          System.out.println(" | | | " + sampler.getSamplerName() + " -> " + varMap.get(proba)/1000000 + "ms");
        }
        pivotMap.put(nbVars, varMap);
      }
      tableHashTimes.put(pivot, pivotMap);
    }
    data.put(modGen.getName(), dataModel);
  }

  private SamplingData<Long> initialSamplerTimes(final ModelGenerator modGen) {
    Sampler randomSampling = new RandomStrategySampling(modGen, random);
    long time = System.nanoTime();
    for (int i = 0; i < nbSamples; i++)
      randomSampling.sample();
    final long randomTime = (System.nanoTime()-time)/nbSamples;
    return new SamplingData<Long>(randomTime, getTimeSampler(new RandomStrategySampling(modGen, random)), new HashMap<Integer,Map<Integer,Map<Double,Long> >>());
  }

  private long getTimeSampler(final Sampler sampler) {
    long time = System.nanoTime();
    sampler.sampleMultiple(nbSamples);
    return (System.nanoTime()-time)/nbSamples;
  }

  private Map<String,SamplingData<Long>> getData() {
    File file = new File(fileName);
    if (file.exists()) {
      try {
        ObjectMapper mapper = new ObjectMapper();
        TypeReference<HashMap<String, SamplingData<Long>>> typeRef = new TypeReference<HashMap<String, SamplingData<Long>>>() {};
        return mapper.readValue(file, typeRef);
      } catch (Exception e) {
        e.printStackTrace();
        System.exit(1);
      }
    }
    else
      return new HashMap<String,SamplingData<Long>>();
    return null; // Impossible to get here because of if-then-else and exit on error catch
  }

  private void saveData(final Map<String,SamplingData<Long>> data) {
    try {
      ObjectWriter mapper = new ObjectMapper().writerWithDefaultPrettyPrinter();
      mapper.writeValue(new File(fileName), data);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }









  
}

