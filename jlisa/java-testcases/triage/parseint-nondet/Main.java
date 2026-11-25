import org.sosy_lab.sv_benchmarks.Verifier;

// the value parsed from a string that is not constant: a counterexample is a
// string denoting 42
public class Main {
  public static void main(String[] args) {
    String s = Verifier.nondetString();
    try {
      int x = Integer.parseInt(s);
      assert x != 42;
    } catch (NumberFormatException e) {
    }
  }
}
