/*
 * mini-cp is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License  v3
 * as published by the Free Software Foundation.
 *
 * mini-cp is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY.
 * See the GNU Lesser General Public License  for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with mini-cp. If not, see http://www.gnu.org/licenses/lgpl-3.0.en.html
 *
 * Copyright (c)  2018. by Laurent Michel, Pierre Schaus, Pascal Van Hentenryck
 *
 * mini-cpbp, replacing classic propagation by belief propagation 
 * Copyright (c)  2019. by Gilles Pesant
 */

package org.mvavrill.tableSampling;

import org.mvavrill.tableSampling.zpz.*;


import org.chocosolver.memory.IStateInt;
import org.chocosolver.memory.IStateBool;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.events.IntEventType;
import org.chocosolver.solver.variables.events.PropagatorEventType;
import org.chocosolver.util.ESat;
import org.chocosolver.util.iterators.DisposableValueIterator;


import java.util.Arrays;
import java.util.Iterator;
import java.util.BitSet;
import java.util.stream.IntStream;
import java.security.InvalidParameterException;

/**
 * System of linear equations in modular arithmetic: Ax = b (mod p)
 * Requires p to be prime, thus defining a finite field F_p
 */
public class LinEqSystemModP extends Propagator<IntVar> {
  private int[][] A;
  private int p; // the prime modulus
  private int m, n, nNonparam;
  private int[] inverse; // multiplicative inverse (reciprocal) of each nonzero element of F_p
  //Constraint[] linEq; // the SumModP constraint associated to each linear equation of the reduced row echelon form
  private int[] unBounds;
  private IStateInt nUnBounds;
  private int[] paramIdx; // indices of the parametric variables in the RRE form
  private static final int nTuplesThreshold = 1000; // apply DC when parametric vars search space <= threshold
  private IStateBool tableFiltering; // true iff we now apply DC (once the threshold has been reached)
  private int[] rhsBound;
  private int[] tuple; // to build tuples during the tuple enumeration
  private int[][] Tuples; // to accumulate tuples
  private int nTuples; // number of tuples
  // data structures borrowed from TableCT implementation
  private int[] ofs; //offsets for each variable's domain
  //supports[i][v] is the set of tuples supported by vars[i]=v
  private BitSet[][] supports;
  //supportedTuples is the set of tuples supported by the current domains of the variables
  private BitSet supportedTuples;
  private BitSet supporti;

