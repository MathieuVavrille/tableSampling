package org.mvavrill.tableSampling;

import org.mvavrill.tableSampling.models.*;
import org.mvavrill.tableSampling.sampler.*;
import org.mvavrill.tableSampling.memoizedData.SamplingData;
import org.mvavrill.tableSampling.plotting.StatisticalTests;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.io.File;
import java.util.Random;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class PvalueMain {

  private final ModelGenerator modGen;
  private final String fileName;
  private final int nbPivots;
  private final int nbNVars;
  private final int nbProbas;
  private final boolean doHashMod;
  private final Random random;

  public PvalueMain(final ModelGenerator modGen, final String fileName, final int nbPivots, final int nbNVars, final int nbProbas, final long maxTime, final boolean doHashMod, final Random random) {
    this.modGen = modGen;
    this.fileName = fileName;
    this.nbPivots = nbPivots;
    this.nbNVars = nbNVars;
    this.nbProbas = nbProbas;
    this.doHashMod = doHashMod;
    this.random = random;
  }

  public void exec() {
    final double bound = 10.0;
    final SamplingData<List<Double>> data = getData(bound);
    updateModelPvalue(data, bound);
    saveData(data);
  }

  private void updateModelPvalue(final SamplingData<List<Double>> data, final double bound) {
    /*Table Hash*/
    final Map<Integer,Map<Integer,Map<Double,List<Double>> >> tableHashPvalues = data.getTableHash();
    for (int pivot_id = 0; pivot_id < nbPivots; pivot_id++) {
      final int pivot = new int[]{2,4,8,16,32,64}[pivot_id];
      System.out.println(" | pivot = " + pivot);
      final Map<Integer,Map<Double,List<Double>>> pivotMap = (tableHashPvalues.containsKey(pivot)) ? tableHashPvalues.get(pivot) : new HashMap<Integer,Map<Double,List<Double>>>();
      for (int nbvars_id = 0; nbvars_id < nbNVars; nbvars_id++) {
        final int nbVars = new int[]{2,3,4,5}[nbvars_id];
        System.out.println(" | | vars = " + nbVars);
        final Map<Double,List<Double>> varMap = (pivotMap.containsKey(nbVars)) ? pivotMap.get(nbVars) : new HashMap<Double,List<Double>>();
        for (int proba_id = 0; proba_id < nbProbas; proba_id++) {
          final int invProba = new int[]{2,4,8,16,32,64,128,256}[proba_id];
          final double proba = 1/(double)invProba;
          final Sampler sampler = new TableHashSampling(pivot, nbVars, proba, modGen, random);
          if (!varMap.containsKey(proba))
            varMap.put(proba, getPvaluesSampler(sampler, bound));
          System.out.println(" | | | " + sampler.getSamplerName() + " -> " + varMap.get(proba).size() + " samples");
        }
        pivotMap.put(nbVars, varMap);
      }
      tableHashPvalues.put(pivot, pivotMap);
    }
    /*HashMod*/
    if (doHashMod) {
      final Map<Integer,List<Double>> hashKDPvalues = data.getHashKDtables();
      for (int pivot_id = 0; pivot_id < nbPivots; pivot_id++) {
        final int pivot = new int[]{50, 40, 30, 20, 10, 5}[pivot_id];
        final Sampler sampler = new HashModKDSampling(pivot, modGen, random);
        if (!hashKDPvalues.containsKey(pivot))
          hashKDPvalues.put(pivot, getPvaluesSampler(sampler, bound));
        System.out.println(" | " + sampler.getSamplerName() + " -> " + hashKDPvalues.get(pivot).size() + " samples");
      }
    }
  }

  private SamplingData<List<Double>> initialSamplerPvalues(final double bound) {
    return new SamplingData<List<Double>>(getPvaluesSampler(new RandomStrategySampling(modGen, random), bound), new HashMap<Integer,Map<Integer,Map<Double,List<Double>> >>(), null, null, new HashMap<Integer,List<Double>>(), null);
  }

  private List<Double> getPvaluesSampler(final Sampler sampler, final Double bound) {
    List<Integer> samples = new ArrayList<Integer>();
    List<Double> pValues = new ArrayList<Double>();
    StatisticalTests statTests = new StatisticalTests(sampler);
    int count = 0;
    while (samples.size() < bound*statTests.getNbSolutions() && count < 20) {
      samples.add(statTests.sample());
      double newPvalue = statTests.pValueChiSquared(samples);
      pValues.add(newPvalue);
      if (newPvalue == 0)
        count++;
      else
        count = 0;
    }
    return pValues;
  }

  private SamplingData<List<Double>> getData(final double bound) {
    File file = new File(fileName);
    if (file.exists()) {
      try {
        ObjectMapper mapper = new ObjectMapper();
        TypeReference<SamplingData<List<Double>> > typeRef = new TypeReference<SamplingData<List<Double>> >() {};
        return mapper.readValue(file, typeRef);
      } catch (Exception e) {
        e.printStackTrace();
        System.exit(1);
        return null;
      }
    }
    else
      return initialSamplerPvalues(bound);
  }

  private void saveData(final SamplingData<List<Double>> data) {
    try {
      ObjectWriter mapper = new ObjectMapper().writerWithDefaultPrettyPrinter();
      mapper.writeValue(new File(fileName), data);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }









  
}

