package org.mvavrill.tableSampling.plotting;

import org.javatuples.Pair;

import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class PlottingUtils {

  /* -------- Conversion of arrays -------- */
  public static <T> List<String> convertToString(final List<T> values) {
    return values.stream().map(T::toString).collect(Collectors.toList());
  }
  
  public static List<String> convertToString(final long[] values) {
    return convertToString(Arrays.stream(values).boxed().collect(Collectors.toList()));
  }
  
  public static List<String> convertToString(final int[] values) {
    return convertToString(Arrays.stream(values).boxed().collect(Collectors.toList()));
  }
  
  public static List<String> convertToString(final double[] values) {
    return convertToString(Arrays.stream(values).boxed().collect(Collectors.toList()));
  }
  
  public static <T> List<String> convertToString(final T[] values) {
    return convertToString(Arrays.stream(values).map(T::toString).collect(Collectors.toList()));
  }

  /* -------- Analysis of data -------- */
  public static Pair<List<Long>, List<Integer>> numberOccurences(final long[] values) {
    Map<Long,Integer> valToOccurences = new HashMap<Long,Integer>();
    for (long val : values)
      valToOccurences.put(val, valToOccurences.containsKey(val) ? valToOccurences.get(val)+1 : 1);
    List<Long> distinctValues = new ArrayList<Long>();
    List<Integer> occurences = new ArrayList<Integer>();
    valToOccurences.entrySet().stream()
      .forEach(e -> {distinctValues.add(e.getKey()); occurences.add(e.getValue());});
    return new Pair<List<Long>, List<Integer>>(distinctValues, occurences);
  }
  
  
}
  
