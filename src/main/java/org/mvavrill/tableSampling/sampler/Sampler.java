package org.mvavrill.tableSampling.sampler;

import org.mvavrill.tableSampling.models.ModelGenerator;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.variables.IntVar;

import java.util.Random;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public abstract class Sampler {

  protected final ModelGenerator modGen;
  protected final Random random;

  public Sampler(final ModelGenerator modGen, final Random random) {
    this.modGen = modGen;
    this.random = random;
  }

  public Solution sample() {
    return sample(Long.MAX_VALUE);
  }

  public abstract Solution sample(final long maxTime);

  public abstract String getSamplerName();

  public String getModelGeneratorName() {
    return modGen.getName();
  }
  
  public List<Solution> sampleMultiple(final int nbSamples, final long maxTime) {
    List<Solution> sols = new ArrayList<Solution>();
    for (int i = 0; i < nbSamples; i++)
      sols.add(sample(maxTime));
    return sols;
  }
  
  public ModelGenerator getModelGenerator() {
    return modGen;
  }

  public Random getRandom() {
    return random;
  }
  
  /*public static List<Integer> getSol(final IntVar[] vars) {
    return Arrays.stream(vars).map(IntVar::getValue).collect(Collectors.toList());
    }*/
}
