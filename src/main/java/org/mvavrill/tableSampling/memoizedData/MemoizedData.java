package org.mvavrill.tableSampling.memoizedData;

/**
 * A class used to store data about the number of solutions of fzn models (optimisation models), under different bound on the objective.
 * It will then be used to store the data in a JSON file.
 *
 * @author Mathieu Vavrille
 */
public class MemoizedData {
  private int bound;
  private int nbSolutions;

  public MemoizedData() {}

  public MemoizedData(final int bound, final int nbSolutions) {
    this.bound = bound;
    this.nbSolutions = nbSolutions;
  }

  public int getBound() {
    return bound;
  }

  public int getNbSolutions() {
    return nbSolutions;
  }
}
