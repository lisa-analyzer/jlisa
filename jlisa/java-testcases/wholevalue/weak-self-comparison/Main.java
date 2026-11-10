import org.sosy_lab.sv_benchmarks.Verifier;

public class Main {
  // the arrays built here have the same allocation site: their lengths and
  // their elements are summarized by the same identifiers
  static int[] make() {
    int n = Verifier.nondetInt();
    Verifier.assume(n >= 1 && n < 8);
    int[] t = new int[n];
    t[0] = Verifier.nondetInt();
    return t;
  }

  public static void main(String[] args) {
    int[] a = make();
    int[] b = make();
    assert a.length == b.length; // fails for some input
    assert a[0] == b[0]; // fails for some input
  }
}
