import org.sosy_lab.sv_benchmarks.Verifier;

public class Main {
  public static void main(String[] args) {
    String s = Verifier.nondetString();
    // the violation depends on an operation that is not encoded precisely,
    // and the inputs of the counterexample do not reproduce it
    if (s.toUpperCase().equals("AB") && s.length() != 2) {
      assert false;
    }
  }
}
