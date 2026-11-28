import org.sosy_lab.sv_benchmarks.Verifier;

public class Main {
  public static void main(String[] args) {
    double x = Verifier.nondetDouble();
    // the sine and the cosine are NaN or between -1 and 1, and the
    // exponential is NaN or not negative
    if (Math.sin(x) > 1.0 || Math.cos(x) < -1.0 || Math.exp(x) < 0.0) {
      assert false;
    }
  }
}
