import org.sosy_lab.sv_benchmarks.Verifier;

class Main {
  public static void main(String[] args) {
    int k = Verifier.nondetInt();
    if (k == 1) literalPos();
    else if (k == 2) literalNeg();
    else if (k == 3) varPos();
    else if (k == 4) varNegWrong();
    else if (k == 5) varNegRight();
    else if (k == 6) nan();
    else if (k == 7) negNumerator();
    else if (k == 8) unknownNumerator();
    else if (k == 9) remNan();
  }
  static void literalPos() { double r = 1.0 / 0.0; assert r > 0; } // holds
  static void literalNeg() { double r = 1.0 / -0.0; assert r < 0; } // holds
  static void varPos() { double z = 0.0; double r = 1.0 / z; assert r > 0; } // holds
  static void varNegWrong() { double z = -0.0; double r = 1.0 / z; assert r > 0; } // fails
  static void varNegRight() { double z = -0.0; double r = 1.0 / z; assert r < 0; } // holds
  static void nan() { double r = 0.0 / 0.0; assert r != r; } // holds
  static void negNumerator() { double r = -1.0 / 0.0; assert r < 0; } // holds
  static void unknownNumerator() { double x = Verifier.nondetDouble(); double r = x / 0.0; assert Double.isInfinite(r) || Double.isNaN(r); } // holds
  static void remNan() { double r = 5.0 % 0.0; assert r != r; } // holds
}
