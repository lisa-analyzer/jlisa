import org.sosy_lab.sv_benchmarks.Verifier;

public class Main {
  static void fail(boolean first) {
    if (first) {
      throw new IllegalStateException();
    } else {
      throw new IllegalArgumentException();
    }
  }

  public static void main(String[] args) {
    fail(Verifier.nondetBoolean());
  }
}
