import org.sosy_lab.sv_benchmarks.Verifier;

public class Main {
  public static void main(String[] args) {
    String s = Verifier.nondetString();
    int x = 5;
    x = s.length(); // the constants cannot process the length: x is no longer 5
    assert x == 5; // fails for some input
  }
}
