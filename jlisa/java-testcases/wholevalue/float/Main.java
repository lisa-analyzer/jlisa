import org.sosy_lab.sv_benchmarks.Verifier;

class Main {
  public static void main(String[] args) {
    int k = Verifier.nondetInt();
    if (k == 1) sumOverflow();
    else if (k == 2) sumInDouble();
    else if (k == 3) narrowing();
    else if (k == 4) divNaN();
    else if (k == 5) loopFinite();
    else if (k == 6) negatedComparison();
    else if (k == 7) mulInfinity();
    else if (k == 8) oneSided();
    else if (k == 9) againstInfinity();
    else if (k == 10) strictAgainstInfinity();
  }

  static void sumOverflow() { float x = Verifier.nondetFloat(); if (!(x >= 3e38f && x <= 3.4e38f)) return; float y = x + x; assert y < Float.POSITIVE_INFINITY; } // 18 F
  static void sumInDouble() { float x = Verifier.nondetFloat(); if (!(x >= 3e38f && x <= 3.4e38f)) return; double y = (double) x + x; assert y == y; } // 19 T
  static void narrowing() { double d = Verifier.nondetDouble(); if (!(d >= 1e300 && d <= 1e301)) return; float f = (float) d; assert f < Float.POSITIVE_INFINITY; } // 20 F
  static void divNaN() { float a = Verifier.nondetFloat(); if (!(a >= 0f && a <= 1f)) return; float n = a / a; assert n == n; } // 21 F
  static void loopFinite() { float x = 0f; while (Verifier.nondetBoolean()) x = x * 0.5f + 1f; assert x == x; } // 22 T
  static void negatedComparison() { float x = 0f; while (Verifier.nondetBoolean()) x = x * 0.5f + 1f; if (!(x <= 2f)) assert false; } // 23 T
  static void mulInfinity() { float x = Verifier.nondetFloat(); if (!(x >= 1e30f && x <= 1e31f)) return; float y = x * x; assert y < Float.POSITIVE_INFINITY; } // 24 F
  static void oneSided() { float x = Verifier.nondetFloat(); if (!(x <= 3.4e38f)) return; assert x > Float.NEGATIVE_INFINITY; } // 25 F
  static void againstInfinity() { float x = Verifier.nondetFloat(); float y = Verifier.nondetFloat(); if (!(x <= y)) return; assert x < Float.POSITIVE_INFINITY; } // 26 F
  static void strictAgainstInfinity() { float x = Verifier.nondetFloat(); float y = Verifier.nondetFloat(); if (!(x < y)) return; double s = (double) x + y; assert s == s; } // 27 F
}
