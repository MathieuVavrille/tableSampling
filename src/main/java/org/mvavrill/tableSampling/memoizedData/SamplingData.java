package org.mvavrill.tableSampling.memoizedData;

import java.util.Map;
import java.util.HashMap;

/**
 * Stores the result of computations on different models, and with different parameters of pivot, number of variables and probability.
 * The generic type allows to store different data. For example, T = double[] when testing the pvalue, and T = long when testing the time.
 * 
 * @author Mathieu Vavrille
 */
public class SamplingData<T> {
  private T random;
  private T randomMultiple;
  private Map<Integer,Map<Integer,Map<Double,T> >> tableHash; // The map is pivot -> vars -> proba -> time

  public SamplingData() {
    /*random = -1;
    randomMultiple = -1;
    tableHash = new HashMap<Integer,Map<Integer,Map<Double,Long>>>();*/
  }

  public SamplingData(final T random, final T randomMultiple, final Map<Integer,Map<Integer,Map<Double,T> >> tableHash) {
    this.random = random;
    this.randomMultiple = randomMultiple;
    this.tableHash = tableHash;
  }

  public T getRandom() {
    return random;
  }

  public T getRandomMultiple() {
    return randomMultiple;
  }

  public Map<Integer,Map<Integer,Map<Double,T> >> getTableHash() {
    return tableHash;
  }
}
