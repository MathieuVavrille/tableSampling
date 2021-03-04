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

  private final String fileName;
  private final int nbPivots;
  private final int nbNVars;
  private final int nbProbas;
  private final Random random;

  public PvalueMain(final String fileName, final int nbPivots, final int nbNVars, final int nbProbas, final Random random) {
    this.fileName = fileName;
    this.nbPivots = nbPivots;
    this.nbNVars = nbNVars;
    this.nbProbas = nbProbas;
    this.random = random;
  }

  public void exec() {
    final Map<String, SamplingData<List<Double>> > data = getData();
    updateModelPvalue(new FznLoader("/home/varville-m/Documents/these/code/gaussianZpZ/csplibmodels/feature.czn",90), data, 10.0, random);
    updateModelPvalue(new FznLoader("/home/varville-m/Documents/these/code/gaussianZpZ/csplibmodels/oc-roster/oc-roster-4s-10d.czn"),data, 8.0,random);
    updateModelPvalue(new FznLoader("/home/varville-m/Documents/these/code/gaussianZpZ/csplibmodels/oc-roster/oc-roster-4s-10d.czn", 1),data,1.0, random);
    updateModelPvalue(new NQueens(8), data, 20.0, random);
    updateModelPvalue(new NQueens(9), data, 30.0, random);
    saveData(data);
  }

  private void updateModelPvalue(final ModelGenerator modGen, final Map<String,SamplingData<List<Double>> > data, final double bound, final Random random) {
    System.out.println(modGen.getName());
    SamplingData<List<Double>> dataModel = (data.containsKey(modGen.getName())) ? data.get(modGen.getName()) : initialSamplerPvalues(modGen, bound);
    final Map<Integer,Map<Integer,Map<Double,List<Double>> >> tableHashPvalues = dataModel.getTableHash();
    for (int pivot_id = 0; pivot_id < nbPivots; pivot_id++) {
      final int pivot = new int[]{2,4,8,16,32,64}[pivot_id];
      final Map<Integer,Map<Double,List<Double>>> pivotMap = (tableHashPvalues.containsKey(pivot)) ? tableHashPvalues.get(pivot) : new HashMap<Integer,Map<Double,List<Double>>>();
      for (int nbvars_id = 0; nbvars_id < nbNVars; nbvars_id++) {
        final int nbVars = new int[]{2,3,4,5}[nbvars_id];
        System.out.println(" | | vars = " + nbVars);
        final Map<Double,List<Double>> varMap = (pivotMap.containsKey(nbVars)) ? pivotMap.get(nbVars) : new HashMap<Double,List<Double>>();
        for (int proba_id = 0; proba_id < nbProbas; proba_id++) {
          final int invProba = new int[]{2,4,8,16,32,64,128,256}[proba_id];
          final double proba = 1/(double)invProba;
          final Sampler sampler = new TableHashDichotomySampling(pivot, nbVars, proba, modGen, random);
          if (!varMap.containsKey(proba)) {
            varMap.put(proba, getPvaluesSampler(sampler, bound));
          }
          System.out.println(" | | | " + sampler.getSamplerName() + " -> " + varMap.get(proba).size() + " samples");
        }
        pivotMap.put(nbVars, varMap);
      }
      tableHashPvalues.put(pivot, pivotMap);
    }
    data.put(modGen.getName(), dataModel);
  }

  private SamplingData<List<Double>> initialSamplerPvalues(final ModelGenerator modGen, final double bound) {
    return new SamplingData<List<Double>>(getPvaluesSampler(new RandomStrategySampling(modGen, random), bound), new ArrayList<Double>(), new HashMap<Integer,Map<Integer,Map<Double,List<Double>> >>());
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

  private Map<String,SamplingData<List<Double>> > getData() {
    File file = new File(fileName);
    if (file.exists()) {
      try {
        ObjectMapper mapper = new ObjectMapper();
        TypeReference<HashMap<String, SamplingData<List<Double>> >> typeRef = new TypeReference<HashMap<String, SamplingData<List<Double>> >>() {};
        return mapper.readValue(file, typeRef);
      } catch (Exception e) {
        e.printStackTrace();
        System.exit(1);
      }
    }
    else
      return new HashMap<String,SamplingData<List<Double>>>();
    return null; // Impossible to get here because of if-then-else and exit on error catch
  }

  private void saveData(final Map<String, SamplingData<List<Double>> > data) {
    try {
      ObjectWriter mapper = new ObjectMapper().writerWithDefaultPrettyPrinter();
      mapper.writeValue(new File(fileName), data);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }









  
}

