import org.sosy_lab.sv_benchmarks.Verifier;

public class Main {
  public static void main(String[] args) {
    String s = Verifier.nondetString();
    int x = Verifier.nondetInt();
    // the counterexample goes through an operation that is not encoded
    // precisely, but its inputs reproduce the violation
    String t = s.trim();
    if (x == 42 || t.length() > 100) {
      assert false;
    }
  }
}
