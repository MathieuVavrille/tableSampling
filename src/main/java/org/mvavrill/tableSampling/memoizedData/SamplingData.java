package org.mvavrill.tableSampling.memoizedData;

import java.util.Map;
import java.util.HashMap;

public class SamplingData<T> {
  private T randomVarDom;
  private Map<Integer,Map<Integer,Map<Double,T> >> tableHash; // The map is pivot -> vars -> proba -> T
  private Map<Integer,Map<Integer,Map<Double,T> >> tableHashNoPropagation; // The map is pivot -> vars -> proba -> T
  private Map<Integer,Map<Integer,Map<Double,T> >> tableHashDichotomy; // The map is pivot -> vars -> proba -> T
  private Map<Integer, T> hashKDtables; // The map is pivot -> T
  private Map<Integer, T> hashKDwatched; // The map is pivot -> T

  public SamplingData() {}

  public SamplingData(final T randomVarDom, final Map<Integer,Map<Integer,Map<Double,T> >> tableHash, final Map<Integer,Map<Integer,Map<Double,T> >> tableHashNoPropagation, final Map<Integer,Map<Integer,Map<Double,T> >> tableHashDichotomy, final Map<Integer,T> hashKDtables, final Map<Integer,T> hashKDwatched) {
    this.randomVarDom = randomVarDom;
    this.tableHash = tableHash;
    this.tableHashNoPropagation = tableHashNoPropagation;
    this.tableHashDichotomy = tableHashDichotomy;
    this.hashKDtables = hashKDtables;
    this.hashKDwatched = hashKDwatched;
  }

  public T getRandomVarDom() {
    return randomVarDom;
  }

  public Map<Integer,Map<Integer,Map<Double,T> >> getTableHash() {
    return tableHash;
  }

  public Map<Integer,Map<Integer,Map<Double,T> >> getTableHashNoPropagation() {
    return tableHashNoPropagation;
  }

  public Map<Integer,Map<Integer,Map<Double,T> >> getTableHashDichotomy() {
    return tableHashDichotomy;
  }

  public Map<Integer,T> getHashKDtables() {
    return hashKDtables;
  }

  public Map<Integer,T> getHashKDwatched() {
    return hashKDwatched;
  }
}