  public static LinEqSystemModP fromZpZEquations(final IntVar[] vars, final ZpZEquation... equations) {
    final ZpZEquationSystem eqSys = new ZpZEquationSystem(equations);
    final int[][] A = eqSys.getCoefficientsMatrix();
    final int[] b = eqSys.getRhs();
    final int p = eqSys.getZpZ().p;
    /*for (int i = 0; i < b.length; i++) {
      System.out.println(Arrays.toString(A[i]) + " = " + b[i]);
    }
    System.out.println(p);*/
    return new LinEqSystemModP(vars, A, b, p);
  }

  
  /**
   * Creates a "system of linear equations modulo p" constraint, with p a prime number.
   * <p> This constraint holds iff
   * {@code Ax == b (mod p)}.
   *
   * @param A the mxn matrix of coefficients
   * @param vars the column vector of n variables
   * @param b the column vector of m rhs values
   * @param p the prime modulus
   */
  public LinEqSystemModP(IntVar[] vars, int[][] A, int[] b, int p) {
    super(vars, PropagatorPriority.QUADRATIC, false);
    n = vars.length;
    m = b.length;
    this.p = p;
    assert( p > 1 );
    assert( A.length == m );
    //StateManager sm = getSolver().getStateManager();
    tableFiltering = this.model.getEnvironment().makeBool(false);

    // precompute the reciprocals used in the Extended Gauss-Jordan Elimination algorithm (also allows us to verify that p is prime)
    inverse = new int[p];
    inverse[1] = 1;
    inverse[p-1] = p-1;
    for (int i=2; i<p-1; i++)
      inverse[i] = -1;
    for (int i=2; i<p-1; i++)
      if (inverse[i] == -1) {
        inverse[i] = reciprocal(i); // every element of the field (except 0) has a multiplicative inverse
        inverse[inverse[i]] = i;
      }

    unBounds = IntStream.range(0, n).toArray();

    // Collect bound vars
    int nU = n;
    for (int i = nU - 1; i >= 0; i--) {
      int idx = unBounds[i];
      if (vars[idx].isInstantiated()) {
        unBounds[i] = unBounds[nU - 1]; // Swap the variables
        unBounds[nU - 1] = idx;
        nU--;
      }
    } 
    nUnBounds = this.model.getEnvironment().makeInt(nU);
    
    // map all constants to their canonical representative (from the set {0,1,...,p-1}) in the congruence relation's equivalence class of finite field F_p, while taking into account bound variables
    this.A = new int[m][n+1]; // augmented coefficient matrix
    for (int i=0; i<m; i++) {
      assert( A[i].length == n );
      this.A[i][n] = b[i];
      for (int j=0; j<n; j++) {
        this.A[i][j] = Math.floorMod(A[i][unBounds[j]],p);
      }
      this.A[i][n] = Math.floorMod(this.A[i][n],p);
    }

    // put A in reduced row echelon form
    GaussJordanElimination(m,nU);

    /* 
       System.out.println("after GJ Elim");
       for (int i=0; i<m; i++) {
       System.out.println(Arrays.toString(this.A[i]));
       }
    */

    rhsBound = new int[nNonparam];
    tuple = new int[nU];
    Tuples = new int[nTuplesThreshold][nU];
    ofs = new int[nU];
    supportedTuples = new BitSet(nTuplesThreshold);
    supporti = new BitSet(nTuplesThreshold);
    // Allocate supportedByVarVal
    supports = new BitSet[nU][];
    int maxDsize = 0;
    for (int i = 0; i < nU; i++) {
      if (vars[unBounds[i]].getUB() - vars[unBounds[i]].getLB() + 1 > maxDsize)
        maxDsize = vars[unBounds[i]].getUB() - vars[unBounds[i]].getLB() + 1;
    }
    for (int i = 0; i < nU; i++) {
      supports[i] = new BitSet[maxDsize];
      for (int j = 0; j < supports[i].length; j++)
        supports[i][j] = new BitSet();
    }

    // identify the parametric variables (useful for branching)
    paramIdx = new int[nU-nNonparam];
    // 	System.out.print("param vars: ");
    for (int j=0; j<nU-nNonparam; j++) {
      paramIdx[j] = unBounds[nNonparam+j];
      // 	    System.out.print(vars[paramIdx[j]].getName()+" ");
    }
    /*
      System.out.print("\n nonparam vars: ");
      for (int j=0; j<nNonparam; j++) {
      System.out.print(x[unBounds[j]].getName()+" ");
      }
      System.out.println();
    */
    /* 
    // post a SumModP constraint for each equation in the reduced row echelon form
    linEq = new Constraint[nNonparam];
    for (int i=0; i<nNonparam; i++) {
    IntVar[] y = new IntVar[nU-nNonparam+1];
    int[] c = new int[nU-nNonparam+1];
    y[0] = vars[unBounds[i]]; // the nonparametric variable
    c[0] = 1;
    for (int j=1; j<c.length; j++) { // the parametric variables
    y[j] = vars[unBounds[nNonparam+j-1]];
    c[j] = this.A[i][unBounds[nNonparam+j-1]];
    }
    linEq[i] = sumModP(c,y,this.A[i][nU],p);
    cp.post(linEq[i]);
    }
    */

    //    	setExactWCounting(true);
  }

  /**
   * Returns the parametric variables in the reduced row echelon form.
   *
   * @return parametric variables
   */
  public IntVar[] getParamVars() {
    IntVar[] paramVars = new IntVar[paramIdx.length];
    for (int i=0; i<paramIdx.length; i++)
      paramVars[i] = vars[paramIdx[i]];
    return paramVars;
  }

  /**
   * Returns an estimate of the number of solutions to this system based on the exact numbers of each linear equality.
   *
   * @return number of solutions
   */
  /*public double getNbSolns() {
    double avgNbSolns = 0;
    int nU = nUnBounds.get();
    // compute the average count over all equations
    for (int i=0; i<nNonparam; i++) {
      double nbSolns = ((SumModP) linEq[i]).getNbSolns();
      // take into account parametric variables with a zero coefficient
      for (int j=1; j<nU-nNonparam+1; j++)
        if (this.A[i][nNonparam+j-1] == 0)
          nbSolns *= vars[unBounds[nNonparam+j-1]].getDomainSize();
      //  	    System.out.println("eq "+i+" has "+nbSolns+" solutions");
      avgNbSolns += nbSolns;
    }
    avgNbSolns /= nNonparam;
    // BIG WORKING HYPOTHESIS: nb of solutions is an exponential function of the number of variables 
    return Math.pow(p*avgNbSolns,((double) nU)/((double) (nU-nNonparam+1))) / Math.pow(p,nNonparam);
    }*/

