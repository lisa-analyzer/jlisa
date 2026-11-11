import org.sosy_lab.sv_benchmarks.Verifier;

public class Main {
  public static void main(String[] args) {
    double x = Verifier.nondetDouble();
    double y = Verifier.nondetDouble();
    if (Math.exp(x) > 5 && Math.sin(x) + Math.cos(y) > 1) {
      assert false;
    }
  }
}
