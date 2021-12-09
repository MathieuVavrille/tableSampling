package org.mvavrill.tableSampling.memoizedData;

import java.util.List;

public class MemoizedData {
  private Long timeToOptimal;
  private Long timeToProof; // redundant for sat problems
  private Boolean isSat;
  private List<MemoizedBound> bounds;

  public MemoizedData() {}

  public MemoizedData(final Long timeToOptimal, final Long timeToProof, final Boolean isSat, final List<MemoizedBound> bounds) {
    this.timeToOptimal = timeToOptimal;
    this.timeToProof = timeToProof;
    this.isSat = isSat;
    this.bounds = bounds;
  }

  public Long getTimeToOptimal() {
    return timeToOptimal;
  }

  public Long getTimeToProof() {
    return timeToProof;
  }

  public Boolean getIsSat() {
    return isSat;
  }

  public List<MemoizedBound> getBounds() {
    return bounds;
  }
}
