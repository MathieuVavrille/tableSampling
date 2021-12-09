package org.mvavrill.tableSampling.memoizedData;

public class MemoizedBound {
  private Integer bound;
  private Integer nbSolutions;
  private Long timeToEnumerate;

  public MemoizedBound() {}

  public MemoizedBound(final Integer bound, final Integer nbSolutions, final Long timeToEnumerate) {
    this.bound = bound;
    this.nbSolutions = nbSolutions;
    this.timeToEnumerate = timeToEnumerate;
  }

  public Integer getBound() {
    return bound;
  }

  public Integer getNbSolutions() {
    return nbSolutions;
  }

  public Long getTimeToEnumerate() {
    return timeToEnumerate;
  }
}
