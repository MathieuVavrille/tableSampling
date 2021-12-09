package org.mvavrill.tableSampling.zpz;

import org.chocosolver.util.iterators.DisposableValueIterator;
import org.chocosolver.util.objects.setDataStructures.iterable.IntIterableRangeSet;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.IntVar;

import java.util.Random;
import java.util.Arrays;

public class ZpZEquation {
  private final ZpZ zpz;
  private int[] coefficients;
  private int constant;

  public ZpZEquation(final ZpZ zpz, int[] coefficients, int constant) {
    this.zpz = zpz;
    this.coefficients = coefficients;
    this.constant = constant;
    if (!this.sanityCheck())
      throw new IllegalStateException("The equation was not generated using zpz");
  }

  private boolean sanityCheck() {
    for (int i = 0; i < coefficients.length; i++)
      if (!zpz.isRepr(coefficients[i]))
        return false;
    return zpz.isRepr(constant);
  }

  public ZpZ getZpZ() {
    return zpz;
  }
  
  public int getCoeff(final int i) {
    return coefficients[i];
  }

  public int[] getCoefficients() {
    return coefficients;
  }

  public int getCst() {
    return constant;
  }

  public int length() {
    return coefficients.length;
  }

  public void mul(final int factor) { // multiplies in place the equation with the given factor
    for (int i = 0; i < coefficients.length; i++) {
      coefficients[i] = zpz.mul(coefficients[i],factor);
    }
    constant = zpz.mul(constant,factor);
  }

  public void subMul(final ZpZEquation eq, final int factor) {
    for (int i = 0; i < coefficients.length; i++) {
      coefficients[i] = zpz.sub(coefficients[i],zpz.mul(eq.coefficients[i],factor));
    }
    constant = zpz.sub(constant,zpz.mul(eq.constant,factor));
  }
  
  public int instantiationOf(final int id, final int[] instantiation) {
    int res = 0;
    for (int i = 0; i < coefficients.length; i++)
      if (i != id)
        res -= coefficients[i] * instantiation[i];
    return zpz.repr(res);
  }
  
  public boolean check(int[] instantiation) {
    int leftValue = 0;
    for (int i = 0; i < coefficients.length; i++)
      leftValue += coefficients[i] * instantiation[i];
    return zpz.repr(leftValue) == constant;
  }

  public int getValueOf(final int varId, final int[] instantiation) {
    int res = constant;
    for (int i = 0; i < coefficients.length; i++) {
      if (i != varId)
        res -= coefficients[i]*instantiation[i];
    }
    return zpz.repr(res);
  }

  public boolean isContradiction() {
    boolean isEmpty = true;
    for (int val : coefficients)
      isEmpty &= val == 0;
    return isEmpty && constant != 0;
  }

  public static ZpZEquation randomEquation(final ZpZ zpz, final int nbCoeffs, final Random random) {
    int[] coefficients = new int[nbCoeffs];
    for (int i = 0; i < nbCoeffs; i++) {
      coefficients[i] = random.nextInt(zpz.p);
    }
    return new ZpZEquation(zpz, coefficients, random.nextInt(zpz.p));
  }

  public String toString() {
    String s = (coefficients.length == 0) ? "0" : "" + coefficients[0] + "X0";
    for (int i = 1; i < coefficients.length; i++) {
      s = s + " + " + coefficients[i] + "X" + i;
    }
    return s + " = " + constant;
  }

  public ZpZEquation clone() {
    return new ZpZEquation(zpz, coefficients.clone(), constant);
  }

  









  
  /** Legacy code */

  public ZpZEquation simplify(final int nbUninstantiatedVars, final Integer[] vars) {
    int[] simplCoeffs = new int[nbUninstantiatedVars];
    int simplConstant = constant;
    int counter = 0;
    for (int i = 0; i < vars.length; i++) {
      if (vars[i] == null) {
        simplCoeffs[counter] = coefficients[i];
        counter++;
      }
      else
        simplConstant = zpz.sub(simplConstant,zpz.mul(zpz.repr(vars[i]),coefficients[i]));
    }
    return new ZpZEquation(zpz, simplCoeffs, simplConstant);
  }

  public void fillInstantiation(int[] instantiation) {
    boolean isAlone = true;
    int nonZeroPosition = -1;
    for (int i = 0; i < coefficients.length; i++) {
      if (coefficients[i] != 0) {
        if (nonZeroPosition == -1) {
          if (coefficients[i] != 1)
            throw new IllegalStateException("the system has not been set to row echelon form");
          nonZeroPosition = i;
        }
        else
          isAlone = false;
      } 
    }
    if (isAlone && nonZeroPosition != -1)
      instantiation[nonZeroPosition] = constant;
  }

