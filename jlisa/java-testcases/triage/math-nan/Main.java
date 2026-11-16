import org.sosy_lab.sv_benchmarks.Verifier;

public class Main {
  public static void main(String[] args) {
    double x = Verifier.nondetDouble();
    // violated near x = 1, while asin and acos are NaN outside of [-1, 1]
    if (1 + Math.acos(x) < Math.asin(x)) {
      assert false;
    }
  }
}
