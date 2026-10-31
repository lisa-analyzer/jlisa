import org.sosy_lab.sv_benchmarks.Verifier;

class Main {
  public static void main(String[] args) {
    int k = Verifier.nondetInt();
    if (k == 1) addOverflow();
    else if (k == 2) mulOverflow();
    else if (k == 3) longAdd();
    else if (k == 4) negMin();
    else if (k == 5) subOverflow();
    else if (k == 6) unboundedAdd();
  }
  static int range(int lo, int hi) { int i = Verifier.nondetInt(); if (i < lo || i > hi) return lo; return i; }
  static long lrange(long lo, long hi) { long i = Verifier.nondetLong(); if (i < lo || i > hi) return lo; return i; }

  static void addOverflow() { int x = range(2147483646, 2147483647); int y = x + 1; assert y > 0; } // fails for some input
  static void mulOverflow() { int x = range(65536, 65537); int y = x * 65536; assert y > 0; } // fails for some input
  static void longAdd() { long x = lrange(9223372036854775806L, 9223372036854775807L); long y = x + 1; assert y > 0; } // fails for some input
  static void negMin() { int x = range(-2147483648, -2147483647); int y = -x; assert y > 0; } // fails for some input
  static void subOverflow() { int x = range(-2147483648, -2147483647); int y = x - 1; assert y < 0; } // fails for some input
  static void unboundedAdd() { int x = Verifier.nondetInt(); if (x < 0) return; int y = x + 1; assert y > 0; } // fails for some input
}