  /**
   * Computes the multiplicative inverse of nonzero element e in finite field F_p (extended GCD algorithm)
   *
   * @param e the element
   * @return the multiplicative inverse of e
   */
  private int reciprocal(int e) {
    assert( e>0 && e<p );
    int r = p;
    int newr = e;
    int t = 0;
    int newt = 1;
    while (newr != 0) {
      int tmp1 = r % newr;
      int tmp2 = t - r / newr * newt;
      r = newr;
      newr = tmp1;
      t = newt;
      newt = tmp2;
    }
    if (r == 1)
      return (t<0 ? t+p : t);
    else
      throw new InvalidParameterException("Modulus p="+p+" is not prime");
  }

  /**
   * Performs Gauss-Jordan Elimination on the mxn system of linear equations in order to simplify it into reduced row echelon form.
   * Note: all elements of A are assumed to lie in the range 0..p-1
   *
   */
  private void GaussJordanElimination(int m, int n) {
    int h = 0; // pivot row 
    int k = 0; // pivot column
    while ( (h < m) && (k < n) ) {
      // Find the k-th pivot
      int pivotCol = k;
      while (pivotCol < n) {
        int pivotRow = h;
        while ( (pivotRow < m) && (A[pivotRow][pivotCol] == 0) )
          pivotRow++;
        if (pivotRow < m) {
          if (pivotRow > h)
            swapRows(h,pivotRow);
          break;
        } else
          pivotCol++; // no pivot in this column, go to next one
      }
      if (pivotCol == n)
        break; // end of procedure
      if (pivotCol > k)
        swapCols(k,pivotCol);
      // transform pivot row to lead with a unit coefficient
      int inv = inverse[A[h][k]];
      A[h][k] = 1;
      for (int j=k+1; j<=n; j++) {
        A[h][j] = Math.floorMod( A[h][j] * inv, p );
      }
      for (int i=h+1; i<m; i++) { // for all rows below pivot
        int f = A[i][k];
        A[i][k] = 0; // element in same column as pivot is set to 0
        for (int j=k+1; j<=n; j++) { // transform the other elements of that row
          A[i][j] = Math.floorMod( A[i][j] - A[h][j] * f, p );
        }
      }
      for (int i=0; i<h; i++) { // and for all rows above pivot
        int f = A[i][k];
        A[i][k] = 0; // element in same column as pivot is set to 0
        for (int j=k+1; j<=n; j++) { // transform the other elements of that row
          A[i][j] = Math.floorMod( A[i][j] - A[h][j] * f, p );
        }
      }
      h++;
      k++;
    }
    nNonparam = k;
  }

  private void swapRows(int i, int j) {
    int[] tmp = A[j];
    A[j] = A[i];
    A[i] = tmp;
  }
    
  private void swapCols(int i, int j) {
    for (int k=0; k<m; k++) {
      int tmp = A[k][j];
      A[k][j] = A[k][i];
      A[k][i] = tmp;
    }
    int tmp = unBounds[j];
    unBounds[j] = unBounds[i];
    unBounds[i] = tmp;
  }

