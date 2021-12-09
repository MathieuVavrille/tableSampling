package org.mvavrill.tableSampling.zpz;

import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.IntVar;

import org.javatuples.Pair;

import java.util.ArrayList;

public class ZpZEquationSystem {
  private final ZpZ zpz;
  public ZpZEquation[] equations;

  public ZpZEquationSystem(ZpZEquation... equations) {
    this.zpz = equations[0].getZpZ();
    this.equations = equations;
  }

  public ZpZEquationSystem(final ZpZ zpz, ZpZEquation... equations) {
    this.zpz = zpz;
    this.equations = equations;
  }

  public void switchLines(final int i,final int j) {
    ZpZEquation eqi = equations[i];
    equations[i] = equations[j];
    equations[j] = eqi;
  }

  /** Modifies the system to put it in row echelon form, and returns the matching between equations and base variables */
  public void setRowEchelonForm() {
    int searchLine = 0;
    for (int pivotCol = 0; pivotCol < equations[0].length() && searchLine < equations.length; pivotCol++) {
      if (equations[searchLine].getCoeff(pivotCol) == 0) { // need to switch lines
        int pivotLine = -1;
        for (int i = searchLine+1; i < equations.length && pivotLine == -1; i++) {// Search for a new pivot
          if (equations[i].getCoeff(pivotCol) != 0)
            pivotLine = i;
        }
        if (pivotLine != -1) // need to switch lines, else
          switchLines(searchLine, pivotLine);
      }
      if (equations[searchLine].getCoeff(pivotCol) != 0) { // if there is a pivot, then normalize
        equations[searchLine].mul(zpz.inv(equations[searchLine].getCoeff(pivotCol)));
        for (int i = 0; i < equations.length; i++) {
          if (i != searchLine && equations[i].getCoeff(pivotCol) != 0) {
            equations[i].subMul(equations[searchLine], equations[i].getCoeff(pivotCol));}
        }
        searchLine++;
      }
    }
  }

  public Pair<int[],int[]> setRowEchelonFormWatched() {
    final int[] lineToVar = new int[equations.length];
    final int[] varToLine = new int[equations[0].length()];
    for (int i = 0; i < varToLine.length; i++)
      varToLine[i] = -1;
    for (int line = 0; line < equations.length; line++) {
      int pivot = 0;
      while (pivot < equations[line].length() && equations[line].getCoeff(pivot) == 0) // If error here then equation equal to zero
        pivot++;
      if (pivot == equations[line].length()) // Case where an equation is 0 = c
        lineToVar[line] = -1;
      else {
        lineToVar[line] = pivot;
        varToLine[pivot] = line;
        makePivot(line,pivot);
      }
    }
    return new Pair<int[],int[]>(lineToVar, varToLine);
  }

  public void makePivot(final int line, final int pivot) {
    equations[line].mul(zpz.inv(equations[line].getCoeff(pivot))); // Normalize the line
    for (int i = 0; i < equations.length; i++) { // Remove pivot from every other equation
      if (i != line && equations[i].getCoeff(pivot) != 0) {
        equations[i].subMul(equations[line], equations[i].getCoeff(pivot));}
    }
  }

  public ZpZEquation getEquation(final int i) {
    return equations[i];
  }

  public int getNbEquations() {
    return equations.length;
  }

  public int getCoeff(final int line, final int column) {
    return equations[line].getCoeff(column);
  }
  
  public int getCst(final int line) {
    return equations[line].getCst();
  }

  public ZpZ getZpZ() {
    return zpz;
  }

  public int[][] getCoefficientsMatrix() {
    final int[][] A = new int[equations.length][];
    for (int i = 0; i < equations.length; i++)
      A[i] = equations[i].getCoefficients();
    return A;
  }

  public int[] getRhs() {
    final int[] b = new int[equations.length];
    for (int i = 0; i < equations.length; i++)
      b[i] = equations[i].getCst();
    return b;
  }

  

  public String toString() {
    String s = equations[0].toString();
    for (int i = 1; i < equations.length; i++) {
      s = s + "\n" + equations[i];
    }
    return s;
  }





  /** Legacy code */

  public ZpZEquationSystem simplify(final int nbUninstantiatedVars, final Integer[] values) {
    ZpZEquation[] simplEquations = new ZpZEquation[equations.length];
    for (int i = 0; i < equations.length; i++) {
      simplEquations[i] = equations[i].simplify(nbUninstantiatedVars, values);
    }
    return new ZpZEquationSystem(zpz, simplEquations);
  }

  public int[] getInstantiation() { // Should already be in row echelon form
    this.setRowEchelonForm();
    int[] instantiation = new int[equations[0].length()];
    for (int i = 0; i < instantiation.length; i++)
      instantiation[i] = -1;
    for (int i = 0; i < equations.length; i++)
      equations[i].fillInstantiation(instantiation);
    return instantiation;
  }

  public void propagate(IntVar[] vars, Propagator prop) throws ContradictionException {
    long size;
    do{
      size = 0;
      for(IntVar v:vars){
        size+=v.getDomainSize();
      }
      for (ZpZEquation eq : equations)
        eq.propagate(vars, prop);
      for(IntVar v:vars){
        size-=v.getDomainSize();
      }
    }while(size>0);
  }

  public boolean isContradiction() {
    boolean contradicts = false;
    for (ZpZEquation eq: equations)
      contradicts |= eq.isContradiction();
    return contradicts;
  }

  public boolean check(int[] instantiation) {
    boolean isOk = true;
    for (ZpZEquation eq: equations)
      isOk &= eq.check(instantiation);
    return isOk;
  }
}
