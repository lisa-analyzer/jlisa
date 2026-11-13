import org.sosy_lab.sv_benchmarks.Verifier;

public class Main {
  static int depth(int n) {
    return n <= 0 ? 0 : 1 + depth(n - 1);
  }

  public static void main(String[] args) {
    int x = Verifier.nondetInt();
    // the counterexample relies on the result of a recursive call, that is
    // not computed precisely, but its inputs reproduce the violation
    int r = depth(3);
    if (x == 42 && r >= 0) {
      assert false;
    }
  }
}