  public void propagate(IntVar[] vars, Propagator prop) throws ContradictionException {
    int firstNonZero = -1;
    int secondNonZero = -1;
    int thirdNonZero = -1;
    int fourthNonZero = -1;
    for (int i = 0; i < coefficients.length; i++) {
      if (coefficients[i] != 0) {
        if (firstNonZero == -1) {
          if (coefficients[i] != 1)
            throw new IllegalStateException("The reduction of the system has not been done");
          firstNonZero = i;
        }
        else {
          if (secondNonZero == -1)
            secondNonZero = i;
          else {
            if (thirdNonZero == -1)
              thirdNonZero = i;
            else
              fourthNonZero = i;
          }
        }
      }
    }
    if (firstNonZero == -1 && constant != 0)
      prop.fails();
    if (fourthNonZero == -1 && firstNonZero != -1) {
      if (secondNonZero != -1) {
        if (thirdNonZero != -1) {
          this.instantiateVarsTriple(vars[firstNonZero], coefficients[firstNonZero], vars[secondNonZero], coefficients[secondNonZero], vars[thirdNonZero], coefficients[thirdNonZero], constant, prop);
        }
        else
          this.instantiateVarsDouble(vars[firstNonZero], coefficients[firstNonZero], vars[secondNonZero], coefficients[secondNonZero], constant, prop);;
      }
      else
        this.instantiateVarsSingle(vars[firstNonZero], coefficients[firstNonZero], constant, prop);
    }
  }

  private void instantiateVarsSingle(IntVar var, int coeff, int constant, Propagator prop) throws ContradictionException { // Only works for variables between [0,bound[, TODO extend
    var.instantiateTo(zpz.getSmallestGreaterEqThan(var.getLB(), zpz.div(constant, coeff)), prop);
  }

  private void instantiateVarsDouble(IntVar var1, int coeff1, IntVar var2, int coeff2, int constant, Propagator prop) throws ContradictionException { // Only works for variables between [0,bound[, TODO extend
    this.instantiateSecondVar(var1, coeff1, var2, coeff2, constant, prop);
    this.instantiateSecondVar(var2, coeff2, var1, coeff1, constant, prop);
  }

  /* Given a constraint `coeff1*var1+coeff2*var2 = constant`, reduce the domain of the second variable */
  private void instantiateSecondVar(IntVar var1, int coeff1, IntVar var2, int coeff2, int constant, Propagator prop) throws ContradictionException {
    IntIterableRangeSet possibleValues2 = new IntIterableRangeSet();
    int lb2 = var2.getLB();
    DisposableValueIterator iterator1 = var1.getValueIterator(true);
    while(iterator1.hasNext()){
      int v = iterator1.next();
      possibleValues2.add(zpz.getSmallestGreaterEqThan(lb2, zpz.div(zpz.sub(constant, zpz.mul(zpz.repr(v),coeff1)), coeff2)));
    }
    var2.removeAllValuesBut(possibleValues2, prop);
  }

  private void instantiateVarsTriple(IntVar var1, int coeff1, IntVar var2, int coeff2, IntVar var3, int coeff3, int constant, Propagator prop) throws ContradictionException { // Only works for variables between [0,bound[, TODO extend
    this.instantiateThirdVar(var1, coeff1, var2, coeff2, var3, coeff3, constant, prop);
    this.instantiateThirdVar(var2, coeff2, var3, coeff3, var1, coeff1, constant, prop);
    this.instantiateThirdVar(var1, coeff1, var3, coeff3, var2, coeff2, constant, prop);
  }

  /* Given a constraint `coeff1*var1+coeff2*var2 = constant`, reduce the domain of the second variable */
  private void instantiateThirdVar(IntVar var1, int coeff1, IntVar var2, int coeff2, IntVar var3, int coeff3, int constant, Propagator prop) throws ContradictionException {
    IntIterableRangeSet possibleValues3 = new IntIterableRangeSet();
    int lb3 = var3.getLB();
    DisposableValueIterator iterator1 = var1.getValueIterator(true);
    while(iterator1.hasNext()){
      int v1 = iterator1.next();
      int updatedConstant = zpz.sub(constant,
                                    zpz.mul(zpz.repr(v1),coeff1));
      DisposableValueIterator iterator2 = var2.getValueIterator(true);
      while(iterator2.hasNext()){
        int v2 = iterator2.next();
        possibleValues3.add(zpz.getSmallestGreaterEqThan(lb3, zpz.div(zpz.sub(updatedConstant,
                                                                             zpz.mul(zpz.repr(v2),coeff2)),
                                                                     coeff3)));
      }
    }
    var3.removeAllValuesBut(possibleValues3, prop);
  }
}
