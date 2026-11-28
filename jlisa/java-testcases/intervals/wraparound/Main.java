import org.sosy_lab.sv_benchmarks.Verifier;

class Main {
  public static void main(String[] args) {
    int k = Verifier.nondetInt();
    if (k == 1) byteBlock();
    else if (k == 2) byteCross();
    else if (k == 3) shortBlock();
    else if (k == 4) intBlock();
    else if (k == 5) intCross();
    else if (k == 6) byteFits();
    else if (k == 7) byteBlockLow();
  }
  static int range(int lo, int hi) { int i = Verifier.nondetInt(); if (i < lo || i > hi) return lo; return i; }
  static long lrange(long lo, long hi) { long i = Verifier.nondetLong(); if (i < lo || i > hi) return lo; return i; }

  static void byteBlock() { int x = range(128, 130); byte b = (byte) x; assert b >= -128 && b <= -126; } // holds
  static void byteCross() { int x = range(126, 129); byte b = (byte) x; assert b >= 0; } // fails for some input
  static void shortBlock() { int x = range(65536, 65540); short s = (short) x; assert s >= 0 && s <= 4; } // holds
  static void intBlock() { long x = lrange(2147483648L, 2147483650L); int i = (int) x; assert i <= -2147483646; } // holds
  static void intCross() { long x = lrange(2147483646L, 2147483649L); int i = (int) x; assert i > 0; } // fails for some input
  static void byteFits() { int x = range(-5, 5); byte b = (byte) x; assert b >= -5 && b <= 5; } // holds
  static void byteBlockLow() { int x = range(-131, -129); byte b = (byte) x; assert b >= 125; } // holds
}
