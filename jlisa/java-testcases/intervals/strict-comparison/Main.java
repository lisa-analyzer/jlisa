import org.sosy_lab.sv_benchmarks.Verifier;

public class Main {
  public static void main(String[] args) {
    double d1 = Verifier.nondetDouble();
    Verifier.assume(d1 < 0.7);
    if (d1 > 0) {
      assert false; // reachable: d1 = 0.5
    }
  }
}
