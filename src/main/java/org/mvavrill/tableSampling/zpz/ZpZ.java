package org.mvavrill.tableSampling.zpz;

public class ZpZ {
  public final int p;
  public final int[] inverse;

  /* -------- Initialisation of the class -------- */
  /**
   * Initialise p to the smallest prime bigger or equal than n.
   * Remark that for the use to store existing values, n should be strictly greater than the maximum of the values
   */
  public ZpZ(int p) {
    this.p = p;
    inverse = new int[p];
    for (int i = 1; i < p; i++)
      inverse[i] = this.expMod(i,p-2); // Using Fermat's little theorem, a^{-1} = a^{p-2} because p is prime
  }

  public static int getPrimeGreaterThan(int n) {
    n++;
    while (!is_prime(n)) {
      n++;
    }
    return n;
  }

  /* Really naive implementation of prime testing */
  private static boolean is_prime(int n) {
    if (n >= 2 && n%2 == 0)
      return false;
    for (int i = 3; i < n/2; i = i+2) {
      if (n%i == 0)
        return false;
    }
    return true;
  }

  /* (a^n)%p */
  public int expMod(final int a,final int n) {
    if (n == 0)
      return 1;
    if (n == 1)
      return a%p;
    int r = expMod(a,n/2);
    return ((r*r%p)*((n%2 == 1) ? a : 1))%p;
  }

  /* -------- Operations on Z/pZ -------- */
  public int repr(final int value) {
    int r = value%p;
    return (r < 0)? r+p : r; // to deal with negative numbers
  }

  public boolean isRepr(final int value) {
    return 0 <= value && value < p;
  }

  public int add(final int a, final int b) {
    return (a+b)%p;
  }

  public int sub(final int a, final int b) {
    return (a-b+p)%p;
  }

  public int mul(final int a, final int b) {
    return (a*b)%p;
  }

  public int div(final int a, final int b) {
    if (b == 0)
      throw new ArithmeticException("Division by zero in ZpZ");
    return this.mul(a, this.inv(b));
  }

  // Uses the precomputed values of the inverse
  public int inv(final int a) {
    if (a == 0)
      throw new ArithmeticException("0 does not have an inverse");
    return inverse[a];
  }

  // Return the smallest integer x greater than m such that x = a (mod p)
  public int getSmallestGreaterEqThan(int m, int a) {
    return m+((a-m)%p+p)%p;
  }

  public String toString() {
    return Integer.toString(p);
  }

}