  @Override
  public void propagate(int evtmask) throws ContradictionException {
    if(PropagatorEventType.isFullPropagation(evtmask)){
      // consider potential rows with all zero coefficients
      for (int i=nNonparam; i<m; i++)
        if (this.A[i][A[i].length-1] != 0) // rhs must be zero as well in order for the system to be satisfiable
          this.fails();
    }
    if (!tableFiltering.get()) {
      // so far parametric search space has been too large to set up table filtering
      int nU = nUnBounds.get();
      double nTuplesUB = 1; // upper bound on nb combinations of values for parametric vars
      for (int i = nU - 1; i >= nNonparam; i--) { // restrict to parametric vars
        int idx = unBounds[i];
        if (vars[idx].isInstantiated()) {
          unBounds[i] = unBounds[nU - 1]; // Swap the variables
          unBounds[nU - 1] = idx;
          nU--;
        }
        else {
          nTuplesUB *= vars[idx].getDomainSize();
        }
      }
      nUnBounds.set(nU); // Warning: this count ignores bound _non_parametric variables

      if (nTuplesUB > nTuplesThreshold) // do nothing further until the number of tuples to enumerate is tractable
        return;

      // otherwise it is now time to set up table filtering
      tableFiltering.set(true);
      // TODO? switch to domain events for all vars
      // TODO: map domain values to their canonical rep
      // enumerate tuples over parametric variables and accumulate them in Tuples

      for (int i = 0; i < nNonparam; i++) {
        int sum = A[i][A[i].length - 1]; // rhs
        for (int j = nUnBounds.get(); j < A[i].length - 1; j++) { // bound parametric vars
          assert (vars[unBounds[j]].isInstantiated());
          sum -= A[i][unBounds[j]] * vars[unBounds[j]].getValue();
        }
        rhsBound[i] = sum;
      }
      nTuples = 0;
      paramEnum(1);
      if (nTuples==0)
        this.fails();	    
      for (int i = 0; i < nU; i++) {
        ofs[i] = vars[unBounds[i]].getLB(); // offsets map the variables' domain to start at 0 for supports[][]
      }
      // Clear supports
      for (int j = 0; j < nU; j++) {
        for (int k = 0; k < supports[j].length; k++) {
          supports[j][k].clear();
        }
      }
      // Set values in supportedByVarVal, which contains all the tuples supported by each var-val pair
      for (int i = 0; i < nTuples; i++) { //i is the index of the tuple (in table)
        for (int j = 0; j < nU; j++) { //j is the index of the current variable (in vars)
          if (vars[unBounds[j]].contains(Tuples[i][j])) {
            supports[j][Tuples[i][j] - ofs[j]].set(i);
          }
        }
      }
    }
    // perform table filtering (borrowed from TableCT.propagate())
    supportedTuples.set(0, nTuples); // set them all to true
    if (supportedTuples.length() > nTuples)
      supportedTuples.clear(nTuples, supportedTuples.length()); // disregard tuples from former table
    int nU = nUnBounds.get(); // WARNING!! assumes no updates to unBounds have been done since tableFiltering was set
    for (int i = 0; i < nU; i++) {
      supporti.clear(); // set them all to false
      int[] domainValues = getValueArray(vars[unBounds[i]]);
      for (int j = 0; j < domainValues.length; j++) {
        supporti.or(supports[i][domainValues[j]-ofs[i]]);
      }
      supportedTuples.and(supporti);
    }
    if (supportedTuples.isEmpty())
      this.fails();	    
    for (int i = 0; i < nU; i++) {
      int[] domainValues = getValueArray(vars[unBounds[i]]);
      for (int j = 0; j < domainValues.length; j++) {
        // The condition for removing the setValue v from vars[i] is to check if
        // there is no intersection between supportedTuples and the support[i][v]
        int v = domainValues[j];
        if (!supports[i][v-ofs[i]].intersects(supportedTuples)) {
          vars[unBounds[i]].removeValue(v,this);
        }
      }
    }
  }

  public int[] getValueArray(final IntVar x) {
    final int[] domain = new int[x.getDomainSize()];
    DisposableValueIterator iterator = x.getValueIterator(true);
    int cpt = 0;
    while(iterator.hasNext())
      domain[cpt++] = iterator.next();
    return domain;
  }

  /**
   * Recursive algorithm enumerating tuples over unbound parametric variables
   *
   * @param r the rank of the next parametric variable to enumerate over
   */
  private void paramEnum(int r) {
    if (r <= nUnBounds.get()-nNonparam) {
      int idx = nNonparam+r-1;
      int ub = vars[unBounds[idx]].getUB();
      for(int v = vars[unBounds[idx]].getLB(); v <= ub; v = vars[unBounds[idx]].nextValue(v)) {
        tuple[idx] = v;
        paramEnum(r + 1);
      }
    }
    else { // complete tuple: check consistency using nonparametric variables
      paramEnum2();
    }
  }

  private void paramEnum2() {
    int i;
    for (i = 0; i < nNonparam; i++) {
      int sum = rhsBound[i]; // rhs
      /*int sum = A[i][A[i].length - 1]; // rhs
      for (int j = nUnBounds.get(); j < A[i].length - 1; j++) { // bound parametric vars
        assert (vars[unBounds[j]].isInstantiated());
        sum -= A[i][unBounds[j]] * vars[unBounds[j]].getValue();
        }*/
      for (int j = nNonparam; j < nUnBounds.get(); j++) { // unbound parametric vars
        sum -= A[i][unBounds[j]] * tuple[j];
      }
      sum = Math.floorMod(sum-vars[unBounds[i]].getLB(), p)+vars[unBounds[i]].getLB();
      if (!vars[unBounds[i]].contains(sum))
        break;
      tuple[i] = sum;
    }
    if (i == nNonparam) { // tuple is consistent: add to Tuples
      for (int j = 0; j < nUnBounds.get(); j++) {
        Tuples[nTuples][j] = tuple[j];
      }
      nTuples++;
    }
  }

  @Override
  public int getPropagationConditions(int vIdx) {
    // From now on only track bound *parametric* vars
    for (int i : unBounds)
      if (i == vIdx)
        return IntEventType.INSTANTIATE.getMask();
    return IntEventType.VOID.getMask();
  }
  
  public ESat isEntailed() { // Does not work, because the constraint will be set passive if converted to table
    return ESat.UNDEFINED;
  }
}
